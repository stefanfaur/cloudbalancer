package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.WorkerCapabilities;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class WorkerRecordRepositoryTest {

    @Autowired
    WorkerRepository workerRepository;

    @BeforeEach
    void cleanUp() {
        workerRepository.deleteAll();
    }

    @Test
    void persistAndRetrieveWithCapabilities() {
        var caps = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED, ExecutorType.SHELL),
            new ResourceProfile(8, 4096, 1000, false, 0, false),
            Set.of("region-eu")
        );
        var record = new WorkerRecord("worker-1", WorkerHealthState.HEALTHY, caps, Instant.now());
        workerRepository.save(record);

        var found = workerRepository.findById("worker-1").orElseThrow();
        assertThat(found.getCapabilities().supportedExecutors())
            .containsExactlyInAnyOrder(ExecutorType.SIMULATED, ExecutorType.SHELL);
        assertThat(found.getCapabilities().totalResources().cpuCores()).isEqualTo(8);
        assertThat(found.getCapabilities().tags()).containsExactly("region-eu");
        assertThat(found.getAllocatedCpu()).isEqualTo(0);
        assertThat(found.getActiveTaskCount()).isEqualTo(0);
    }

    @Test
    void findByHealthState() {
        workerRepository.save(new WorkerRecord("w-healthy", WorkerHealthState.HEALTHY,
            minCaps(), Instant.now()));
        workerRepository.save(new WorkerRecord("w-dead", WorkerHealthState.DEAD,
            minCaps(), Instant.now()));
        workerRepository.save(new WorkerRecord("w-suspect", WorkerHealthState.SUSPECT,
            minCaps(), Instant.now()));

        var healthy = workerRepository.findByHealthState(WorkerHealthState.HEALTHY);
        assertThat(healthy).hasSize(1);
        assertThat(healthy.getFirst().getId()).isEqualTo("w-healthy");
    }

    @Test
    void resourceLedgerAllocateAndRelease() {
        var record = new WorkerRecord("w1", WorkerHealthState.HEALTHY, minCaps(), Instant.now());
        var profile = new ResourceProfile(2, 512, 100, false, 60, false);

        record.allocateResources(profile);
        assertThat(record.getAllocatedCpu()).isEqualTo(2);
        assertThat(record.getAllocatedMemoryMb()).isEqualTo(512);
        assertThat(record.getAllocatedDiskMb()).isEqualTo(100);
        assertThat(record.getActiveTaskCount()).isEqualTo(1);

        record.releaseResources(profile);
        assertThat(record.getAllocatedCpu()).isEqualTo(0);
        assertThat(record.getAllocatedMemoryMb()).isEqualTo(0);
        assertThat(record.getAllocatedDiskMb()).isEqualTo(0);
        assertThat(record.getActiveTaskCount()).isEqualTo(0);
    }

    private WorkerCapabilities minCaps() {
        return new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED),
            new ResourceProfile(4, 2048, 500, false, 0, false),
            Set.of()
        );
    }
}
