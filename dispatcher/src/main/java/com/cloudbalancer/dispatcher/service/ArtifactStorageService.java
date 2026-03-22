package com.cloudbalancer.dispatcher.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class ArtifactStorageService {

    private static final Logger log = LoggerFactory.getLogger(ArtifactStorageService.class);

    private final String basePath;
    private final long maxSizeBytes;

    public ArtifactStorageService(
            @Value("${cloudbalancer.dispatcher.artifact-base-path:/tmp/cloudbalancer/artifacts}") String basePath,
            @Value("${cloudbalancer.dispatcher.artifact-max-size-bytes:104857600}") long maxSizeBytes) {
        this.basePath = basePath;
        this.maxSizeBytes = maxSizeBytes;
    }

    public void store(UUID taskId, String name, InputStream data, long contentLength) throws IOException {
        if (contentLength > maxSizeBytes) {
            throw new IllegalArgumentException(
                    "Artifact size " + contentLength + " exceeds max " + maxSizeBytes);
        }
        Path dir = Path.of(basePath, taskId.toString());
        Files.createDirectories(dir);
        Path target = safeResolve(dir, name);
        log.info("Storing artifact '{}' for task {} ({} bytes)", name, taskId, contentLength);
        try (data) {
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Optional<Path> retrieve(UUID taskId, String name) {
        Path dir = Path.of(basePath, taskId.toString());
        Path file = safeResolve(dir, name);
        return Files.exists(file) ? Optional.of(file) : Optional.empty();
    }

    private Path safeResolve(Path dir, String name) {
        Path resolved = dir.resolve(name).normalize();
        if (!resolved.startsWith(dir)) {
            throw new IllegalArgumentException("Invalid artifact name: " + name);
        }
        return resolved;
    }
}
