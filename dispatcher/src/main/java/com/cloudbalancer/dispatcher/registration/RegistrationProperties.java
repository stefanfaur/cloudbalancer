package com.cloudbalancer.dispatcher.registration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudbalancer.registration")
public class RegistrationProperties {
    private String kafkaBootstrapExternal = "localhost:9092";
    private String kafkaUsername = "cloudbalancer-agent";
    private String kafkaPassword = "changeme";

    public String getKafkaBootstrapExternal() { return kafkaBootstrapExternal; }
    public void setKafkaBootstrapExternal(String v) { this.kafkaBootstrapExternal = v; }
    public String getKafkaUsername() { return kafkaUsername; }
    public void setKafkaUsername(String v) { this.kafkaUsername = v; }
    public String getKafkaPassword() { return kafkaPassword; }
    public void setKafkaPassword(String v) { this.kafkaPassword = v; }
}
