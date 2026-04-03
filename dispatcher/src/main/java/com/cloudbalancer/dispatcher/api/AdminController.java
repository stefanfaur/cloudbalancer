package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.api.dto.StrategyRequest;
import com.cloudbalancer.dispatcher.api.dto.StrategyResponse;
import com.cloudbalancer.dispatcher.api.dto.WorkerTagsRequest;
import com.cloudbalancer.dispatcher.service.SchedulingConfigService;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SchedulingConfigService configService;
    private final WorkerRegistryService workerRegistryService;

    public AdminController(SchedulingConfigService configService,
                           WorkerRegistryService workerRegistryService) {
        this.configService = configService;
        this.workerRegistryService = workerRegistryService;
    }

    @GetMapping("/strategy")
    @PreAuthorize("hasRole('ADMIN')")
    public StrategyResponse getStrategy() {
        var current = configService.getCurrentStrategy();
        return new StrategyResponse(current.getName(), current.getWeights());
    }

    @PutMapping("/strategy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StrategyResponse> switchStrategy(@RequestBody StrategyRequest request) {
        try {
            var weights = request.weights() != null ? request.weights() : Map.<String, Integer>of();
            configService.switchStrategy(request.strategy(), weights);
            var updated = configService.getCurrentStrategy();
            return ResponseEntity.ok(new StrategyResponse(updated.getName(), updated.getWeights()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/workers/{id}/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<String>> updateWorkerTags(@PathVariable String id,
                                                         @RequestBody WorkerTagsRequest request) {
        try {
            Set<String> updated = workerRegistryService.updateWorkerTags(id, request.tags());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
