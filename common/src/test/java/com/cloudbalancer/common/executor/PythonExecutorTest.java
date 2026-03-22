package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.SecurityLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PythonExecutorTest {

    private final PythonExecutor executor = new PythonExecutor("python3");

    // ---- Validation tests ----

    @Test
    void validateRejectsMissingScript() {
        assertThat(executor.validate(Map.of()).valid()).isFalse();
    }

    @Test
    void validateRejectsBlankScript() {
        assertThat(executor.validate(Map.of("script", "   ")).valid()).isFalse();
    }

    @Test
    void validateAcceptsValidScript() {
        assertThat(executor.validate(Map.of("script", "print('hello')")).valid()).isTrue();
    }

    // ---- Type and capabilities tests ----

    @Test
    void getExecutorTypeReturnsPython() {
        assertThat(executor.getExecutorType()).isEqualTo(ExecutorType.PYTHON);
    }

    @Test
    void getCapabilitiesDoesNotRequireDocker() {
        var caps = executor.getCapabilities();
        assertThat(caps.requiresDocker()).isFalse();
        assertThat(caps.securityLevel()).isEqualTo(SecurityLevel.SANDBOXED);
    }

    // ---- Resource estimation test ----

    @Test
    void estimateResourcesReturnsReasonableDefaults() {
        var est = executor.estimateResources(Map.of("script", "print(1)"));
        assertThat(est.estimatedCpuCores()).isGreaterThan(0);
        assertThat(est.estimatedMemoryMB()).isGreaterThan(0);
    }

    // ---- Execution tests ----

    @Test
    void executePrintHelloReturnsStdout(@TempDir Path workDir) {
        Map<String, Object> spec = Map.of("script", "print('hello')");
        var ctx = new TaskContext(UUID.randomUUID(), workDir);
        ExecutionResult result = executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("hello");
    }

    @Test
    void executeSyntaxErrorReturnsNonZero(@TempDir Path workDir) {
        Map<String, Object> spec = Map.of("script", "def broken(");
        var ctx = new TaskContext(UUID.randomUUID(), workDir);
        ExecutionResult result = executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);
        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.stderr()).isNotBlank();
    }

    @Test
    void executeStderrCaptured(@TempDir Path workDir) {
        Map<String, Object> spec = Map.of("script", "import sys; sys.stderr.write('err msg\\n')");
        var ctx = new TaskContext(UUID.randomUUID(), workDir);
        ExecutionResult result = executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);
        assertThat(result.stderr()).contains("err msg");
    }

    @Test
    void executeInvokesLogCallback(@TempDir Path workDir) {
        List<String> lines = new ArrayList<>();
        LogCallback cb = (line, isStderr, ts) -> lines.add(line);
        Map<String, Object> spec = Map.of("script", "print('a')\nprint('b')");
        var ctx = new TaskContext(UUID.randomUUID(), workDir, cb);
        executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);
        assertThat(lines).contains("a", "b");
    }

    @Test
    void executeWithEmptyRequirementsCreatesVenv(@TempDir Path workDir) {
        Map<String, Object> spec = new HashMap<>();
        spec.put("script", "import json; print(json.dumps({'ok': True}))");
        spec.put("requirements", List.of());
        var ctx = new TaskContext(UUID.randomUUID(), workDir);
        ExecutionResult result = executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("ok");
    }

    @Test
    void executeTruncatesLargeOutput(@TempDir Path workDir) {
        // Generate output larger than 1MB (~10MB total)
        String script = "for i in range(100000): print('x' * 100)";
        Map<String, Object> spec = Map.of("script", script);
        var ctx = new TaskContext(UUID.randomUUID(), workDir);
        ExecutionResult result = executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout().length()).isLessThanOrEqualTo(1_048_576);
    }

    @Test
    void cancelKillsPythonProcess(@TempDir Path workDir) throws Exception {
        Map<String, Object> spec = Map.of("script", "import time; time.sleep(300)");
        UUID taskId = UUID.randomUUID();
        var ctx = new TaskContext(taskId, workDir);
        CompletableFuture<ExecutionResult> future = CompletableFuture.supplyAsync(
            () -> executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx)
        );
        // Wait for venv creation + script start; retry cancel until process is registered
        Thread.sleep(5000);
        executor.cancel(new ExecutionHandle(taskId.toString()));
        ExecutionResult result = future.get(30, TimeUnit.SECONDS);
        assertThat(result.succeeded()).isFalse();
    }
}
