package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.runtime.WorkerConfig;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessRuntimeTest {

    @Test
    void buildCommandIncludesJarPath() {
        var config = new WorkerConfig("auto-proc-1", Set.of(ExecutorType.SIMULATED, ExecutorType.SHELL),
            4, 8192, 10240, Set.of("gpu"));
        var command = ProcessRuntime.buildCommand(config, "worker/build/libs/worker.jar");

        assertThat(command).contains("java", "-jar", "worker/build/libs/worker.jar");
        assertThat(command).anyMatch(s -> s.contains("auto-proc-1"));
    }

    @Test
    void buildCommandIncludesMemoryFlag() {
        var config = new WorkerConfig("auto-proc-1", Set.of(ExecutorType.SIMULATED),
            4, 8192, 10240, Set.of());
        var command = ProcessRuntime.buildCommand(config, "worker.jar");

        assertThat(command).contains("-Xmx8192m");
    }

    @Test
    void buildEnvironmentContainsWorkerConfig() {
        var config = new WorkerConfig("auto-proc-1", Set.of(ExecutorType.SIMULATED),
            4, 8192, 10240, Set.of());
        var env = ProcessRuntime.buildEnvironment(config, "localhost:9092");

        assertThat(env).containsEntry("WORKER_ID", "auto-proc-1");
        assertThat(env).containsEntry("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        assertThat(env.get("SUPPORTED_EXECUTORS")).contains("SIMULATED");
        assertThat(env).containsEntry("CPU_CORES", "4");
        assertThat(env).containsEntry("MEMORY_MB", "8192");
    }

    @Test
    void buildEnvironmentIncludesTags() {
        var config = new WorkerConfig("auto-proc-1", Set.of(ExecutorType.SIMULATED),
            4, 8192, 10240, Set.of("gpu", "high-mem"));
        var env = ProcessRuntime.buildEnvironment(config, "localhost:9092");

        assertThat(env).containsKey("TAGS");
        assertThat(env.get("TAGS")).contains("gpu");
    }
}
