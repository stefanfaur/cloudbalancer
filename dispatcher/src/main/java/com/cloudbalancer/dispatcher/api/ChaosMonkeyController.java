package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.service.ChaosMonkeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/chaos")
@PreAuthorize("hasRole('ADMIN')")
public class ChaosMonkeyController {

    private final ChaosMonkeyService chaosMonkeyService;

    public ChaosMonkeyController(ChaosMonkeyService chaosMonkeyService) {
        this.chaosMonkeyService = chaosMonkeyService;
    }

    @PostMapping("/kill-worker")
    public ResponseEntity<?> killWorker(@RequestBody(required = false) Map<String, String> body) {
        Optional<String> workerId = Optional.ofNullable(body).map(b -> b.get("workerId"));
        var response = chaosMonkeyService.killWorker(workerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fail-task")
    public ResponseEntity<?> failTask(@RequestBody(required = false) Map<String, String> body) {
        Optional<UUID> taskId = Optional.ofNullable(body)
            .map(b -> b.get("taskId"))
            .map(UUID::fromString);
        var response = chaosMonkeyService.failTask(taskId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/latency")
    public ResponseEntity<?> injectLatency(@RequestBody Map<String, Object> body) {
        String component = (String) body.get("targetComponent");
        long delayMs = ((Number) body.get("delayMs")).longValue();
        int durationSeconds = ((Number) body.get("durationSeconds")).intValue();
        var response = chaosMonkeyService.injectLatency(component, delayMs, durationSeconds);
        return ResponseEntity.ok(response);
    }
}
