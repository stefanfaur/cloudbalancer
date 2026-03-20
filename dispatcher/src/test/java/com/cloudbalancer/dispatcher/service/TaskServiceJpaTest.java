package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class TaskServiceJpaTest {

    @Autowired TaskService taskService;
    @Autowired TaskRepository taskRepository;

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
    }

    @Test
    void submitTaskPersistsToDatabase() {
        var descriptor = testDescriptor(Priority.NORMAL);
        var envelope = taskService.submitTask(descriptor);

        assertThat(taskRepository.findById(envelope.getId())).isPresent();
        var record = taskRepository.findById(envelope.getId()).get();
        assertThat(record.getState()).isEqualTo(TaskState.QUEUED);
    }

    @Test
    void getTaskReturnsEnvelope() {
        var descriptor = testDescriptor(Priority.NORMAL);
        var created = taskService.submitTask(descriptor);

        var fetched = taskService.getTask(created.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getState()).isEqualTo(TaskState.QUEUED);
        assertThat(fetched.getDescriptor().executorType()).isEqualTo(ExecutorType.SIMULATED);
    }

    @Test
    void getQueuedTasksOrderedByPriority() {
        taskService.submitTask(testDescriptor(Priority.LOW));
        taskService.submitTask(testDescriptor(Priority.CRITICAL));
        taskService.submitTask(testDescriptor(Priority.NORMAL));

        var queued = taskService.getQueuedTasks();
        assertThat(queued).extracting(t -> t.getDescriptor().priority())
            .containsExactly(Priority.CRITICAL, Priority.NORMAL, Priority.LOW);
    }

    @Test
    void listTasksReturnsAll() {
        taskService.submitTask(testDescriptor(Priority.NORMAL));
        taskService.submitTask(testDescriptor(Priority.HIGH));

        var all = taskService.listTasks();
        assertThat(all).hasSize(2);
    }

    private TaskDescriptor testDescriptor(Priority priority) {
        return new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 100),
            new ResourceProfile(1, 256, 50, false, 10, false),
            TaskConstraints.unconstrained(), priority,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
    }
}
