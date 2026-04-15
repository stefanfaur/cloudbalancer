package com.cloudbalancer.agent.config;

import com.cloudbalancer.agent.registration.AgentRegistrationClient;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnExpression("!'${cloudbalancer.agent.registration-token:}'.isEmpty()")
public class KafkaRegistrationConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaRegistrationConfig.class);

    @Bean
    public AgentRegistrationClient.RegistrationResult registrationResult(AgentRegistrationClient client) {
        var result = client.register();
        if (result == null) {
            throw new IllegalStateException("Registration token is set but registration returned null");
        }
        return result;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory(AgentRegistrationClient.RegistrationResult result) {
        log.info("Configuring Kafka producer with SASL for remote agent");
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, result.kafkaBootstrap());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        addSaslProperties(props, result);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory(AgentRegistrationClient.RegistrationResult result,
                                                            AgentProperties agentProps) {
        log.info("Configuring Kafka consumer with SASL for remote agent");
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, result.kafkaBootstrap());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "agent-" + agentProps.getId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        addSaslProperties(props, result);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    private void addSaslProperties(Map<String, Object> props, AgentRegistrationClient.RegistrationResult result) {
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG,
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
            + result.kafkaUsername() + "\" password=\"" + result.kafkaPassword() + "\";");
    }
}
