package com.cloudbalancer.dispatcher.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "scaling_policy")
public class ScalingPolicyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_workers", nullable = false)
    private int minWorkers = 2;

    @Column(name = "max_workers", nullable = false)
    private int maxWorkers = 20;

    @Column(name = "cooldown_seconds", nullable = false)
    private int cooldownSeconds = 180;

    @Column(name = "scale_up_step", nullable = false)
    private int scaleUpStep = 1;

    @Column(name = "scale_down_step", nullable = false)
    private int scaleDownStep = 1;

    @Column(name = "drain_time_seconds", nullable = false)
    private int drainTimeSeconds = 60;

    protected ScalingPolicyRecord() {}

    public ScalingPolicyRecord(int minWorkers, int maxWorkers, int cooldownSeconds,
                               int scaleUpStep, int scaleDownStep, int drainTimeSeconds) {
        this.minWorkers = minWorkers;
        this.maxWorkers = maxWorkers;
        this.cooldownSeconds = cooldownSeconds;
        this.scaleUpStep = scaleUpStep;
        this.scaleDownStep = scaleDownStep;
        this.drainTimeSeconds = drainTimeSeconds;
    }

    public Long getId() { return id; }
    public int getMinWorkers() { return minWorkers; }
    public void setMinWorkers(int minWorkers) { this.minWorkers = minWorkers; }
    public int getMaxWorkers() { return maxWorkers; }
    public void setMaxWorkers(int maxWorkers) { this.maxWorkers = maxWorkers; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public int getScaleUpStep() { return scaleUpStep; }
    public void setScaleUpStep(int scaleUpStep) { this.scaleUpStep = scaleUpStep; }
    public int getScaleDownStep() { return scaleDownStep; }
    public void setScaleDownStep(int scaleDownStep) { this.scaleDownStep = scaleDownStep; }
    public int getDrainTimeSeconds() { return drainTimeSeconds; }
    public void setDrainTimeSeconds(int drainTimeSeconds) { this.drainTimeSeconds = drainTimeSeconds; }
}
