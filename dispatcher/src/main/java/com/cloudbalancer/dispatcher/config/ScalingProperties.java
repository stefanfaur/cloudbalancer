package com.cloudbalancer.dispatcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudbalancer.dispatcher.scaling")
public class ScalingProperties {
    private boolean enabled = true;
    private String runtimeMode = "LOCAL";
    private long evaluationIntervalMs = 30000;
    private double cpuHighThreshold = 80.0;
    private double cpuLowThreshold = 30.0;
    private int reactiveWindowSeconds = 120;
    private int scaleDownWindowSeconds = 300;
    private int queuePressureWindowSeconds = 120;
    private double queuePressureRatioThreshold = 1.5;
    private String processWorkerJarPath = "worker/build/libs/worker.jar";
    private String processWorkerKafkaBootstrap = "localhost:9092";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getRuntimeMode() { return runtimeMode; }
    public void setRuntimeMode(String runtimeMode) { this.runtimeMode = runtimeMode; }
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
    public String getProcessWorkerJarPath() { return processWorkerJarPath; }
    public void setProcessWorkerJarPath(String processWorkerJarPath) { this.processWorkerJarPath = processWorkerJarPath; }
    public String getProcessWorkerKafkaBootstrap() { return processWorkerKafkaBootstrap; }
    public void setProcessWorkerKafkaBootstrap(String processWorkerKafkaBootstrap) { this.processWorkerKafkaBootstrap = processWorkerKafkaBootstrap; }
}
