package com.cloudbalancer.common.executor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TaskContextTest {

    @Test
    void taskContextWithoutLogCallbackHasNullCallback(@TempDir Path workDir) {
        var ctx = new TaskContext(UUID.randomUUID(), workDir);
        assertThat(ctx.logCallback()).isNull();
    }

    @Test
    void taskContextWithLogCallbackInvokesCallback(@TempDir Path workDir) {
        List<String> captured = new ArrayList<>();
        LogCallback callback = (line, isStderr, timestamp) -> captured.add(line);
        var ctx = new TaskContext(UUID.randomUUID(), workDir, callback);

        ctx.logCallback().onLogLine("hello", false, Instant.now());
        assertThat(captured).containsExactly("hello");
    }

    @Test
    void twoArgConstructorPreservesFields(@TempDir Path workDir) {
        UUID id = UUID.randomUUID();
        var ctx = new TaskContext(id, workDir);
        assertThat(ctx.taskId()).isEqualTo(id);
        assertThat(ctx.workingDirectory()).isEqualTo(workDir);
    }
}
