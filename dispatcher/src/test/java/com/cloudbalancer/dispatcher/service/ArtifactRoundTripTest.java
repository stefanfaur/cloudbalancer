package com.cloudbalancer.dispatcher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying the full store-then-retrieve round-trip of
 * {@link ArtifactStorageService}, including edge cases around content
 * encoding, multiple artifacts per task, and cross-task isolation.
 *
 * <p>Runs without Spring context -- direct construction with @TempDir.
 */
class ArtifactRoundTripTest {

    @Test
    void storeAndRetrieveRoundTrip(@TempDir Path baseDir) throws Exception {
        var service = new ArtifactStorageService(baseDir.toString(), 104_857_600);
        UUID taskId = UUID.randomUUID();
        byte[] content = "artifact content with special chars: \u00e9\u00e0\u00fc".getBytes(StandardCharsets.UTF_8);

        service.store(taskId, "data.bin", new ByteArrayInputStream(content), content.length);

        var retrieved = service.retrieve(taskId, "data.bin");
        assertThat(retrieved).isPresent();
        assertThat(Files.readAllBytes(retrieved.get())).isEqualTo(content);
    }

    @Test
    void multipleArtifactsPerTaskIsolated(@TempDir Path baseDir) throws Exception {
        var service = new ArtifactStorageService(baseDir.toString(), 104_857_600);
        UUID taskId = UUID.randomUUID();

        byte[] content1 = "first artifact".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "second artifact".getBytes(StandardCharsets.UTF_8);

        service.store(taskId, "file1.txt", new ByteArrayInputStream(content1), content1.length);
        service.store(taskId, "file2.txt", new ByteArrayInputStream(content2), content2.length);

        var r1 = service.retrieve(taskId, "file1.txt");
        var r2 = service.retrieve(taskId, "file2.txt");

        assertThat(r1).isPresent();
        assertThat(r2).isPresent();
        assertThat(Files.readAllBytes(r1.get())).isEqualTo(content1);
        assertThat(Files.readAllBytes(r2.get())).isEqualTo(content2);
    }

    @Test
    void artifactsIsolatedBetweenTasks(@TempDir Path baseDir) throws Exception {
        var service = new ArtifactStorageService(baseDir.toString(), 104_857_600);
        UUID task1 = UUID.randomUUID();
        UUID task2 = UUID.randomUUID();

        byte[] content1 = "task1 data".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "task2 data".getBytes(StandardCharsets.UTF_8);

        service.store(task1, "shared-name.txt", new ByteArrayInputStream(content1), content1.length);
        service.store(task2, "shared-name.txt", new ByteArrayInputStream(content2), content2.length);

        // Same artifact name, different tasks -- must return different content
        assertThat(Files.readAllBytes(service.retrieve(task1, "shared-name.txt").orElseThrow()))
                .isEqualTo(content1);
        assertThat(Files.readAllBytes(service.retrieve(task2, "shared-name.txt").orElseThrow()))
                .isEqualTo(content2);
    }

    @Test
    void storeOverwritesExistingArtifact(@TempDir Path baseDir) throws Exception {
        var service = new ArtifactStorageService(baseDir.toString(), 104_857_600);
        UUID taskId = UUID.randomUUID();

        byte[] original = "original".getBytes(StandardCharsets.UTF_8);
        byte[] updated = "updated content".getBytes(StandardCharsets.UTF_8);

        service.store(taskId, "data.txt", new ByteArrayInputStream(original), original.length);
        service.store(taskId, "data.txt", new ByteArrayInputStream(updated), updated.length);

        var retrieved = service.retrieve(taskId, "data.txt");
        assertThat(retrieved).isPresent();
        assertThat(Files.readAllBytes(retrieved.get())).isEqualTo(updated);
    }

    @Test
    void retrieveNonExistentTaskReturnsEmpty(@TempDir Path baseDir) {
        var service = new ArtifactStorageService(baseDir.toString(), 104_857_600);
        assertThat(service.retrieve(UUID.randomUUID(), "nothing.bin")).isEmpty();
    }

    @Test
    void binaryContentPreservedExactly(@TempDir Path baseDir) throws Exception {
        var service = new ArtifactStorageService(baseDir.toString(), 104_857_600);
        UUID taskId = UUID.randomUUID();

        // Binary content with all byte values 0x00-0xFF
        byte[] binary = new byte[256];
        for (int i = 0; i < 256; i++) {
            binary[i] = (byte) i;
        }

        service.store(taskId, "binary.dat", new ByteArrayInputStream(binary), binary.length);

        var retrieved = service.retrieve(taskId, "binary.dat");
        assertThat(retrieved).isPresent();
        assertThat(Files.readAllBytes(retrieved.get())).isEqualTo(binary);
    }
}
