package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.ScalingAction;
import com.cloudbalancer.common.model.ScalingPolicy;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.dispatcher.api.dto.*;
import com.cloudbalancer.dispatcher.service.AutoScalerService;
import com.cloudbalancer.dispatcher.service.ScalingPolicyService;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/scaling")
public class ScalingController {

    private final ScalingPolicyService policyService;
    private final WorkerRegistryService workerRegistry;
    private final AutoScalerService autoScaler;

    public ScalingController(ScalingPolicyService policyService,
                             WorkerRegistryService workerRegistry,
                             AutoScalerService autoScaler) {
        this.policyService = policyService;
        this.workerRegistry = workerRegistry;
        this.autoScaler = autoScaler;
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ScalingStatusResponse getStatus() {
        var all = workerRegistry.getAllWorkers();
        int total = all.size();
        int active = (int) all.stream()
            .filter(w -> w.getHealthState() == WorkerHealthState.HEALTHY
                      || w.getHealthState() == WorkerHealthState.RECOVERING)
            .count();
        int draining = (int) all.stream()
            .filter(w -> w.getHealthState() == WorkerHealthState.DRAINING)
            .count();

        return new ScalingStatusResponse(
            total, active, draining,
            policyService.getCurrentPolicy(),
            autoScaler.getLastDecision(),
            autoScaler.getCooldownRemainingSeconds(),
            autoScaler.getRuntimeMode()
        );
    }

    @PutMapping("/policy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScalingPolicyResponse> updatePolicy(@RequestBody ScalingPolicyRequest request) {
        try {
            var policy = ScalingPolicy.validated(
                request.minWorkers(), request.maxWorkers(),
                Duration.ofSeconds(request.cooldownSeconds()),
                request.scaleUpStep(), request.scaleDownStep(),
                Duration.ofSeconds(request.drainTimeSeconds()));
            policyService.updatePolicy(policy);
            return ResponseEntity.ok(new ScalingPolicyResponse(policy, Instant.now()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScalingStatusResponse> triggerScaling(@RequestBody ScalingTriggerRequest request) {
        ScalingAction action;
        try {
            action = ScalingAction.valueOf(request.action());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        autoScaler.triggerManual(action, request.count());

        return ResponseEntity.ok(getStatus());
    }
}
