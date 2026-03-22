package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.executor.ExecutionResult;
import com.cloudbalancer.common.executor.LogCallback;
import com.cloudbalancer.common.executor.LogMessage;
import com.cloudbalancer.common.executor.PythonExecutor;
import com.cloudbalancer.common.executor.ResourceAllocation;
import com.cloudbalancer.common.executor.TaskContext;
import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that the log callback mechanism correctly
 * captures execution output and produces valid, serializable LogMessage
 * JSON -- the same format that would be published to the {@code tasks.logs}
 * Kafka topic.
 *
 * <p>Runs without Spring context, Kafka, or external services.
 */
class LogStreamIntegrationTest {

    @Test
    void logCallbackProducesValidLogMessageJson() throws Exception {
        UUID taskId = UUID.randomUUID();
        List<String> capturedJson = new ArrayList<>();

        LogCallback callback = (line, isStderr, timestamp) -> {
            LogMessage msg = new LogMessage(taskId, line, isStderr, timestamp);
            try {
                capturedJson.add(JsonUtil.mapper().writeValueAsString(msg));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize LogMessage", e);
            }
        };

        // Invoke the callback manually to simulate what an executor would do
        Instant now = Instant.now();
        callback.onLogLine("stdout line", false, now);
        callback.onLogLine("stderr line", true, now);

        assertThat(capturedJson).hasSize(2);

        // Verify round-trip deserialization of stdout message
        LogMessage stdout = JsonUtil.mapper().readValue(capturedJson.get(0), LogMessage.class);
        assertThat(stdout.taskId()).isEqualTo(taskId);
        assertThat(stdout.line()).isEqualTo("stdout line");
        assertThat(stdout.stderr()).isFalse();
        assertThat(stdout.timestamp()).isEqualTo(now);

        // Verify round-trip deserialization of stderr message
        LogMessage stderr = JsonUtil.mapper().readValue(capturedJson.get(1), LogMessage.class);
        assertThat(stderr.taskId()).isEqualTo(taskId);
        assertThat(stderr.line()).isEqualTo("stderr line");
        assertThat(stderr.stderr()).isTrue();
    }

    @Test
    void pythonExecutorStreamsLogsThroughCallback(@TempDir Path workDir) {
        UUID taskId = UUID.randomUUID();
        List<LogMessage> captured = new CopyOnWriteArrayList<>();

        LogCallback callback = (line, isStderr, timestamp) ->
                captured.add(new LogMessage(taskId, line, isStderr, timestamp));

        var executor = new PythonExecutor("python3");
        Map<String, Object> spec = Map.of("script",
                "import sys\nprint('out1')\nprint('out2')\nsys.stderr.write('err1\\n')");

        var ctx = new TaskContext(taskId, workDir, callback);
        ExecutionResult result = executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);

        assertThat(result.exitCode()).isZero();

        // Verify stdout lines were captured
        List<String> stdoutLines = captured.stream()
                .filter(m -> !m.stderr())
                .map(LogMessage::line)
                .toList();
        assertThat(stdoutLines).contains("out1", "out2");

        // Verify stderr line was captured
        List<String> stderrLines = captured.stream()
                .filter(LogMessage::stderr)
                .map(LogMessage::line)
                .toList();
        assertThat(stderrLines).contains("err1");

        // All messages should carry the correct taskId and non-null timestamps
        assertThat(captured).allSatisfy(msg -> {
            assertThat(msg.taskId()).isEqualTo(taskId);
            assertThat(msg.timestamp()).isNotNull();
        });
    }

    @Test
    void logMessagesPreserveSpecialCharacters() throws Exception {
        UUID taskId = UUID.randomUUID();
        List<String> jsonMessages = new ArrayList<>();

        LogCallback callback = (line, isStderr, timestamp) -> {
            LogMessage msg = new LogMessage(taskId, line, isStderr, timestamp);
            try {
                jsonMessages.add(JsonUtil.mapper().writeValueAsString(msg));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // Lines with unicode, quotes, and backslashes
        callback.onLogLine("data: \u00e9\u00e0\u00fc\u00f1", false, Instant.now());
        callback.onLogLine("path: C:\\Users\\test", false, Instant.now());
        callback.onLogLine("json: {\"key\": \"value\"}", false, Instant.now());

        assertThat(jsonMessages).hasSize(3);

        // Each should survive a round-trip through JSON
        for (String json : jsonMessages) {
            LogMessage deserialized = JsonUtil.mapper().readValue(json, LogMessage.class);
            assertThat(deserialized.taskId()).isEqualTo(taskId);
            assertThat(deserialized.line()).isNotBlank();
        }

        // Verify specific content after round-trip
        LogMessage unicodeMsg = JsonUtil.mapper().readValue(jsonMessages.get(0), LogMessage.class);
        assertThat(unicodeMsg.line()).isEqualTo("data: \u00e9\u00e0\u00fc\u00f1");
    }

    @Test
    void emptyLogLineSerializesCorrectly() throws Exception {
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        LogMessage msg = new LogMessage(taskId, "", false, now);

        String json = JsonUtil.mapper().writeValueAsString(msg);
        LogMessage deserialized = JsonUtil.mapper().readValue(json, LogMessage.class);

        assertThat(deserialized.taskId()).isEqualTo(taskId);
        assertThat(deserialized.line()).isEmpty();
        assertThat(deserialized.stderr()).isFalse();
        assertThat(deserialized.timestamp()).isEqualTo(now);
    }
}
