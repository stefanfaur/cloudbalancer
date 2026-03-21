package com.cloudbalancer.worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class WorkerChaosService {

    private static final Logger log = LoggerFactory.getLogger(WorkerChaosService.class);

    private final AtomicReference<LatencyInjection> latencyInjection = new AtomicReference<>(null);

    public record LatencyInjection(String targetComponent, long delayMs, Instant expiresAt) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public void injectLatency(String component, long delayMs, int durationSeconds) {
        Instant expiresAt = Instant.now().plusSeconds(durationSeconds);
        latencyInjection.set(new LatencyInjection(component, delayMs, expiresAt));
        log.warn("[WorkerChaos] Injecting {}ms latency into '{}' for {}s (expires at {})",
            delayMs, component, durationSeconds, expiresAt);
    }

    public void checkAndApplyLatency(String component) throws InterruptedException {
        var injection = latencyInjection.get();
        if (injection != null && !injection.isExpired() && component.equals(injection.targetComponent())) {
            log.debug("[WorkerChaos] Applying {}ms latency to '{}'", injection.delayMs(), component);
            Thread.sleep(injection.delayMs());
        } else if (injection != null && injection.isExpired()) {
            latencyInjection.compareAndSet(injection, null);
        }
    }
}
