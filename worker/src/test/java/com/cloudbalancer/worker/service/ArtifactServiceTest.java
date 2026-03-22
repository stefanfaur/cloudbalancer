package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.model.TaskIO.InputArtifact;
import com.cloudbalancer.common.model.TaskIO.OutputArtifact;
import com.cloudbalancer.common.model.TaskIO.ArtifactSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtifactServiceTest {

    @Test
    void stageInlineArtifactWritesDecodedContent(@TempDir Path workDir) throws Exception {
        String content = "hello artifact";
        String encoded = Base64.getEncoder().encodeToString(content.getBytes());
        var input = new InputArtifact("data.txt", encoded, ArtifactSource.INLINE);
        var service = new ArtifactService(104_857_600, "http://localhost:8080");

        service.stageInputs(List.of(input), workDir);

        assertThat(workDir.resolve("data.txt")).exists();
        assertThat(Files.readString(workDir.resolve("data.txt"))).isEqualTo(content);
    }

    @Test
    void stageObjectStorageThrowsUnsupported(@TempDir Path workDir) {
        var input = new InputArtifact("data.txt", "s3://bucket/key", ArtifactSource.OBJECT_STORAGE);
        var service = new ArtifactService(104_857_600, "http://localhost:8080");

        assertThatThrownBy(() -> service.stageInputs(List.of(input), workDir))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void collectOutputsReturnsExistingFiles(@TempDir Path workDir) throws Exception {
        Files.writeString(workDir.resolve("result.csv"), "a,b,c");
        var output = new OutputArtifact("result.csv", "result.csv");
        var service = new ArtifactService(104_857_600, "http://localhost:8080");

        var collected = service.collectOutputs(List.of(output), workDir);

        assertThat(collected).hasSize(1);
        assertThat(collected.get(0).name()).isEqualTo("result.csv");
    }

    @Test
    void collectOutputsSkipsMissingFiles(@TempDir Path workDir) {
        var output = new OutputArtifact("missing.txt", "missing.txt");
        var service = new ArtifactService(104_857_600, "http://localhost:8080");

        var collected = service.collectOutputs(List.of(output), workDir);

        assertThat(collected).isEmpty();
    }

    @Test
    void stageEmptyListDoesNothing(@TempDir Path workDir) throws Exception {
        var service = new ArtifactService(104_857_600, "http://localhost:8080");
        service.stageInputs(List.of(), workDir); // no exception
    }
}
