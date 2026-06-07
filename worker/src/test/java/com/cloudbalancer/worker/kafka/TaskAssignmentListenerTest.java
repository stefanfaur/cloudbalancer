package com.cloudbalancer.worker.kafka;

import com.cloudbalancer.worker.service.TaskExecutionService;
import com.cloudbalancer.worker.service.WorkerRegistrationService;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.ConsumerSeekAware;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAssignmentListenerTest {

    @Mock
    private TaskExecutionService executionService;

    @Mock
    private WorkerRegistrationService registrationService;

    @Mock
    private ConsumerSeekAware.ConsumerSeekCallback seekCallback;

    private TaskAssignmentListener listener;

    @BeforeEach
    void setUp() {
        listener = new TaskAssignmentListener(
            executionService, registrationService, new AtomicBoolean(false), "worker-1");
    }

    @Test
    void registersWorkerOnceAssignmentPartitionsAreAssigned() {
        // The consumer's position is fixed at partition assignment; only then
        // is it safe to announce readiness — assignments published earlier
        // would be below the 'latest' reset position and silently lost.
        listener.onPartitionsAssigned(
            Map.of(new TopicPartition("tasks.assigned", 0), 0L), seekCallback);

        verify(registrationService).registerOnce();
    }

    @Test
    void everyRebalanceDelegatesToIdempotentRegistration() {
        listener.onPartitionsAssigned(
            Map.of(new TopicPartition("tasks.assigned", 0), 0L), seekCallback);
        listener.onPartitionsAssigned(
            Map.of(new TopicPartition("tasks.assigned", 0), 5L), seekCallback);

        // delegate every time; WorkerRegistrationService.registerOnce dedupes
        verify(registrationService, times(2)).registerOnce();
    }
}
