package com.cloudbalancer.dispatcher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtifactStorageServiceTest {

    @Test
    void storeAndRetrieveArtifact(@TempDir Path baseDir) throws Exception {
        var service = new ArtifactStorageService(baseDir.toString(), 104_857_600);
        UUID taskId = UUID.randomUUID();
        byte[] content = "hello artifact".getBytes();
        service.store(taskId, "result.txt", new ByteArrayInputStream(content), content.length);
        var retrieved = service.retrieve(taskId, "result.txt");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().toFile()).hasContent("hello artifact");
    }

    @Test
    void retrieveNonExistentReturnsEmpty(@TempDir Path baseDir) {
        var service = new ArtifactStorageService(baseDir.toString(), 104_857_600);
        assertThat(service.retrieve(UUID.randomUUID(), "missing.txt")).isEmpty();
    }

    @Test
    void storeRejectsOversizedArtifact(@TempDir Path baseDir) {
        var service = new ArtifactStorageService(baseDir.toString(), 10);
        byte[] content = "this exceeds 10 bytes easily".getBytes();
        assertThatThrownBy(() ->
            service.store(UUID.randomUUID(), "big.bin",
                new ByteArrayInputStream(content), content.length))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
