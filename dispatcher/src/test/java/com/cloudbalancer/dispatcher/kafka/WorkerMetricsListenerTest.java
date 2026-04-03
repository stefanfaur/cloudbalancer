package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.event.WorkerMetricsEvent;
import com.cloudbalancer.common.model.WorkerMetrics;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.service.AutoScalerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerMetricsListenerTest {

    @Mock AutoScalerService autoScalerService;
    @InjectMocks WorkerMetricsListener listener;

    @Test
    void forwardsCpuMetricsToAutoScaler() throws Exception {
        var metrics = new WorkerMetrics(85.5, 512, 1024, 20, 3, 100, 5, 150.0, Instant.now());
        var event = new WorkerMetricsEvent(UUID.randomUUID().toString(), Instant.now(), "worker-1", metrics);
        String json = JsonUtil.mapper().writeValueAsString(event);

        listener.onMetrics(json);

        verify(autoScalerService).recordMetrics("worker-1", 85.5);
    }

    @Test
    void handlesInvalidJsonGracefully() {
        listener.onMetrics("not valid json");

        verifyNoInteractions(autoScalerService);
    }
}
