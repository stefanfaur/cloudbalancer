package com.cloudbalancer.common.executor;

public record ResourceEstimate(int estimatedCpuCores, int estimatedMemoryMB, long estimatedDurationMs) {}
