package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.service.ArtifactStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@RestController
public class ArtifactController {

    private final ArtifactStorageService artifactStorageService;

    public ArtifactController(ArtifactStorageService artifactStorageService) {
        this.artifactStorageService = artifactStorageService;
    }

    // Internal endpoint - unauthenticated (worker uploads)
    @PostMapping(value = "/internal/tasks/{taskId}/artifacts/{name}",
                 consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> uploadArtifact(
            @PathVariable UUID taskId,
            @PathVariable String name,
            HttpServletRequest request) throws IOException {
        artifactStorageService.store(taskId, name, request.getInputStream(),
                                     request.getContentLengthLong());
        return ResponseEntity.ok().build();
    }

    // Public endpoint - authenticated (client downloads)
    @GetMapping("/api/tasks/{taskId}/artifacts/{name}")
    public ResponseEntity<Resource> downloadArtifact(
            @PathVariable UUID taskId,
            @PathVariable String name) throws IOException {
        return artifactStorageService.retrieve(taskId, name)
            .map(path -> {
                try {
                    String contentType = Files.probeContentType(path);
                    if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                    return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body((Resource) new UrlResource(path.toUri()));
                } catch (IOException e) {
                    return ResponseEntity.internalServerError().<Resource>build();
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
