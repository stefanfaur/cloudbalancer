package com.cloudbalancer.metrics;

import com.cloudbalancer.metrics.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class MetricsAggregatorApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void flywayMigrationCreatesAllTables() {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'metrics'",
            String.class);

        assertThat(tables).containsAll(List.of(
            "worker_metrics", "worker_heartbeats", "task_metrics"));
    }

    @Test
    void workerMetricsIsTimescaleHypertable() {
        List<String> hypertables = jdbcTemplate.queryForList(
            "SELECT hypertable_name FROM timescaledb_information.hypertables "
                + "WHERE hypertable_name = 'worker_metrics'",
            String.class);

        assertThat(hypertables).containsExactly("worker_metrics");
    }

    @Test
    void workerHeartbeatsIsTimescaleHypertable() {
        List<String> hypertables = jdbcTemplate.queryForList(
            "SELECT hypertable_name FROM timescaledb_information.hypertables "
                + "WHERE hypertable_name = 'worker_heartbeats'",
            String.class);

        assertThat(hypertables).containsExactly("worker_heartbeats");
    }
}
