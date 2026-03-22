package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.executor.ExecutionResult;
import com.cloudbalancer.common.executor.PythonExecutor;
import com.cloudbalancer.common.executor.ResourceAllocation;
import com.cloudbalancer.common.executor.TaskContext;
import com.cloudbalancer.common.model.TaskIO.ArtifactSource;
import com.cloudbalancer.common.model.TaskIO.InputArtifact;
import com.cloudbalancer.common.model.TaskIO.OutputArtifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying the full pipeline: artifact staging, Python
 * execution that reads the staged artifact and produces an output, followed
 * by output artifact collection.
 *
 * <p>Runs without Spring context or external services -- only requires
 * python3 on the PATH.
 */
class PythonArtifactIntegrationTest {

    private final PythonExecutor executor = new PythonExecutor("python3");
    private final ArtifactService artifactService = new ArtifactService(104_857_600, "http://localhost:8080");

    @Test
    void inlineArtifactStagedAndReadByPythonScript(@TempDir Path workDir) throws Exception {
        // 1. Stage an inline artifact into workDir
        String content = "integration test data";
        String encoded = Base64.getEncoder().encodeToString(content.getBytes());
        var input = new InputArtifact("input.txt", encoded, ArtifactSource.INLINE);

        artifactService.stageInputs(List.of(input), workDir);

        // Verify file was staged correctly
        Path stagedFile = workDir.resolve("input.txt");
        assertThat(stagedFile).exists();
        assertThat(Files.readString(stagedFile)).isEqualTo(content);

        // 2. Execute Python script that reads the staged artifact using its absolute path,
        //    transforms the content, and writes output back to workDir.
        //    PythonExecutor runs in a temp subdirectory, so we pass absolute paths.
        String script = String.format("""
                with open('%s') as f:
                    data = f.read()
                output_path = '%s'
                with open(output_path, 'w') as f:
                    f.write(data.upper())
                print('done')
                """,
                stagedFile.toString().replace("\\", "\\\\"),
                workDir.resolve("output.txt").toString().replace("\\", "\\\\"));

        Map<String, Object> spec = new HashMap<>();
        spec.put("script", script);

        var ctx = new TaskContext(UUID.randomUUID(), workDir);
        ExecutionResult result = executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("done");

        // 3. Collect output artifact
        var output = new OutputArtifact("output.txt", "output.txt");
        var collected = artifactService.collectOutputs(List.of(output), workDir);

        assertThat(collected).hasSize(1);
        assertThat(collected.get(0).name()).isEqualTo("output.txt");
        assertThat(Files.readString(collected.get(0).path())).isEqualTo("INTEGRATION TEST DATA");
    }

    @Test
    void multipleArtifactsStagedAndCollected(@TempDir Path workDir) throws Exception {
        // Stage two inline artifacts
        String data1 = "first file";
        String data2 = "second file";
        var input1 = new InputArtifact("a.txt",
                Base64.getEncoder().encodeToString(data1.getBytes()), ArtifactSource.INLINE);
        var input2 = new InputArtifact("b.txt",
                Base64.getEncoder().encodeToString(data2.getBytes()), ArtifactSource.INLINE);

        artifactService.stageInputs(List.of(input1, input2), workDir);

        assertThat(Files.readString(workDir.resolve("a.txt"))).isEqualTo(data1);
        assertThat(Files.readString(workDir.resolve("b.txt"))).isEqualTo(data2);

        // Python script reads both and concatenates into a single output
        String script = String.format("""
                with open('%s') as f1, open('%s') as f2:
                    combined = f1.read() + ' | ' + f2.read()
                with open('%s', 'w') as out:
                    out.write(combined)
                print('merged')
                """,
                workDir.resolve("a.txt").toString().replace("\\", "\\\\"),
                workDir.resolve("b.txt").toString().replace("\\", "\\\\"),
                workDir.resolve("merged.txt").toString().replace("\\", "\\\\"));

        Map<String, Object> spec = new HashMap<>();
        spec.put("script", script);

        var ctx = new TaskContext(UUID.randomUUID(), workDir);
        ExecutionResult result = executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("merged");

        // Collect outputs -- one exists, one does not
        var collected = artifactService.collectOutputs(
                List.of(new OutputArtifact("merged.txt", "merged.txt"),
                        new OutputArtifact("missing.txt", "missing.txt")),
                workDir);

        assertThat(collected).hasSize(1);
        assertThat(Files.readString(collected.get(0).path())).isEqualTo("first file | second file");
    }

    @Test
    void scriptFailureDoesNotPreventOutputCollection(@TempDir Path workDir) throws Exception {
        // Stage an artifact
        String content = "partial data";
        var input = new InputArtifact("input.txt",
                Base64.getEncoder().encodeToString(content.getBytes()), ArtifactSource.INLINE);
        artifactService.stageInputs(List.of(input), workDir);

        // Script writes output but then exits with error
        String script = String.format("""
                with open('%s') as f:
                    data = f.read()
                with open('%s', 'w') as out:
                    out.write(data)
                raise RuntimeError('intentional failure')
                """,
                workDir.resolve("input.txt").toString().replace("\\", "\\\\"),
                workDir.resolve("partial_output.txt").toString().replace("\\", "\\\\"));

        Map<String, Object> spec = new HashMap<>();
        spec.put("script", script);

        var ctx = new TaskContext(UUID.randomUUID(), workDir);
        ExecutionResult result = executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);

        assertThat(result.exitCode()).isNotZero();

        // Output was written before the failure, so collection should find it
        var collected = artifactService.collectOutputs(
                List.of(new OutputArtifact("partial_output.txt", "partial_output.txt")),
                workDir);

        assertThat(collected).hasSize(1);
        assertThat(Files.readString(collected.get(0).path())).isEqualTo("partial data");
    }
}
