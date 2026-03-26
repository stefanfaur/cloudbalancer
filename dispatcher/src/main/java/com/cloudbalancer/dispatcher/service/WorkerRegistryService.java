package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.TaskState;
import com.cloudbalancer.common.model.WorkerCapabilities;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class WorkerRegistryService {

    private static final Logger log = LoggerFactory.getLogger(WorkerRegistryService.class);

    private final WorkerRepository workerRepository;
    private final TaskRepository taskRepository;

    public WorkerRegistryService(WorkerRepository workerRepository, TaskRepository taskRepository) {
        this.workerRepository = workerRepository;
        this.taskRepository = taskRepository;
    }

    public void registerWorker(String workerId, WorkerHealthState healthState,
                               WorkerCapabilities capabilities) {
        var existing = workerRepository.findById(workerId);
        if (existing.isPresent()) {
            var record = existing.get();
            if (record.getHealthState() == WorkerHealthState.DEAD) {
                // Previously dead worker returning — soft-start
                record.setHealthState(WorkerHealthState.RECOVERING);
                record.setRecoveryStartedAt(Instant.now());
                log.info("Worker {} recovered from DEAD -> RECOVERING", workerId);
            } else {
                record.setHealthState(healthState);
            }
            record.setCapabilities(capabilities);
            record.setRegisteredAt(Instant.now());
            workerRepository.save(record);
        } else {
            // New worker — no soft-start needed
            var record = new WorkerRecord(workerId, healthState, capabilities, Instant.now());
            workerRepository.save(record);
        }
        log.info("Worker registered/updated: {}", workerId);
    }

    public WorkerRecord getWorker(String workerId) {
        return workerRepository.findById(workerId).orElse(null);
    }

    public List<WorkerRecord> getAvailableWorkers() {
        var healthy = workerRepository.findByHealthState(WorkerHealthState.HEALTHY);
        var recovering = workerRepository.findByHealthState(WorkerHealthState.RECOVERING);
        var combined = new ArrayList<>(healthy);
        combined.addAll(recovering);
        return combined;
    }

    public List<WorkerRecord> getAllWorkers() {
        return workerRepository.findAll();
    }

    public void drainWorker(String workerId) {
        var worker = workerRepository.findById(workerId).orElseThrow(
            () -> new IllegalArgumentException("Worker not found: " + workerId));
        worker.setHealthState(WorkerHealthState.DRAINING);
        workerRepository.save(worker);
        log.info("Worker {} transitioned to DRAINING", workerId);
    }

    public void allocateResources(String workerId, ResourceProfile profile) {
        var worker = workerRepository.findById(workerId).orElseThrow(
            () -> new IllegalArgumentException("Worker not found: " + workerId));
        worker.allocateResources(profile);
        workerRepository.save(worker);
    }

    public void releaseResources(String workerId, ResourceProfile profile) {
        var worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            log.warn("Cannot release resources: worker {} not found", workerId);
            return;
        }
        worker.releaseResources(profile);
        workerRepository.save(worker);
    }

    public void rebuildResourceLedger() {
        log.info("Rebuilding resource ledger from persisted tasks...");
        var allWorkers = workerRepository.findAll();
        for (var worker : allWorkers) {
            worker.resetLedger();
        }
        workerRepository.saveAll(allWorkers);

        var activeTasks = taskRepository.findByStateIn(
            Set.of(TaskState.ASSIGNED, TaskState.PROVISIONING, TaskState.RUNNING));
        for (TaskRecord task : activeTasks) {
            if (task.getAssignedWorkerId() != null) {
                var worker = workerRepository.findById(task.getAssignedWorkerId()).orElse(null);
                if (worker != null) {
                    worker.allocateResources(task.getDescriptor().resourceProfile());
                    workerRepository.save(worker);
                }
            }
        }
        log.info("Resource ledger rebuilt: {} active tasks across {} workers",
            activeTasks.size(), allWorkers.size());
    }
}
