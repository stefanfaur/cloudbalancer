package com.cloudbalancer.worker.config;

import com.cloudbalancer.common.executor.DockerExecutor;
import com.cloudbalancer.common.executor.ShellExecutor;
import com.cloudbalancer.common.executor.SimulatedExecutor;
import com.cloudbalancer.common.executor.TaskExecutor;
import com.cloudbalancer.common.model.ExecutorType;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Configuration
public class ExecutorConfig {

    @Bean
    public Optional<DockerClient> dockerClient(WorkerProperties props) {
        if (!props.getSupportedExecutors().contains(ExecutorType.DOCKER)) {
            return Optional.empty();
        }
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(props.getDocker().getHost())
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();
        return Optional.of(DockerClientImpl.getInstance(config, httpClient));
    }

    @Bean
    public List<TaskExecutor> taskExecutors(WorkerProperties props, Optional<DockerClient> dockerClient) {
        List<TaskExecutor> executors = new ArrayList<>();
        for (ExecutorType type : props.getSupportedExecutors()) {
            switch (type) {
                case SIMULATED -> executors.add(new SimulatedExecutor());
                case SHELL -> executors.add(new ShellExecutor(
                        props.getShell().getBlockedCommands(),
                        props.getShell().getMaxOutputBytes()));
                case DOCKER -> dockerClient.ifPresent(dc ->
                        executors.add(new DockerExecutor(dc)));
                default -> { /* unsupported executor types are ignored */ }
            }
        }
        return executors;
    }
}
