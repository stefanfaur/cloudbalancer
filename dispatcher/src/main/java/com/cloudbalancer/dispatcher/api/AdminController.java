package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.api.dto.StrategyRequest;
import com.cloudbalancer.dispatcher.api.dto.StrategyResponse;
import com.cloudbalancer.dispatcher.service.SchedulingConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SchedulingConfigService configService;

    public AdminController(SchedulingConfigService configService) {
        this.configService = configService;
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
}
