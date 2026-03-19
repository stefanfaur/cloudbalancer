package com.cloudbalancer.common.model;

public record ExecutorCapabilities(
    boolean requiresDocker,
    boolean requiresNetworkAccess,
    ResourceProfile maxResourceCeiling,
    SecurityLevel securityLevel
) {}
