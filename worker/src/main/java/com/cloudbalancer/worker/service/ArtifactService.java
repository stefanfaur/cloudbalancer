package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.model.TaskIO.InputArtifact;
import com.cloudbalancer.common.model.TaskIO.OutputArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class ArtifactService {

    private static final Logger log = LoggerFactory.getLogger(ArtifactService.class);

    private final long maxSizeBytes;
    private final String dispatcherUrl;
    private final HttpClient httpClient;

    public record CollectedArtifact(String name, Path path) {}

    public ArtifactService(
            @Value("${cloudbalancer.worker.artifacts.max-size-bytes:104857600}") long maxSizeBytes,
            @Value("${cloudbalancer.worker.artifacts.dispatcher-url:http://localhost:8080}") String dispatcherUrl) {
        this.maxSizeBytes = maxSizeBytes;
        this.dispatcherUrl = dispatcherUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Stages input artifacts into the working directory.
     * Supports INLINE (Base64-decoded), HTTP (downloaded), and throws for OBJECT_STORAGE.
     */
    public void stageInputs(List<InputArtifact> inputs, Path workDir) throws IOException, InterruptedException {
        for (InputArtifact input : inputs) {
            switch (input.source()) {
                case INLINE -> stageInline(input, workDir);
                case HTTP -> stageHttp(input, workDir);
                case OBJECT_STORAGE -> throw new UnsupportedOperationException("OBJECT_STORAGE not yet supported");
            }
        }
    }

    /**
     * Collects output artifacts from the working directory.
     * Returns only files that actually exist; missing files are skipped with a warning.
     */
    public List<CollectedArtifact> collectOutputs(List<OutputArtifact> outputs, Path workDir) {
        List<CollectedArtifact> collected = new ArrayList<>();
        for (OutputArtifact output : outputs) {
            Path filePath = safeResolve(workDir, output.path());
            if (Files.exists(filePath)) {
                collected.add(new CollectedArtifact(output.name(), filePath));
                log.debug("Collected output artifact: {}", output.name());
            } else {
                log.warn("Output artifact not found, skipping: {} (path: {})", output.name(), filePath);
            }
        }
        return collected;
    }

    /**
     * Uploads collected artifacts to the dispatcher via HTTP POST.
     */
    public void uploadArtifacts(UUID taskId, List<CollectedArtifact> artifacts) throws IOException, InterruptedException {
        for (CollectedArtifact artifact : artifacts) {
            byte[] body = Files.readAllBytes(artifact.path());
            if (body.length > maxSizeBytes) {
                log.warn("Artifact {} exceeds max size ({} > {}), skipping upload",
                        artifact.name(), body.length, maxSizeBytes);
                continue;
            }

            String url = String.format("%s/internal/tasks/%s/artifacts/%s",
                    dispatcherUrl, taskId, artifact.name());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Uploaded artifact {} for task {} ({} bytes)", artifact.name(), taskId, body.length);
            } else {
                log.error("Failed to upload artifact {} for task {}: HTTP {}", artifact.name(), taskId, response.statusCode());
            }
        }
    }

    private void stageInline(InputArtifact input, Path workDir) throws IOException {
        byte[] decoded = Base64.getDecoder().decode(input.location());
        if (decoded.length > maxSizeBytes) {
            throw new IOException("Inline artifact " + input.name() + " exceeds max size: " + decoded.length);
        }
        Path target = safeResolve(workDir, input.name());
        Files.write(target, decoded);
        log.debug("Staged inline artifact: {} ({} bytes)", input.name(), decoded.length);
    }

    private void stageHttp(InputArtifact input, Path workDir) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(input.location()))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Failed to download artifact " + input.name()
                    + " from " + input.location() + ": HTTP " + response.statusCode());
        }

        byte[] body = response.body();
        if (body.length > maxSizeBytes) {
            throw new IOException("HTTP artifact " + input.name() + " exceeds max size: " + body.length);
        }

        Path target = safeResolve(workDir, input.name());
        Files.write(target, body);
        log.debug("Staged HTTP artifact: {} ({} bytes)", input.name(), body.length);
    }

    private Path safeResolve(Path dir, String name) {
        Path resolved = dir.resolve(name).normalize();
        if (!resolved.startsWith(dir.normalize())) {
            throw new IllegalArgumentException("Invalid artifact name: " + name);
        }
        return resolved;
    }
}
