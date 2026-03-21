package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.event.CloudBalancerEvent;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CircuitBreaker circuitBreaker;

    public EventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                          @Qualifier("kafkaProducerCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.kafkaTemplate = kafkaTemplate;
        this.circuitBreaker = circuitBreaker;
    }

    public void publishEvent(String topic, String key, CloudBalancerEvent event) {
        try {
            String json = JsonUtil.mapper().writeValueAsString(event);
            circuitBreaker.executeRunnable(() -> kafkaTemplate.send(topic, key, json));
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is open — dropping event to topic {}: {}", topic, e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic {}", topic, e);
        }
    }

    public void publishMessage(String topic, String key, Object message) {
        try {
            String json = JsonUtil.mapper().writeValueAsString(message);
            circuitBreaker.executeRunnable(() -> kafkaTemplate.send(topic, key, json));
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is open — dropping message to topic {}: {}", topic, e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for topic {}", topic, e);
        }
    }
}
