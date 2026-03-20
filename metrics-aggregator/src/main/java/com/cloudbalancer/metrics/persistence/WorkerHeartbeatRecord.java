package com.cloudbalancer.metrics.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "worker_heartbeats", schema = "metrics")
public class WorkerHeartbeatRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", nullable = false)
    private String workerId;

    @Column(name = "health_state", nullable = false)
    private String healthState;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public WorkerHeartbeatRecord() {}

    public Long getId() { return id; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getHealthState() { return healthState; }
    public void setHealthState(String healthState) { this.healthState = healthState; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
