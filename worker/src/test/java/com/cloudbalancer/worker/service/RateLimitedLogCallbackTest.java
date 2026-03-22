package com.cloudbalancer.worker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitedLogCallbackTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void batchesLinesWithinInterval() {
        UUID taskId = UUID.randomUUID();
        var callback = new RateLimitedLogCallback(taskId, kafkaTemplate, 50);

        // First line sends immediately (lastSendTime=0, so interval elapsed)
        callback.onLogLine("line1", false, Instant.now());
        verify(kafkaTemplate, times(1)).send(eq("tasks.logs"), eq(taskId.toString()), anyString());

        // Second line within 50ms should be buffered, not sent
        callback.onLogLine("line2", false, Instant.now());
        // Still only 1 send (line2 is buffered)
        verify(kafkaTemplate, times(1)).send(eq("tasks.logs"), eq(taskId.toString()), anyString());

        // Flush sends the buffered line
        callback.flush();
        verify(kafkaTemplate, times(2)).send(eq("tasks.logs"), eq(taskId.toString()), anyString());
    }

    @Test
    void flushSendsAllBufferedLines() {
        UUID taskId = UUID.randomUUID();
        var callback = new RateLimitedLogCallback(taskId, kafkaTemplate, 100_000); // very long interval

        callback.onLogLine("line1", false, Instant.now()); // first call sends immediately
        callback.onLogLine("line2", false, Instant.now()); // buffered
        callback.onLogLine("line3", true, Instant.now());  // buffered

        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());

        callback.flush();
        verify(kafkaTemplate, times(3)).send(anyString(), anyString(), anyString());
    }

    @Test
    void emptyFlushDoesNothing() {
        var callback = new RateLimitedLogCallback(UUID.randomUUID(), kafkaTemplate, 50);
        callback.flush(); // no lines buffered
        verifyNoInteractions(kafkaTemplate);
    }
}
