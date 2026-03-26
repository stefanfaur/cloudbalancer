package com.cloudbalancer.dispatcher.config;

import com.cloudbalancer.common.runtime.NodeRuntime;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.scaling.DispatcherLocalRuntime;
import com.cloudbalancer.dispatcher.scaling.ProcessRuntime;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScalingConfig {

    @Bean
    public NodeRuntime nodeRuntime(ScalingProperties props,
                                   WorkerRegistryService workerRegistry,
                                   EventPublisher eventPublisher) {
        if ("PROCESS".equalsIgnoreCase(props.getRuntimeMode())) {
            return new ProcessRuntime(workerRegistry, eventPublisher, props);
        }
        return new DispatcherLocalRuntime(workerRegistry, eventPublisher);
    }
}
