package com.cloudbalancer.dispatcher.registration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AgentTokenService {

    private static final String TOKEN_PREFIX = "cb_at_";
    private static final int TOKEN_BYTES = 24;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AgentTokenRepository repository;

    public AgentTokenService(AgentTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CreateTokenResult create(String label, String createdBy) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(randomBytes);
        String plaintext = TOKEN_PREFIX + HexFormat.of().formatHex(randomBytes);
        String hash = sha256(plaintext);

        AgentToken entity = new AgentToken();
        entity.setTokenHash(hash);
        entity.setLabel(label);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(Instant.now());
        repository.save(entity);

        return new CreateTokenResult(entity.getId(), plaintext, label);
    }

    @Transactional
    public boolean validate(String plaintext) {
        String hash = sha256(plaintext);
        Optional<AgentToken> opt = repository.findByTokenHash(hash);
        if (opt.isEmpty()) return false;
        AgentToken token = opt.get();
        if (token.isRevoked()) return false;
        token.setLastUsedAt(Instant.now());
        repository.save(token);
        return true;
    }

    @Transactional
    public void revoke(UUID id) {
        repository.findById(id).ifPresent(token -> {
            token.setRevoked(true);
            repository.save(token);
        });
    }

    public List<AgentToken> listAll() {
        return repository.findAll();
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record CreateTokenResult(UUID id, String token, String label) {}
}
