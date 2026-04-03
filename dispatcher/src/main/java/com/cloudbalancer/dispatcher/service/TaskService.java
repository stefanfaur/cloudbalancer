package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.api.dto.BulkResultEntry;
import com.cloudbalancer.dispatcher.api.dto.TaskPageResponse;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final AutoScalerService autoScalerService;

    public TaskService(TaskRepository taskRepository, AutoScalerService autoScalerService) {
        this.taskRepository = taskRepository;
        this.autoScalerService = autoScalerService;
    }

    public TaskEnvelope submitTask(TaskDescriptor descriptor) {
        TaskRecord record = TaskRecord.create(descriptor);
        record.transitionTo(TaskState.VALIDATED);
        record.transitionTo(TaskState.QUEUED);
        taskRepository.save(record);
        autoScalerService.recordTaskSubmitted();
        return record.toEnvelope();
    }

    public TaskEnvelope getTask(UUID id) {
        return taskRepository.findById(id)
            .map(TaskRecord::toEnvelope)
            .orElse(null);
    }

    public TaskRecord getTaskRecord(UUID id) {
        return taskRepository.findById(id).orElse(null);
    }

    public List<TaskEnvelope> listTasks() {
        return taskRepository.findAll().stream()
            .map(TaskRecord::toEnvelope)
            .toList();
    }

    public TaskPageResponse listTasks(int offset, int limit, TaskState status, Priority priority,
                                       ExecutorType executorType, String workerId, Instant since) {
        Specification<TaskRecord> spec = buildSpec(status, priority, executorType, workerId, since);
        long total = taskRepository.count(spec);
        int page = offset / Math.max(limit, 1);
        Page<TaskRecord> results = taskRepository.findAll(spec, PageRequest.of(page, limit));
        List<TaskEnvelope> envelopes = results.getContent().stream()
            .map(TaskRecord::toEnvelope)
            .toList();
        return new TaskPageResponse(envelopes, total, offset, limit);
    }

    private Specification<TaskRecord> buildSpec(TaskState status, Priority priority,
                                                 ExecutorType executorType, String workerId, Instant since) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("state"), status));
            if (priority != null) predicates.add(cb.equal(root.get("priority"), priority));
            if (executorType != null) predicates.add(cb.equal(root.get("executorType"), executorType));
            if (workerId != null) predicates.add(cb.equal(root.get("assignedWorkerId"), workerId));
            if (since != null) predicates.add(cb.greaterThanOrEqualTo(root.get("submittedAt"), since));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<BulkResultEntry> bulkCancel(List<UUID> taskIds) {
        List<BulkResultEntry> results = new ArrayList<>();
        for (UUID id : taskIds) {
            taskRepository.findById(id).ifPresentOrElse(record -> {
                if (record.getState().canTransitionTo(TaskState.CANCELLED)) {
                    record.transitionTo(TaskState.CANCELLED);
                    record.setCompletedAt(Instant.now());
                    taskRepository.save(record);
                    results.add(new BulkResultEntry(id, true, null));
                } else {
                    results.add(new BulkResultEntry(id, false, "Cannot cancel task in state " + record.getState()));
                }
            }, () -> results.add(new BulkResultEntry(id, false, "Task not found")));
        }
        return results;
    }

    public List<BulkResultEntry> bulkRetry(List<UUID> taskIds) {
        List<BulkResultEntry> results = new ArrayList<>();
        for (UUID id : taskIds) {
            taskRepository.findById(id).ifPresentOrElse(record -> {
                if (record.getState() == TaskState.FAILED || record.getState() == TaskState.TIMED_OUT) {
                    record.transitionTo(TaskState.QUEUED);
                    record.setAssignedWorkerId(null);
                    record.setCurrentExecutionId(UUID.randomUUID());
                    taskRepository.save(record);
                    results.add(new BulkResultEntry(id, true, null));
                } else {
                    results.add(new BulkResultEntry(id, false, "Cannot retry task in state " + record.getState()));
                }
            }, () -> results.add(new BulkResultEntry(id, false, "Task not found")));
        }
        return results;
    }

    public List<BulkResultEntry> bulkReprioritize(List<UUID> taskIds, Priority priority) {
        List<BulkResultEntry> results = new ArrayList<>();
        for (UUID id : taskIds) {
            taskRepository.findById(id).ifPresentOrElse(record -> {
                if (!record.getState().isTerminal()) {
                    record.setPriority(priority);
                    taskRepository.save(record);
                    results.add(new BulkResultEntry(id, true, null));
                } else {
                    results.add(new BulkResultEntry(id, false, "Cannot reprioritize task in terminal state " + record.getState()));
                }
            }, () -> results.add(new BulkResultEntry(id, false, "Task not found")));
        }
        return results;
    }

    public List<TaskRecord> getQueuedTasks() {
        var queued = taskRepository.findByState(TaskState.QUEUED);
        queued.sort(Comparator.comparingInt((TaskRecord t) -> t.getPriority().ordinal())
            .thenComparing(TaskRecord::getSubmittedAt));
        return queued;
    }

    public void updateTask(TaskRecord record) {
        taskRepository.save(record);
    }
}
