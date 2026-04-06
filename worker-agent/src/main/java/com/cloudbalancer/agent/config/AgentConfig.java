package com.cloudbalancer.agent.config;

import com.cloudbalancer.agent.registration.AgentRegistrationClient;
import com.cloudbalancer.agent.service.ContainerManager;
import com.github.dockerjava.api.DockerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    public ContainerManager containerManager(DockerClient dockerClient, AgentProperties props,
                                              AgentRegistrationClient registrationClient) {
        return new ContainerManager(dockerClient, props, registrationClient);
    }
}
