package com.cloudbalancer.dispatcher.config;

import com.cloudbalancer.common.runtime.NodeRuntime;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.scaling.AgentRegistry;
import com.cloudbalancer.dispatcher.scaling.AgentRuntime;
import com.cloudbalancer.dispatcher.scaling.DockerRuntime;
import com.cloudbalancer.dispatcher.scaling.PendingWorkerTracker;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.net.URI;

@Configuration
public class ScalingConfig {

    private static final Logger log = LoggerFactory.getLogger(ScalingConfig.class);

    @Bean
    @ConditionalOnProperty(name = "cloudbalancer.dispatcher.scaling.runtime-mode", havingValue = "DOCKER", matchIfMissing = true)
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
    @ConditionalOnProperty(name = "cloudbalancer.dispatcher.scaling.runtime-mode", havingValue = "DOCKER", matchIfMissing = true)
    public NodeRuntime dockerNodeRuntime(DockerClient dockerClient,
                                          ScalingProperties props,
                                          WorkerRegistryService workerRegistry,
                                          EventPublisher eventPublisher) {
        log.info("Scaling runtime: DOCKER (single-host)");
        return new DockerRuntime(dockerClient, workerRegistry, eventPublisher, props);
    }

    @Bean
    @ConditionalOnProperty(name = "cloudbalancer.dispatcher.scaling.runtime-mode", havingValue = "AGENT")
    public NodeRuntime agentNodeRuntime(KafkaTemplate<String, String> kafkaTemplate,
                                         AgentRegistry agentRegistry,
                                         PendingWorkerTracker pendingTracker) {
        log.info("Scaling runtime: AGENT (distributed)");
        return new AgentRuntime(kafkaTemplate, agentRegistry, pendingTracker);
    }
}
