package com.cloudbalancer.agent.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerClientConfig {

    @Bean
    public DockerClient dockerClient(AgentProperties props) {
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(props.getDocker().getHost())
            .withApiVersion("1.41")
            .build();
        var httpClient = new ZerodepDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }
}
