package com.cloudbalancer.dispatcher.scaling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PendingWorkerTrackerTest {

    private PendingWorkerTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new PendingWorkerTracker();
    }

    @Test
    void markPendingIncreasesCount() {
        tracker.markPending("w-1", "agent-1", Instant.now());
        assertThat(tracker.pendingCount()).isEqualTo(1);
    }

    @Test
    void resolveDecreasesCount() {
        tracker.markPending("w-1", "agent-1", Instant.now());
        tracker.resolve("w-1");
        assertThat(tracker.pendingCount()).isZero();
    }

    @Test
    void failDecreasesCount() {
        tracker.markPending("w-1", "agent-1", Instant.now());
        tracker.fail("w-1");
        assertThat(tracker.pendingCount()).isZero();
    }

    @Test
    void expireStaleRemovesOldEntries() {
        tracker.markPending("w-1", "agent-1", Instant.now().minus(Duration.ofSeconds(120)));
        tracker.expireStale(Duration.ofSeconds(60));
        assertThat(tracker.pendingCount()).isZero();
    }

    @Test
    void resolveUnknownWorkerIsNoOp() {
        tracker.resolve("unknown");
        assertThat(tracker.pendingCount()).isZero();
    }
}
