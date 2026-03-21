package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.model.ExecutorCapabilities;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.SecurityLevel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ShellExecutor implements TaskExecutor {

    private static final Set<String> DEFAULT_BLOCKED = Set.of(
        "rm -rf /", "shutdown", "reboot", "mkfs", "dd", ":(){ :|:& };:"
    );

    private final Set<String> blockedCommands;
    private final int maxOutputBytes;
    private final ConcurrentHashMap<UUID, Process> runningProcesses = new ConcurrentHashMap<>();

    public ShellExecutor() {
        this(DEFAULT_BLOCKED, 1_048_576);
    }

    public ShellExecutor(Set<String> blockedCommands, int maxOutputBytes) {
        this.blockedCommands = blockedCommands;
        this.maxOutputBytes = maxOutputBytes;
    }

    @Override
    public ExecutionResult execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context) {
        ValidationResult vr = validate(spec);
        if (!vr.valid()) {
            return new ExecutionResult(1, "", "Validation failed: " + vr.errors(), 0, false);
        }

        String command = (String) spec.get("command");
        long start = System.currentTimeMillis();

        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);

        // Set working directory
        Path workDir = context.workingDirectory();
        if (workDir != null) {
            pb.directory(workDir.toFile());
        }

        // Set environment variables if provided
        @SuppressWarnings("unchecked")
        Map<String, String> envVars = (Map<String, String>) spec.get("environment");
        if (envVars != null) {
            pb.environment().putAll(envVars);
        }

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - start;
            return new ExecutionResult(1, "", "Failed to start process: " + e.getMessage(), elapsed, false);
        }

        // Track the process for cancellation
        runningProcesses.put(context.taskId(), process);

        try {
            // Capture stdout and stderr on separate threads
            CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(
                () -> readStream(process.getInputStream())
            );
            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(
                () -> readStream(process.getErrorStream())
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
    }

    @Override
    public ValidationResult validate(Map<String, Object> spec) {
        if (!(spec.get("command") instanceof String command) || command.isBlank()) {
            return ValidationResult.invalid("Missing required field: command");
        }
        for (String blocked : blockedCommands) {
            if (command.contains(blocked)) {
                return ValidationResult.invalid("Blocked command detected: " + blocked);
            }
        }

        return ValidationResult.ok();
    }

    @Override
    public ResourceEstimate estimateResources(Map<String, Object> spec) {
        return new ResourceEstimate(1, 256, 60_000);
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
    public ExecutorType getExecutorType() {
        return ExecutorType.SHELL;
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

    private String readStream(InputStream inputStream) {
        try {
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytes = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                int remaining = maxOutputBytes - totalBytes;
                if (remaining <= 0) {
                    break;
                }
                int toAppend = Math.min(bytesRead, remaining);
                sb.append(new String(buffer, 0, toAppend, StandardCharsets.UTF_8));
                totalBytes += toAppend;
            }
            return sb.toString();
        } catch (IOException e) {
            return "Error reading stream: " + e.getMessage();
        }
    }
}
