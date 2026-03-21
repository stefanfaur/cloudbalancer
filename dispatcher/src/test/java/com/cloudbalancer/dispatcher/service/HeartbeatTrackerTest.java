package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeartbeatTrackerTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private WorkerFailureHandler workerFailureHandler;

    private HeartbeatTracker tracker;

    private static final long SUSPECT_THRESHOLD = 30;
    private static final long DEAD_THRESHOLD = 60;

    @BeforeEach
    void setUp() {
        tracker = new HeartbeatTracker(workerRepository, workerFailureHandler, SUSPECT_THRESHOLD, DEAD_THRESHOLD);
    }

    @Test
    void heartbeatUpdatesLastSeenAt() {
        String workerId = "worker-1";
        Instant now = Instant.now();

        when(workerRepository.findById(workerId)).thenReturn(Optional.empty());

        tracker.onHeartbeat(workerId, now);

        assertThat(tracker.getLastSeenMap()).containsKey(workerId);
        assertThat(tracker.getLastSeenMap().get(workerId)).isEqualTo(now);
    }

    @Test
    void workerTransitionsToSuspectAfter30Seconds() {
        String workerId = "worker-1";
        WorkerRecord worker = createWorker(workerId, WorkerHealthState.HEALTHY);

        // Register heartbeat 35 seconds ago
        Instant thirtyFiveSecondsAgo = Instant.now().minusSeconds(35);
        tracker.getLastSeenMap().put(workerId, thirtyFiveSecondsAgo);

        when(workerRepository.findAll()).thenReturn(List.of(worker));

        tracker.checkLiveness();

        ArgumentCaptor<WorkerRecord> captor = ArgumentCaptor.forClass(WorkerRecord.class);
        verify(workerRepository).save(captor.capture());
        assertThat(captor.getValue().getHealthState()).isEqualTo(WorkerHealthState.SUSPECT);
    }

    @Test
    void workerTransitionsToDeadAfter60Seconds() {
        String workerId = "worker-1";
        WorkerRecord worker = createWorker(workerId, WorkerHealthState.HEALTHY);

        // Register heartbeat 65 seconds ago
        Instant sixtyFiveSecondsAgo = Instant.now().minusSeconds(65);
        tracker.getLastSeenMap().put(workerId, sixtyFiveSecondsAgo);

        when(workerRepository.findAll()).thenReturn(List.of(worker));

        tracker.checkLiveness();

        ArgumentCaptor<WorkerRecord> captor = ArgumentCaptor.forClass(WorkerRecord.class);
        verify(workerRepository).save(captor.capture());
        assertThat(captor.getValue().getHealthState()).isEqualTo(WorkerHealthState.DEAD);
        verify(workerFailureHandler).onWorkerDead(workerId);
    }

    @Test
    void workerRecoversFromSuspectToHealthy() {
        String workerId = "worker-1";
        WorkerRecord worker = createWorker(workerId, WorkerHealthState.SUSPECT);

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(worker));

        tracker.onHeartbeat(workerId, Instant.now());

        ArgumentCaptor<WorkerRecord> captor = ArgumentCaptor.forClass(WorkerRecord.class);
        verify(workerRepository).save(captor.capture());
        assertThat(captor.getValue().getHealthState()).isEqualTo(WorkerHealthState.HEALTHY);
    }

    @Test
    void gracePeriodForNewWorkers() {
        // Worker just registered, no heartbeat received yet
        String workerId = "worker-new";
        WorkerRecord worker = createWorker(workerId, WorkerHealthState.HEALTHY);
        // registeredAt is Instant.now() (just created), no entry in lastSeenMap

        when(workerRepository.findAll()).thenReturn(List.of(worker));

        tracker.checkLiveness();

        // Worker should remain HEALTHY — grace period uses registeredAt as fallback
        verify(workerRepository, never()).save(any());
        assertThat(worker.getHealthState()).isEqualTo(WorkerHealthState.HEALTHY);
    }

    @Test
    void alreadyDeadWorkerStaysDeadOnLivenessCheck() {
        String workerId = "worker-dead";
        WorkerRecord worker = createWorker(workerId, WorkerHealthState.DEAD);

        // Last seen 120 seconds ago
        Instant longAgo = Instant.now().minusSeconds(120);
        tracker.getLastSeenMap().put(workerId, longAgo);

        when(workerRepository.findAll()).thenReturn(List.of(worker));

        tracker.checkLiveness();

        // Already DEAD — no additional save/transition should happen
        verify(workerRepository, never()).save(any());
    }

    private WorkerRecord createWorker(String id, WorkerHealthState healthState) {
        var caps = new WorkerCapabilities(
                Set.of(ExecutorType.SIMULATED),
                new ResourceProfile(4, 8192, 10240, false, 0, true),
                Set.of()
        );
        return new WorkerRecord(id, healthState, caps, Instant.now());
    }
}
