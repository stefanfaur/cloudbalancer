package com.cloudbalancer.common.model;

import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void roleEnumHasFourValues() {
        assertThat(Role.values()).containsExactly(
            Role.ADMIN, Role.OPERATOR, Role.VIEWER, Role.API_CLIENT
        );
    }

    @Test
    void roleSerializesAsString() throws Exception {
        String json = JsonUtil.mapper().writeValueAsString(Role.ADMIN);
        assertThat(json).isEqualTo("\"ADMIN\"");

        Role deserialized = JsonUtil.mapper().readValue(json, Role.class);
        assertThat(deserialized).isEqualTo(Role.ADMIN);
    }
}
