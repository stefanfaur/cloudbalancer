package com.cloudbalancer.dispatcher.scaling;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingWorkerTracker {

    private record PendingWorker(String workerId, String agentId, Instant requestedAt) {}

    private final ConcurrentHashMap<String, PendingWorker> pending = new ConcurrentHashMap<>();

    public void markPending(String workerId, String agentId, Instant requestedAt) {
        pending.put(workerId, new PendingWorker(workerId, agentId, requestedAt));
    }

    public void resolve(String workerId) {
        pending.remove(workerId);
    }

    public void fail(String workerId) {
        pending.remove(workerId);
    }

    public void expireStale(Duration timeout) {
        Instant cutoff = Instant.now().minus(timeout);
        pending.entrySet().removeIf(e -> e.getValue().requestedAt().isBefore(cutoff));
    }

    public int pendingCount() {
        return pending.size();
    }

    public String getAgentForWorker(String workerId) {
        var pw = pending.get(workerId);
        return pw != null ? pw.agentId() : null;
    }
}
