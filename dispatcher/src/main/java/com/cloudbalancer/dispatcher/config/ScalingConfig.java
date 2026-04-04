package com.cloudbalancer.dispatcher.config;

import com.cloudbalancer.common.runtime.NodeRuntime;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.scaling.DockerRuntime;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class ScalingConfig {

    @Bean
    @ConditionalOnMissingBean
    public DockerClient dockerClient() {
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("unix:///var/run/docker.sock")
            .build();
        var httpClient = new ZerodepDockerHttpClient.Builder()
            .dockerHost(URI.create("unix:///var/run/docker.sock"))
            .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    @Bean
    public NodeRuntime nodeRuntime(DockerClient dockerClient,
                                   ScalingProperties props,
                                   WorkerRegistryService workerRegistry,
                                   EventPublisher eventPublisher) {
        return new DockerRuntime(dockerClient, workerRegistry, eventPublisher, props);
    }
}
