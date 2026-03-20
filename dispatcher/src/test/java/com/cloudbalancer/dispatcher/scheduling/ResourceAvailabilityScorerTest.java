package com.cloudbalancer.dispatcher.scheduling;

import org.junit.jupiter.api.Test;

import static com.cloudbalancer.dispatcher.scheduling.FilterTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

class ResourceAvailabilityScorerTest {

    private final ResourceAvailabilityScorer scorer = new ResourceAvailabilityScorer();

    @Test
    void fullyFreeWorkerScoresHigh() {
        var worker = workerWithCapacity("w1", 8, 4096, 1000, 0, 0, 0);
        var score = scorer.score(anyTask(), worker);
        assertThat(score).isEqualTo(100);
    }

    @Test
    void fullyAllocatedWorkerScoresZero() {
        var worker = workerWithCapacity("w1", 8, 4096, 1000, 8, 4096, 1000);
        var score = scorer.score(anyTask(), worker);
        assertThat(score).isEqualTo(0);
    }

    @Test
    void halfAllocatedWorkerScoresAround50() {
        var worker = workerWithCapacity("w1", 8, 4096, 1000, 4, 2048, 500);
        var score = scorer.score(anyTask(), worker);
        assertThat(score).isBetween(45, 55);
    }

    @Test
    void scoreIsWithinRange() {
        var worker = workerWithCapacity("w1", 8, 4096, 1000, 2, 1024, 200);
        var score = scorer.score(anyTask(), worker);
        assertThat(score).isBetween(0, 100);
    }
}
