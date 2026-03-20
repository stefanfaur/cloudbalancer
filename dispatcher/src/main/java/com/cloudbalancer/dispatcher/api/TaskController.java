package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.TaskDescriptor;
import com.cloudbalancer.common.model.TaskEnvelope;
import com.cloudbalancer.dispatcher.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskEnvelope> submitTask(@RequestBody TaskDescriptor descriptor) {
        if (descriptor.executorType() == null) {
            return ResponseEntity.badRequest().build();
        }
        TaskEnvelope envelope = taskService.submitTask(descriptor);
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskEnvelope> getTask(@PathVariable UUID id) {
        TaskEnvelope envelope = taskService.getTask(id);
        if (envelope == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(envelope);
    }

    @GetMapping
    public List<TaskEnvelope> listTasks() {
        return taskService.listTasks();
    }
}
