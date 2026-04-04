package com.cloudbalancer.worker.config;

import com.cloudbalancer.common.executor.DockerExecutor;
import com.cloudbalancer.common.executor.PythonExecutor;
import com.cloudbalancer.common.executor.ShellExecutor;
import com.cloudbalancer.common.executor.SimulatedExecutor;
import com.cloudbalancer.common.executor.TaskExecutor;
import com.cloudbalancer.common.model.ExecutorType;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ExecutorConfig {

    @Bean
    public List<TaskExecutor> taskExecutors(WorkerProperties props) {
        List<TaskExecutor> executors = new ArrayList<>();
        for (ExecutorType type : props.getSupportedExecutors()) {
            switch (type) {
                case SIMULATED -> executors.add(new SimulatedExecutor());
                case SHELL -> executors.add(new ShellExecutor(
                        props.getShell().getBlockedCommands(),
                        props.getShell().getMaxOutputBytes()));
                case DOCKER -> executors.add(new DockerExecutor(createDockerClient(props)));
                case PYTHON -> executors.add(new PythonExecutor(
                        props.getPython().getPythonBinary()));
                default -> { /* unsupported executor types are ignored */ }
            }
        }
        return executors;
    }

    private DockerClient createDockerClient(WorkerProperties props) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(props.getDocker().getHost())
                .build();
        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }
}
