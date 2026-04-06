package com.cloudbalancer.dispatcher.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AgentTokenRepository extends JpaRepository<AgentToken, UUID> {
    Optional<AgentToken> findByTokenHash(String tokenHash);
}
