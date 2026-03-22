package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.api.dto.TaskLogsResponse;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class LogController {

    private final TaskRepository taskRepository;

    public LogController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<TaskLogsResponse> getTaskLogs(@PathVariable UUID id) {
        return taskRepository.findById(id)
            .map(r -> ResponseEntity.ok(new TaskLogsResponse(r.getLastStdout(), r.getLastStderr())))
            .orElse(ResponseEntity.notFound().build());
    }
}
