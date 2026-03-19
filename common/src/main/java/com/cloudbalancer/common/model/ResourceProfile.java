package com.cloudbalancer.common.model;

public record ResourceProfile(
    int cpuCores,
    int memoryMB,
    int diskMB,
    boolean gpuRequired,
    int estimatedDurationSeconds,
    boolean networkAccessRequired
) {}
