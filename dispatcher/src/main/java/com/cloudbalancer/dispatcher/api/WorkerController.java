package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.api.dto.WorkerSummary;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {

    private final WorkerRegistryService workerRegistryService;

    public WorkerController(WorkerRegistryService workerRegistryService) {
        this.workerRegistryService = workerRegistryService;
    }

    @GetMapping
    public List<WorkerSummary> listWorkers() {
        return workerRegistryService.getAllWorkers().stream()
            .map(w -> new WorkerSummary(
                w.getId(),
                w.getHealthState().name(),
                w.getAgentId(),
                w.getActiveTaskCount(),
                w.getRegisteredAt() != null ? w.getRegisteredAt().toString() : null))
            .toList();
    }
}
