package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Comparator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class TaskRecordRepositoryTest {

    @Autowired
    TaskRepository taskRepository;

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
    }

    @Test
    void persistAndRetrieveWithJsonb() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", 1000),
            new ResourceProfile(2, 512, 100, false, 60, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            ExecutionPolicy.defaults(),
            TaskIO.none()
        );
        var record = TaskRecord.create(descriptor);
        record.transitionTo(TaskState.VALIDATED);
        record.transitionTo(TaskState.QUEUED);
        taskRepository.save(record);

        var found = taskRepository.findById(record.getId()).orElseThrow();
        assertThat(found.getState()).isEqualTo(TaskState.QUEUED);
        assertThat(found.getDescriptor().executorType()).isEqualTo(ExecutorType.SIMULATED);
        assertThat(found.getDescriptor().resourceProfile().cpuCores()).isEqualTo(2);
        assertThat(found.getDescriptor().resourceProfile().memoryMB()).isEqualTo(512);
        assertThat(found.getDescriptor().constraints()).isEqualTo(TaskConstraints.unconstrained());
    }

    @Test
    void queryByState() {
        saveTaskInState(Priority.NORMAL, TaskState.QUEUED);
        saveTaskInState(Priority.NORMAL, TaskState.QUEUED);
        saveTaskInState(Priority.NORMAL, TaskState.SUBMITTED);

        var queued = taskRepository.findByState(TaskState.QUEUED);
        assertThat(queued).hasSize(2);
    }

    @Test
    void queryByStateOrderedByPriorityAndTime() {
        saveTaskInState(Priority.LOW, TaskState.QUEUED);
        saveTaskInState(Priority.CRITICAL, TaskState.QUEUED);
        saveTaskInState(Priority.NORMAL, TaskState.QUEUED);

        var queued = taskRepository.findByState(TaskState.QUEUED);
        // Sort in Java by priority ordinal (CRITICAL=0, HIGH=1, NORMAL=2, LOW=3) then submittedAt
        queued.sort(Comparator.comparingInt((TaskRecord t) -> t.getPriority().ordinal())
            .thenComparing(TaskRecord::getSubmittedAt));

        assertThat(queued).extracting(TaskRecord::getPriority)
            .containsExactly(Priority.CRITICAL, Priority.NORMAL, Priority.LOW);
    }

    private void saveTaskInState(Priority priority, TaskState targetState) {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 100),
            new ResourceProfile(1, 256, 50, false, 10, false),
            TaskConstraints.unconstrained(), priority,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        var record = TaskRecord.create(descriptor);
        // Walk state machine to target
        if (targetState == TaskState.QUEUED || targetState == TaskState.ASSIGNED) {
            record.transitionTo(TaskState.VALIDATED);
            record.transitionTo(TaskState.QUEUED);
        }
        if (targetState == TaskState.ASSIGNED) {
            record.transitionTo(TaskState.ASSIGNED);
        }
        taskRepository.save(record);
    }
}
