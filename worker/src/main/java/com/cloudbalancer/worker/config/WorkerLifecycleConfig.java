package com.cloudbalancer.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
public class WorkerLifecycleConfig {

    @Bean
    public AtomicBoolean drainingFlag() {
        return new AtomicBoolean(false);
    }
}
