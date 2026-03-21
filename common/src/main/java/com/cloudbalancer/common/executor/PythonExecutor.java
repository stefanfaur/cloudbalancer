package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.model.ExecutorCapabilities;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.SecurityLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PythonExecutor implements TaskExecutor {

    private final String pythonBinary;
    private final ConcurrentHashMap<UUID, Process> runningProcesses = new ConcurrentHashMap<>();

    public PythonExecutor(String pythonBinary) {
        this.pythonBinary = pythonBinary;
    }

    public PythonExecutor() {
        this("python3");
    }

    @Override
    public ValidationResult validate(Map<String, Object> spec) {
        if (!(spec.get("script") instanceof String script) || script.isBlank()) {
            return ValidationResult.invalid("Missing required field: script");
        }
        return ValidationResult.ok();
    }

    @Override
    public ExecutorType getExecutorType() {
        return ExecutorType.PYTHON;
    }

    @Override
    public ExecutorCapabilities getCapabilities() {
        return new ExecutorCapabilities(
            false,
            false,
            new ResourceProfile(4, 8192, 10240, false, 3600, false),
            SecurityLevel.SANDBOXED
        );
    }

    @Override
    public ResourceEstimate estimateResources(Map<String, Object> spec) {
        return new ResourceEstimate(1, 512, 120_000);
    }

    @Override
    public void cancel(ExecutionHandle handle) {
        UUID taskId = UUID.fromString(handle.handleId());
        Process process = runningProcesses.get(taskId);
        if (process != null) {
            process.destroyForcibly();
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public ExecutionResult execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context) {
        ValidationResult vr = validate(spec);
        if (!vr.valid()) {
            return new ExecutionResult(1, "", "Validation failed: " + vr.errors(), 0, false);
        }

        String script = (String) spec.get("script");
        @SuppressWarnings("unchecked")
        List<String> requirements = (List<String>) spec.get("requirements");
        boolean networkAccessRequired = Boolean.TRUE.equals(spec.get("networkAccessRequired"));

        long start = System.currentTimeMillis();
        Path tempDir = null;

        try {
            // Create temp workdir inside context working directory
            tempDir = Files.createTempDirectory(context.workingDirectory(), "python-");
            Path venvDir = tempDir.resolve("venv");

            // Create virtualenv
            int venvExit = runProcess(
                new String[]{pythonBinary, "-m", "venv", venvDir.toString()},
                tempDir, null, null
            );
            if (venvExit != 0) {
                long elapsed = System.currentTimeMillis() - start;
                return new ExecutionResult(1, "", "Failed to create virtualenv (exit code " + venvExit + ")", elapsed, false);
            }

            // Determine paths within the venv
            String venvBinDir = venvDir.resolve("bin").toString();
            String pythonPath = venvDir.resolve("bin").resolve("python").toString();
            String pipPath = venvDir.resolve("bin").resolve("pip").toString();

            // Install requirements if present
            if (requirements != null && !requirements.isEmpty()) {
                List<String> pipCmd = new ArrayList<>();
                pipCmd.add(pipPath);
                pipCmd.add("install");
                pipCmd.addAll(requirements);
                int pipExit = runProcess(
                    pipCmd.toArray(new String[0]),
                    tempDir, null, null
                );
                if (pipExit != 0) {
                    long elapsed = System.currentTimeMillis() - start;
                    return new ExecutionResult(1, "", "Failed to install requirements (exit code " + pipExit + ")", elapsed, false);
                }
            }

            // Write script to file
            Path scriptFile = tempDir.resolve("script.py");
            Files.writeString(scriptFile, script);

            // Build the command
            List<String> command = new ArrayList<>();

            // On Linux, apply network isolation when not required
            if (!networkAccessRequired && isLinux()) {
                command.add("unshare");
                command.add("--net");
            }

            command.add(pythonPath);
            command.add(scriptFile.toString());

            // Set up process with minimal environment
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(tempDir.toFile());
            Map<String, String> env = pb.environment();
            env.clear();
            env.put("PATH", venvBinDir + ":/usr/bin:/bin");
            env.put("HOME", tempDir.toString());
            env.put("TMPDIR", tempDir.toString());
            env.put("VIRTUAL_ENV", venvDir.toString());

            Process process = pb.start();
            runningProcesses.put(context.taskId(), process);

            try {
                // Capture stdout and stderr on separate threads with line-by-line callback
                LogCallback callback = context.logCallback();
                CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(
                    () -> readStreamWithCallback(process.getInputStream(), false, callback)
                );
                CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(
                    () -> readStreamWithCallback(process.getErrorStream(), true, callback)
                );

                int exitCode = process.waitFor();
                String stdout = stdoutFuture.join();
                String stderr = stderrFuture.join();
                long elapsed = System.currentTimeMillis() - start;

                return new ExecutionResult(exitCode, stdout, stderr, elapsed, false);
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                long elapsed = System.currentTimeMillis() - start;
                return new ExecutionResult(1, "", "Execution interrupted", elapsed, true);
            } finally {
                runningProcesses.remove(context.taskId());
            }
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - start;
            return new ExecutionResult(1, "", "Failed to set up Python execution: " + e.getMessage(), elapsed, false);
        } finally {
            // Clean up temp directory
            if (tempDir != null) {
                deleteDirectoryQuietly(tempDir);
            }
        }
    }

    private int runProcess(String[] command, Path workDir, LogCallback callback, UUID taskId) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Drain output to prevent blocking
            try (var reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {
                    // discard output from setup commands
                }
            }

            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return 1;
        }
    }

    private String readStreamWithCallback(InputStream inputStream, boolean isStderr,
                                          LogCallback callback) {
        try (var reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (callback != null) {
                    callback.onLogLine(line, isStderr, Instant.now());
                }
                if (!sb.isEmpty()) sb.append('\n');
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            return "Error reading stream: " + e.getMessage();
        }
    }

    private static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    private static void deleteDirectoryQuietly(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.deleteIfExists(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
