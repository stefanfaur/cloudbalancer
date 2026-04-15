package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.api.dto.AgentInfoResponse;
import com.cloudbalancer.dispatcher.api.dto.AgentWorkerResponse;
import com.cloudbalancer.dispatcher.scaling.AgentInfo;
import com.cloudbalancer.dispatcher.scaling.AgentRegistry;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/agents")
public class AgentController {

    private final AgentRegistry agentRegistry;
    private final WorkerRepository workerRepository;

    public AgentController(AgentRegistry agentRegistry, WorkerRepository workerRepository) {
        this.agentRegistry = agentRegistry;
        this.workerRepository = workerRepository;
    }

    @GetMapping
    public List<AgentInfoResponse> list() {
        return agentRegistry.getAliveAgents().stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<AgentInfoResponse> get(@PathVariable String agentId) {
        return agentRegistry.getAgent(agentId)
            .map(a -> ResponseEntity.ok(toResponse(a)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{agentId}/workers")
    public List<AgentWorkerResponse> getWorkers(@PathVariable String agentId) {
        return workerRepository.findByAgentId(agentId).stream()
            .map(w -> new AgentWorkerResponse(w.getId(), w.getHealthState().name(),
                w.getActiveTaskCount(), w.getRegisteredAt() != null ? w.getRegisteredAt().toString() : null))
            .toList();
    }

    private AgentInfoResponse toResponse(AgentInfo a) {
        return new AgentInfoResponse(
            a.agentId(), a.hostname(),
            a.totalCpuCores(), a.availableCpuCores(),
            a.totalMemoryMB(), a.availableMemoryMB(),
            a.activeWorkerIds() != null ? a.activeWorkerIds() : List.of(),
            a.supportedExecutors() != null
                ? a.supportedExecutors().stream().map(Enum::name).toList()
                : List.of(),
            a.lastHeartbeat() != null ? a.lastHeartbeat().toString() : null
        );
    }
}
