package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.api.dto.*;
import com.cloudbalancer.dispatcher.registration.AgentTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/agent-tokens")
public class AgentTokenController {

    private final AgentTokenService tokenService;

    public AgentTokenController(AgentTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<CreateAgentTokenResponse> create(@RequestBody CreateAgentTokenRequest request,
                                                            Principal principal) {
        var result = tokenService.create(request.label(), principal.getName());
        return ResponseEntity.ok(new CreateAgentTokenResponse(result.id(), result.token(), result.label()));
    }

    @GetMapping
    public List<AgentTokenSummary> list() {
        return tokenService.listAll().stream()
            .map(t -> new AgentTokenSummary(t.getId(), t.getLabel(), t.getCreatedBy(),
                    t.getCreatedAt(), t.getLastUsedAt(), t.isRevoked()))
            .toList();
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        tokenService.revoke(id);
        return ResponseEntity.noContent().build();
    }
}
