package com.cloudbalancer.agent.config;

import com.cloudbalancer.agent.registration.AgentRegistrationClient;
import com.cloudbalancer.agent.service.ContainerManager;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonUtil.mapper();
    }

    @Bean
    public ContainerManager containerManager(DockerClient dockerClient, AgentProperties props,
                                              AgentRegistrationClient registrationClient) {
        return new ContainerManager(dockerClient, props, registrationClient);
    }
}
