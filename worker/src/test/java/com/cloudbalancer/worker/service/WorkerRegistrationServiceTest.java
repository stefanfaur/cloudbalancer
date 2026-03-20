package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.event.WorkerRegisteredEvent;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.WorkerCapabilities;
import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkerRegistrationServiceTest {

    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void registerPublishesWorkerRegisteredEvent() throws Exception {
        var properties = new com.cloudbalancer.worker.config.WorkerProperties();
        properties.setId("test-worker");
        properties.setSupportedExecutors(Set.of(ExecutorType.SIMULATED));
        properties.setCpuCores(4);
        properties.setMemoryMb(8192);
        properties.setDiskMb(10240);
        properties.setTags(Set.of());

        var service = new WorkerRegistrationService(kafkaTemplate, properties);
        service.register();

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("workers.registration"), eq("test-worker"), valueCaptor.capture());

        WorkerRegisteredEvent event = JsonUtil.mapper().readValue(valueCaptor.getValue(), WorkerRegisteredEvent.class);
        assertThat(event.workerId()).isEqualTo("test-worker");
        assertThat(event.capabilities().supportedExecutors()).contains(ExecutorType.SIMULATED);
        assertThat(event.capabilities().totalResources().cpuCores()).isEqualTo(4);
    }
}
