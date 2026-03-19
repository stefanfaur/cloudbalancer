package com.cloudbalancer.common.testutil;

import org.testcontainers.kafka.KafkaContainer;

public final class KafkaTestContainer {

    // Workaround: Kafka 3.9.0 rejects 0.0.0.0 in advertised.listeners, but TC 2.0.x
    // KafkaHelper still uses that format. Override with bare-port notation until TC ships
    // the fix (see https://github.com/testcontainers/testcontainers-java/issues/9506).
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.0")
            .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094");

    static {
        KAFKA.start();
    }

    private KafkaTestContainer() {}

    public static String getBootstrapServers() {
        return KAFKA.getBootstrapServers();
    }
}
