package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskService {

    private final Map<UUID, TaskEnvelope> tasks = new ConcurrentHashMap<>();

    public TaskEnvelope submitTask(TaskDescriptor descriptor) {
        TaskEnvelope envelope = TaskEnvelope.create(descriptor);
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        tasks.put(envelope.getId(), envelope);
        return envelope;
    }

    public TaskEnvelope getTask(UUID id) {
        return tasks.get(id);
    }

    public List<TaskEnvelope> listTasks() {
        return List.copyOf(tasks.values());
    }

    public List<TaskEnvelope> getQueuedTasks() {
        return tasks.values().stream()
            .filter(e -> e.getState() == TaskState.QUEUED)
            .toList();
    }
}
