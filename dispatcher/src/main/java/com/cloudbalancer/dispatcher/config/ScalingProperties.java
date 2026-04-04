package com.cloudbalancer.dispatcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudbalancer.dispatcher.scaling")
public class ScalingProperties {
    private boolean enabled = true;
    private long evaluationIntervalMs = 30000;
    private double cpuHighThreshold = 80.0;
    private double cpuLowThreshold = 30.0;
    private int reactiveWindowSeconds = 120;
    private int scaleDownWindowSeconds = 300;
    private int queuePressureWindowSeconds = 120;
    private double queuePressureRatioThreshold = 1.5;

    // Docker runtime config
    private String dockerWorkerImage = "cloudbalancer-worker";
    private String dockerNetworkName = "docker_default";
    private String kafkaBootstrapInternal = "kafka:29092";
    private int drainTimeSeconds = 60;

    // Default worker resource config
    private String defaultWorkerExecutorTypes = "SIMULATED,DOCKER,SHELL";
    private int defaultWorkerCpuCores = 4;
    private int defaultWorkerMemoryMb = 8192;
    private int defaultWorkerDiskMb = 10240;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getEvaluationIntervalMs() { return evaluationIntervalMs; }
    public void setEvaluationIntervalMs(long evaluationIntervalMs) { this.evaluationIntervalMs = evaluationIntervalMs; }
    public double getCpuHighThreshold() { return cpuHighThreshold; }
    public void setCpuHighThreshold(double cpuHighThreshold) { this.cpuHighThreshold = cpuHighThreshold; }
    public double getCpuLowThreshold() { return cpuLowThreshold; }
    public void setCpuLowThreshold(double cpuLowThreshold) { this.cpuLowThreshold = cpuLowThreshold; }
    public int getReactiveWindowSeconds() { return reactiveWindowSeconds; }
    public void setReactiveWindowSeconds(int reactiveWindowSeconds) { this.reactiveWindowSeconds = reactiveWindowSeconds; }
    public int getScaleDownWindowSeconds() { return scaleDownWindowSeconds; }
    public void setScaleDownWindowSeconds(int scaleDownWindowSeconds) { this.scaleDownWindowSeconds = scaleDownWindowSeconds; }
    public int getQueuePressureWindowSeconds() { return queuePressureWindowSeconds; }
    public void setQueuePressureWindowSeconds(int queuePressureWindowSeconds) { this.queuePressureWindowSeconds = queuePressureWindowSeconds; }
    public double getQueuePressureRatioThreshold() { return queuePressureRatioThreshold; }
    public void setQueuePressureRatioThreshold(double queuePressureRatioThreshold) { this.queuePressureRatioThreshold = queuePressureRatioThreshold; }

    public String getDockerWorkerImage() { return dockerWorkerImage; }
    public void setDockerWorkerImage(String dockerWorkerImage) { this.dockerWorkerImage = dockerWorkerImage; }
    public String getDockerNetworkName() { return dockerNetworkName; }
    public void setDockerNetworkName(String dockerNetworkName) { this.dockerNetworkName = dockerNetworkName; }
    public String getKafkaBootstrapInternal() { return kafkaBootstrapInternal; }
    public void setKafkaBootstrapInternal(String kafkaBootstrapInternal) { this.kafkaBootstrapInternal = kafkaBootstrapInternal; }
    public int getDrainTimeSeconds() { return drainTimeSeconds; }
    public void setDrainTimeSeconds(int drainTimeSeconds) { this.drainTimeSeconds = drainTimeSeconds; }

    public String getDefaultWorkerExecutorTypes() { return defaultWorkerExecutorTypes; }
    public void setDefaultWorkerExecutorTypes(String defaultWorkerExecutorTypes) { this.defaultWorkerExecutorTypes = defaultWorkerExecutorTypes; }
    public int getDefaultWorkerCpuCores() { return defaultWorkerCpuCores; }
    public void setDefaultWorkerCpuCores(int defaultWorkerCpuCores) { this.defaultWorkerCpuCores = defaultWorkerCpuCores; }
    public int getDefaultWorkerMemoryMb() { return defaultWorkerMemoryMb; }
    public void setDefaultWorkerMemoryMb(int defaultWorkerMemoryMb) { this.defaultWorkerMemoryMb = defaultWorkerMemoryMb; }
    public int getDefaultWorkerDiskMb() { return defaultWorkerDiskMb; }
    public void setDefaultWorkerDiskMb(int defaultWorkerDiskMb) { this.defaultWorkerDiskMb = defaultWorkerDiskMb; }
}
