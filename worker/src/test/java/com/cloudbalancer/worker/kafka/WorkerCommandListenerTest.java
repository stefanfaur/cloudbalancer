package com.cloudbalancer.worker.kafka;

import com.cloudbalancer.common.model.DrainCommand;
import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WorkerCommandListenerTest {

    @Test
    void drainCommandSetsDrainingFlag() throws Exception {
        AtomicBoolean drainingFlag = new AtomicBoolean(false);
        var listener = new WorkerCommandListener("worker-1", drainingFlag);

        var cmd = new DrainCommand("worker-1", 60, Instant.now());
        String json = JsonUtil.mapper().writeValueAsString(cmd);

        listener.onCommand(json);

        assertThat(drainingFlag.get()).isTrue();
    }

    @Test
    void drainCommandForOtherWorkerIgnored() throws Exception {
        AtomicBoolean drainingFlag = new AtomicBoolean(false);
        var listener = new WorkerCommandListener("worker-1", drainingFlag);

        var cmd = new DrainCommand("worker-2", 60, Instant.now());
        String json = JsonUtil.mapper().writeValueAsString(cmd);

        listener.onCommand(json);

        assertThat(drainingFlag.get()).isFalse();
    }
}
