package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class LogMessageTest {

    @Test
    void serializationRoundTrip() throws Exception {
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        LogMessage msg = new LogMessage(taskId, "hello world", false, now);

        String json = JsonUtil.mapper().writeValueAsString(msg);
        LogMessage deserialized = JsonUtil.mapper().readValue(json, LogMessage.class);

        assertThat(deserialized.taskId()).isEqualTo(taskId);
        assertThat(deserialized.line()).isEqualTo("hello world");
        assertThat(deserialized.stderr()).isFalse();
        assertThat(deserialized.timestamp()).isEqualTo(now);
    }
}
