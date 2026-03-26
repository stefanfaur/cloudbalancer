package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.ScalingPolicy;
import com.cloudbalancer.dispatcher.persistence.ScalingPolicyRecord;
import com.cloudbalancer.dispatcher.persistence.ScalingPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ScalingPolicyService {

    private static final Logger log = LoggerFactory.getLogger(ScalingPolicyService.class);
    private final ScalingPolicyRepository repository;
    private volatile ScalingPolicy currentPolicy;

    public ScalingPolicyService(ScalingPolicyRepository repository) {
        this.repository = repository;
        this.currentPolicy = loadOrDefault();
    }

    public ScalingPolicy getCurrentPolicy() {
        return currentPolicy;
    }

    public void reloadPolicy() {
        this.currentPolicy = loadOrDefault();
    }

    public ScalingPolicy updatePolicy(ScalingPolicy policy) {
        var record = repository.findAll().stream().findFirst().orElse(null);
        if (record == null) {
            record = new ScalingPolicyRecord(
                policy.minWorkers(), policy.maxWorkers(),
                (int) policy.cooldownPeriod().getSeconds(),
                policy.scaleUpStep(), policy.scaleDownStep(),
                (int) policy.scaleDownDrainTime().getSeconds());
        } else {
            record.setMinWorkers(policy.minWorkers());
            record.setMaxWorkers(policy.maxWorkers());
            record.setCooldownSeconds((int) policy.cooldownPeriod().getSeconds());
            record.setScaleUpStep(policy.scaleUpStep());
            record.setScaleDownStep(policy.scaleDownStep());
            record.setDrainTimeSeconds((int) policy.scaleDownDrainTime().getSeconds());
        }
        repository.save(record);
        this.currentPolicy = policy;
        log.info("Scaling policy updated: {}", policy);
        return policy;
    }

    private ScalingPolicy loadOrDefault() {
        try {
            return repository.findAll().stream().findFirst()
                .map(r -> new ScalingPolicy(
                    r.getMinWorkers(), r.getMaxWorkers(),
                    Duration.ofSeconds(r.getCooldownSeconds()),
                    r.getScaleUpStep(), r.getScaleDownStep(),
                    Duration.ofSeconds(r.getDrainTimeSeconds())))
                .orElseGet(() -> {
                    log.info("No scaling policy found, using defaults");
                    return ScalingPolicy.defaults();
                });
        } catch (Exception e) {
            log.warn("Failed to load scaling policy: {}", e.getMessage());
            return ScalingPolicy.defaults();
        }
    }
}
