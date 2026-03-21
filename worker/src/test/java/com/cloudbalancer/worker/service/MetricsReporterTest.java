package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.event.WorkerHeartbeatEvent;
import com.cloudbalancer.common.event.WorkerMetricsEvent;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.common.model.WorkerMetrics;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.worker.config.WorkerProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsReporterTest {

    private static final String WORKER_ID = "test-worker-42";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<String> valueCaptor;

    private WorkerProperties properties;
    private MetricsReporter reporter;

    @BeforeEach
    void setUp() {
        // Make circuit breaker execute the runnable passed to it (lenient for tests that don't publish)
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(circuitBreaker).executeRunnable(any(Runnable.class));

        properties = new WorkerProperties();
        properties.setId(WORKER_ID);
        properties.setMetricsIntervalMs(5000);
        properties.setHeartbeatIntervalMs(10000);
        reporter = new MetricsReporter(kafkaTemplate, properties, taskExecutionService, circuitBreaker);
    }

    @Test
    void metricsPublishedToCorrectTopicWithReasonableJvmValues() throws Exception {
        when(taskExecutionService.getActiveTaskCount()).thenReturn(0);
        when(taskExecutionService.getCompletedTaskCount()).thenReturn(0L);
        when(taskExecutionService.getFailedTaskCount()).thenReturn(0L);
        when(taskExecutionService.getAverageExecutionDurationMs()).thenReturn(0.0);

        reporter.publishMetrics();

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("workers.metrics");
        assertThat(keyCaptor.getValue()).isEqualTo(WORKER_ID);

        WorkerMetricsEvent event = JsonUtil.mapper().readValue(
                valueCaptor.getValue(), WorkerMetricsEvent.class);
        assertThat(event.workerId()).isEqualTo(WORKER_ID);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.timestamp()).isNotNull();

        WorkerMetrics metrics = event.metrics();
        assertThat(metrics.cpuUsagePercent()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.heapUsedMB()).isGreaterThan(0);
        assertThat(metrics.heapMaxMB()).isGreaterThan(0);
        assertThat(metrics.threadCount()).isGreaterThan(0);
    }

    @Test
    void heartbeatPublishedToCorrectTopicWithHealthyState() throws Exception {
        reporter.publishHeartbeat();

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("workers.heartbeat");
        assertThat(keyCaptor.getValue()).isEqualTo(WORKER_ID);

        WorkerHeartbeatEvent event = JsonUtil.mapper().readValue(
                valueCaptor.getValue(), WorkerHeartbeatEvent.class);
        assertThat(event.workerId()).isEqualTo(WORKER_ID);
        assertThat(event.healthState()).isEqualTo(WorkerHealthState.HEALTHY);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void taskCountersReflectedInMetrics() throws Exception {
        when(taskExecutionService.getActiveTaskCount()).thenReturn(3);
        when(taskExecutionService.getCompletedTaskCount()).thenReturn(42L);
        when(taskExecutionService.getFailedTaskCount()).thenReturn(7L);
        when(taskExecutionService.getAverageExecutionDurationMs()).thenReturn(150.5);

        reporter.publishMetrics();

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        WorkerMetricsEvent event = JsonUtil.mapper().readValue(
                valueCaptor.getValue(), WorkerMetricsEvent.class);
        WorkerMetrics metrics = event.metrics();

        assertThat(metrics.activeTaskCount()).isEqualTo(3);
        assertThat(metrics.completedTaskCount()).isEqualTo(42L);
        assertThat(metrics.failedTaskCount()).isEqualTo(7L);
        assertThat(metrics.averageExecutionDurationMs()).isEqualTo(150.5);
    }

    @Test
    void metricsIntervalConfigurable() {
        WorkerProperties customProps = new WorkerProperties();
        customProps.setId("custom-worker");
        customProps.setMetricsIntervalMs(2000);
        customProps.setHeartbeatIntervalMs(3000);

        assertThat(customProps.getMetricsIntervalMs()).isEqualTo(2000);
        assertThat(customProps.getHeartbeatIntervalMs()).isEqualTo(3000);

        // Verify defaults
        WorkerProperties defaultProps = new WorkerProperties();
        assertThat(defaultProps.getMetricsIntervalMs()).isEqualTo(5000);
        assertThat(defaultProps.getHeartbeatIntervalMs()).isEqualTo(10000);
    }
}
