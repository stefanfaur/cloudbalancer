package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.api.dto.AgentRegistrationRequest;
import com.cloudbalancer.dispatcher.api.dto.AgentRegistrationResponse;
import com.cloudbalancer.dispatcher.registration.AgentTokenService;
import com.cloudbalancer.dispatcher.registration.RegistrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agents")
public class AgentRegistrationController {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistrationController.class);

    private final AgentTokenService tokenService;
    private final RegistrationProperties registrationProps;

    public AgentRegistrationController(AgentTokenService tokenService,
                                        RegistrationProperties registrationProps) {
        this.tokenService = tokenService;
        this.registrationProps = registrationProps;
    }

    @PostMapping("/register")
    public ResponseEntity<AgentRegistrationResponse> register(@RequestBody AgentRegistrationRequest request) {
        if (!tokenService.validate(request.token())) {
            log.warn("Registration rejected for agentId={}: invalid or revoked token", request.agentId());
            return ResponseEntity.status(401).build();
        }

        log.info("Agent registered: agentId={}, cpuCores={}, memoryMb={}",
                request.agentId(), request.cpuCores(), request.memoryMb());

        return ResponseEntity.ok(new AgentRegistrationResponse(
            registrationProps.getKafkaBootstrapExternal(),
            registrationProps.getKafkaUsername(),
            registrationProps.getKafkaPassword()
        ));
    }
}
