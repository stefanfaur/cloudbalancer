package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.WorkerCapabilities;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static com.cloudbalancer.dispatcher.scheduling.FilterTestHelper.anyTask;
import static org.assertj.core.api.Assertions.assertThat;

class QueueDepthScorerTest {

    private final QueueDepthScorer scorer = new QueueDepthScorer();

    @Test
    void zeroActiveTasksScoresHigh() {
        var worker = workerWithActiveTasks(0);
        assertThat(scorer.score(anyTask(), worker)).isEqualTo(100);
    }

    @Test
    void manyActiveTasksScoresLow() {
        var worker = workerWithActiveTasks(50);
        assertThat(scorer.score(anyTask(), worker)).isEqualTo(50);
    }

    @Test
    void maxActiveTasksScoresZero() {
        var worker = workerWithActiveTasks(100);
        assertThat(scorer.score(anyTask(), worker)).isEqualTo(0);
    }

    @Test
    void singleActiveTaskScoresNear100() {
        var worker = workerWithActiveTasks(1);
        assertThat(scorer.score(anyTask(), worker)).isEqualTo(99);
    }

    private WorkerRecord workerWithActiveTasks(int count) {
        var record = new WorkerRecord("w1", WorkerHealthState.HEALTHY,
            new WorkerCapabilities(Set.of(ExecutorType.SIMULATED),
                new ResourceProfile(8, 4096, 1000, false, 0, false), Set.of()),
            Instant.now());
        // Allocate resources N times to simulate active tasks
        for (int i = 0; i < count; i++) {
            record.allocateResources(new ResourceProfile(0, 0, 0, false, 0, false));
        }
        return record;
    }
}
