package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class TaskTimestampsTest {

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
    }

    @Test
    void v2MigrationAddsTimestampColumns() {
        // V2 migration should have run; verify columns exist via raw SQL
        var columns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns " +
            "WHERE table_name = 'tasks' AND column_name IN ('assigned_at', 'started_at', 'completed_at')",
            String.class);
        assertThat(columns).containsExactlyInAnyOrder("assigned_at", "started_at", "completed_at");
    }

    @Test
    void assignedAtSetOnAssignment() {
        var record = createTask();
        record.transitionTo(TaskState.VALIDATED);
        record.transitionTo(TaskState.QUEUED);
        taskRepository.save(record);

        // Simulate assignment: set assignedAt
        var saved = taskRepository.findById(record.getId()).orElseThrow();
        saved.transitionTo(TaskState.ASSIGNED);
        saved.setAssignedWorkerId("worker-1");
        saved.setAssignedAt(Instant.now());
        taskRepository.save(saved);

        var reloaded = taskRepository.findById(record.getId()).orElseThrow();
        assertThat(reloaded.getAssignedAt()).isNotNull();
        assertThat(Duration.between(reloaded.getAssignedAt(), Instant.now()).abs())
            .isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void startedAtAndCompletedAtSetOnResultProcessing() {
        var record = createTask();
        record.transitionTo(TaskState.VALIDATED);
        record.transitionTo(TaskState.QUEUED);
        record.transitionTo(TaskState.ASSIGNED);
        record.setAssignedWorkerId("worker-1");
        record.setAssignedAt(Instant.now());
        taskRepository.save(record);

        // Simulate result processing: set startedAt and completedAt
        var saved = taskRepository.findById(record.getId()).orElseThrow();
        Instant completedAt = Instant.now();
        Instant startedAt = completedAt.minusMillis(500);
        saved.setStartedAt(startedAt);
        saved.setCompletedAt(completedAt);
        taskRepository.save(saved);

        var reloaded = taskRepository.findById(record.getId()).orElseThrow();
        assertThat(reloaded.getStartedAt()).isNotNull();
        assertThat(reloaded.getCompletedAt()).isNotNull();
        assertThat(reloaded.getCompletedAt()).isAfter(reloaded.getStartedAt());
    }

    @Test
    void timestampsNullableForUnassignedTasks() {
        var record = createTask();
        record.transitionTo(TaskState.VALIDATED);
        record.transitionTo(TaskState.QUEUED);
        taskRepository.save(record);

        var reloaded = taskRepository.findById(record.getId()).orElseThrow();
        assertThat(reloaded.getAssignedAt()).isNull();
        assertThat(reloaded.getStartedAt()).isNull();
        assertThat(reloaded.getCompletedAt()).isNull();
    }

    private TaskRecord createTask() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", 100),
            new ResourceProfile(1, 256, 50, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            ExecutionPolicy.defaults(),
            TaskIO.none()
        );
        return TaskRecord.create(descriptor);
    }
}
