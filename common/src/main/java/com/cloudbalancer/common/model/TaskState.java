package com.cloudbalancer.common.model;

import java.util.Map;
import java.util.Set;

public enum TaskState {
    SUBMITTED, VALIDATED, QUEUED, ASSIGNED, PROVISIONING,
    RUNNING, POST_PROCESSING, COMPLETED, FAILED, TIMED_OUT, CANCELLED, DEAD_LETTERED;

    private static final Map<TaskState, Set<TaskState>> VALID_TRANSITIONS = Map.ofEntries(
        Map.entry(SUBMITTED,        Set.of(VALIDATED, FAILED, CANCELLED)),
        Map.entry(VALIDATED,        Set.of(QUEUED, CANCELLED)),
        Map.entry(QUEUED,           Set.of(ASSIGNED, CANCELLED)),
        Map.entry(ASSIGNED,         Set.of(PROVISIONING, CANCELLED)),
        Map.entry(PROVISIONING,     Set.of(RUNNING, FAILED, CANCELLED)),
        Map.entry(RUNNING,          Set.of(POST_PROCESSING, FAILED, TIMED_OUT, CANCELLED)),
        Map.entry(POST_PROCESSING,  Set.of(COMPLETED, FAILED)),
        Map.entry(COMPLETED,        Set.of()),
        Map.entry(FAILED,           Set.of(QUEUED, DEAD_LETTERED)),  // FAILED -> QUEUED for retries, DEAD_LETTERED when exhausted
        Map.entry(TIMED_OUT,        Set.of(QUEUED, DEAD_LETTERED)),  // TIMED_OUT -> QUEUED for retries, DEAD_LETTERED when exhausted
        Map.entry(CANCELLED,        Set.of()),
        Map.entry(DEAD_LETTERED,    Set.of())
    );

    public boolean canTransitionTo(TaskState target) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == TIMED_OUT || this == FAILED || this == DEAD_LETTERED;
    }
}
