package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.api.dto.*;
import com.cloudbalancer.dispatcher.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
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
    public TaskPageResponse listTasks(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) TaskState status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) ExecutorType executorType,
            @RequestParam(required = false) String workerId,
            @RequestParam(required = false) Instant since) {
        return taskService.listTasks(offset, limit, status, priority, executorType, workerId, since);
    }

    @PostMapping("/bulk/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public List<BulkResultEntry> bulkCancel(@RequestBody BulkTaskRequest request) {
        return taskService.bulkCancel(request.taskIds());
    }

    @PostMapping("/bulk/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public List<BulkResultEntry> bulkRetry(@RequestBody BulkTaskRequest request) {
        return taskService.bulkRetry(request.taskIds());
    }

    @PostMapping("/bulk/reprioritize")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public List<BulkResultEntry> bulkReprioritize(@RequestBody BulkReprioritizeRequest request) {
        return taskService.bulkReprioritize(request.taskIds(), request.priority());
    }
}
