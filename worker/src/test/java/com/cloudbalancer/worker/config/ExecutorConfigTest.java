package com.cloudbalancer.worker.config;

import com.cloudbalancer.common.executor.DockerExecutor;
import com.cloudbalancer.common.executor.PythonExecutor;
import com.cloudbalancer.common.executor.ShellExecutor;
import com.cloudbalancer.common.executor.SimulatedExecutor;
import com.cloudbalancer.common.executor.TaskExecutor;
import com.cloudbalancer.common.model.ExecutorType;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExecutorConfigTest {

    private ExecutorConfig executorConfig;

    @BeforeEach
    void setUp() {
        executorConfig = new ExecutorConfig();
    }

    @Test
    void whenSimulatedConfigured_thenSimulatedExecutorCreated() {
        WorkerProperties props = buildProperties(Set.of(ExecutorType.SIMULATED));

        List<TaskExecutor> executors = executorConfig.taskExecutors(props, Optional.empty());

        assertThat(executors).hasSize(1);
        assertThat(executors.get(0)).isInstanceOf(SimulatedExecutor.class);
    }

    @Test
    void whenShellConfigured_thenShellExecutorCreatedWithConfigFromProperties() {
        WorkerProperties props = buildProperties(Set.of(ExecutorType.SHELL));
        props.getShell().setBlockedCommands(Set.of("rm -rf /", "shutdown"));
        props.getShell().setMaxOutputBytes(512_000);

        List<TaskExecutor> executors = executorConfig.taskExecutors(props, Optional.empty());

        assertThat(executors).hasSize(1);
        assertThat(executors.get(0)).isInstanceOf(ShellExecutor.class);
        assertThat(executors.get(0).getExecutorType()).isEqualTo(ExecutorType.SHELL);
    }

    @Test
    void whenOnlySimulatedConfigured_thenNoShellOrDockerExecutors() {
        WorkerProperties props = buildProperties(Set.of(ExecutorType.SIMULATED));

        List<TaskExecutor> executors = executorConfig.taskExecutors(props, Optional.empty());

        assertThat(executors).hasSize(1);
        assertThat(executors).noneMatch(e -> e instanceof ShellExecutor);
        assertThat(executors).noneMatch(e -> e instanceof DockerExecutor);
    }

    @Test
    void whenMultipleExecutorsConfigured_thenAllCreated() {
        WorkerProperties props = buildProperties(Set.of(ExecutorType.SIMULATED, ExecutorType.SHELL));

        List<TaskExecutor> executors = executorConfig.taskExecutors(props, Optional.empty());

        assertThat(executors).hasSize(2);
        assertThat(executors).anyMatch(e -> e instanceof SimulatedExecutor);
        assertThat(executors).anyMatch(e -> e instanceof ShellExecutor);
    }

    @Test
    void whenDockerConfiguredWithClient_thenDockerExecutorCreated() {
        WorkerProperties props = buildProperties(Set.of(ExecutorType.DOCKER));
        DockerClient mockClient = mock(DockerClient.class);

        List<TaskExecutor> executors = executorConfig.taskExecutors(props, Optional.of(mockClient));

        assertThat(executors).hasSize(1);
        assertThat(executors.get(0)).isInstanceOf(DockerExecutor.class);
    }

    @Test
    void whenDockerConfiguredButNoClient_thenDockerExecutorNotCreated() {
        WorkerProperties props = buildProperties(Set.of(ExecutorType.DOCKER));

        List<TaskExecutor> executors = executorConfig.taskExecutors(props, Optional.empty());

        assertThat(executors).isEmpty();
    }

    @Test
    void whenDockerNotInSupportedExecutors_thenDockerClientIsEmpty() {
        WorkerProperties props = buildProperties(Set.of(ExecutorType.SIMULATED));

        Optional<DockerClient> client = executorConfig.dockerClient(props);

        assertThat(client).isEmpty();
    }

    @Test
    void executorListMatchesConfiguredTypes() {
        WorkerProperties props = buildProperties(Set.of(ExecutorType.SIMULATED, ExecutorType.SHELL, ExecutorType.DOCKER));
        DockerClient mockClient = mock(DockerClient.class);

        List<TaskExecutor> executors = executorConfig.taskExecutors(props, Optional.of(mockClient));

        assertThat(executors).hasSize(3);
        Set<ExecutorType> types = Set.of(
                executors.get(0).getExecutorType(),
                executors.get(1).getExecutorType(),
                executors.get(2).getExecutorType()
        );
        assertThat(types).containsExactlyInAnyOrder(
                ExecutorType.SIMULATED, ExecutorType.SHELL, ExecutorType.DOCKER
        );
    }

    @Test
    void whenPythonConfigured_thenPythonExecutorCreated() {
        WorkerProperties props = buildProperties(Set.of(ExecutorType.PYTHON));

        List<TaskExecutor> executors = executorConfig.taskExecutors(props, Optional.empty());

        assertThat(executors).hasSize(1);
        assertThat(executors.get(0)).isInstanceOf(PythonExecutor.class);
        assertThat(executors.get(0).getExecutorType()).isEqualTo(ExecutorType.PYTHON);
    }

    private WorkerProperties buildProperties(Set<ExecutorType> executorTypes) {
        WorkerProperties props = new WorkerProperties();
        props.setSupportedExecutors(executorTypes);
        return props;
    }
}
