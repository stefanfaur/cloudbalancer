package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.WorkerCapabilities;
import com.cloudbalancer.common.model.WorkerHealthState;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "workers")
public class WorkerRecord {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_state", nullable = false)
    private WorkerHealthState healthState;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Convert(converter = WorkerCapabilitiesConverter.class)
    private WorkerCapabilities capabilities;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "allocated_cpu", nullable = false)
    private int allocatedCpu = 0;

    @Column(name = "allocated_memory_mb", nullable = false)
    private int allocatedMemoryMb = 0;

    @Column(name = "allocated_disk_mb", nullable = false)
    private int allocatedDiskMb = 0;

    @Column(name = "active_task_count", nullable = false)
    private int activeTaskCount = 0;

    @Column(name = "recovery_started_at")
    private Instant recoveryStartedAt;

    @Column(name = "drain_started_at")
    private Instant drainStartedAt;

    protected WorkerRecord() {}

    public WorkerRecord(String id, WorkerHealthState healthState,
                        WorkerCapabilities capabilities, Instant registeredAt) {
        this.id = id;
        this.healthState = healthState;
        this.capabilities = capabilities;
        this.registeredAt = registeredAt;
    }

    public void allocateResources(ResourceProfile profile) {
        if (profile != null) {
            this.allocatedCpu += profile.cpuCores();
            this.allocatedMemoryMb += profile.memoryMB();
            this.allocatedDiskMb += profile.diskMB();
        }
        this.activeTaskCount++;
    }

    public void releaseResources(ResourceProfile profile) {
        if (profile != null) {
            this.allocatedCpu = Math.max(0, this.allocatedCpu - profile.cpuCores());
            this.allocatedMemoryMb = Math.max(0, this.allocatedMemoryMb - profile.memoryMB());
            this.allocatedDiskMb = Math.max(0, this.allocatedDiskMb - profile.diskMB());
        }
        this.activeTaskCount = Math.max(0, this.activeTaskCount - 1);
    }

    public void resetLedger() {
        this.allocatedCpu = 0;
        this.allocatedMemoryMb = 0;
        this.allocatedDiskMb = 0;
        this.activeTaskCount = 0;
    }

    public String getId() { return id; }
    public WorkerHealthState getHealthState() { return healthState; }
    public void setHealthState(WorkerHealthState healthState) { this.healthState = healthState; }
    public WorkerCapabilities getCapabilities() { return capabilities; }
    public void setCapabilities(WorkerCapabilities capabilities) { this.capabilities = capabilities; }
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
    public int getAllocatedCpu() { return allocatedCpu; }
    public int getAllocatedMemoryMb() { return allocatedMemoryMb; }
    public int getAllocatedDiskMb() { return allocatedDiskMb; }
    public int getActiveTaskCount() { return activeTaskCount; }
    public Instant getRecoveryStartedAt() { return recoveryStartedAt; }
    public void setRecoveryStartedAt(Instant recoveryStartedAt) { this.recoveryStartedAt = recoveryStartedAt; }
    public Instant getDrainStartedAt() { return drainStartedAt; }
    public void setDrainStartedAt(Instant drainStartedAt) { this.drainStartedAt = drainStartedAt; }
}
