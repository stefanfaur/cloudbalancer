package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskEnvelope submitTask(TaskDescriptor descriptor) {
        TaskRecord record = TaskRecord.create(descriptor);
        record.transitionTo(TaskState.VALIDATED);
        record.transitionTo(TaskState.QUEUED);
        taskRepository.save(record);
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
