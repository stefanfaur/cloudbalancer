package com.cloudbalancer.agent.registration;

import com.cloudbalancer.agent.config.AgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRegistrationClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void register_withNoToken_returnsNull() {
        var props = new AgentProperties();
        props.setRegistrationToken("");

        var client = new AgentRegistrationClient(props, objectMapper);
        assertThat(client.register()).isNull();
    }

    @Test
    void register_withNullToken_returnsNull() {
        var props = new AgentProperties();
        props.setRegistrationToken(null);

        var client = new AgentRegistrationClient(props, objectMapper);
        assertThat(client.register()).isNull();
    }
}
