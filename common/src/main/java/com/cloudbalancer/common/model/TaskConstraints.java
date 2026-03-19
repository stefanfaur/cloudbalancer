package com.cloudbalancer.common.model;

import java.util.Set;

public record TaskConstraints(
    Set<String> requiredTags,
    Set<String> blacklistedWorkers,
    Set<String> whitelistedWorkers
) {
    public static TaskConstraints unconstrained() {
        return new TaskConstraints(Set.of(), Set.of(), Set.of());
    }
}
