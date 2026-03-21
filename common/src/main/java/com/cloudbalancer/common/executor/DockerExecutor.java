package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.model.ExecutorCapabilities;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.SecurityLevel;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DockerExecutor implements TaskExecutor {

    private final DockerClient dockerClient;
    private final ConcurrentHashMap<UUID, String> runningContainers = new ConcurrentHashMap<>();

    public DockerExecutor(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public ExecutionResult execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context) {
        ValidationResult vr = validate(spec);
        if (!vr.valid()) {
            return new ExecutionResult(1, "", "Validation failed: " + vr.errors(), 0, false);
        }

        String image = (String) spec.get("image");

        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) spec.get("command");

        @SuppressWarnings("unchecked")
        Map<String, String> environment = (Map<String, String>) spec.get("environment");

        Long memoryLimitBytes = spec.containsKey("memoryLimitBytes")
                ? ((Number) spec.get("memoryLimitBytes")).longValue() : null;
        Long memorySwapBytes = spec.containsKey("memorySwapBytes")
                ? ((Number) spec.get("memorySwapBytes")).longValue() : null;
        Integer cpuCount = spec.containsKey("cpuCount")
                ? ((Number) spec.get("cpuCount")).intValue() : null;
        Boolean readOnlyRootfs = (Boolean) spec.get("readOnlyRootfs");
        Boolean networkDisabled = (Boolean) spec.get("networkDisabled");

        long start = System.currentTimeMillis();
        String containerId = null;

        try {
            // Pull image
            dockerClient.pullImageCmd(image)
                    .start()
                    .awaitCompletion(120, TimeUnit.SECONDS);

            // Build host config with security hardening
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withSecurityOpts(List.of("no-new-privileges"))
                    .withCapDrop(Capability.ALL);

            if (memoryLimitBytes != null) {
                hostConfig.withMemory(memoryLimitBytes);
            }
            if (memorySwapBytes != null) {
                hostConfig.withMemorySwap(memorySwapBytes);
            }
            if (cpuCount != null) {
                hostConfig.withCpuCount((long) cpuCount);
            }
            if (Boolean.TRUE.equals(readOnlyRootfs)) {
                hostConfig.withReadonlyRootfs(true);
            }

            // Build environment variables list
            List<String> envList = new ArrayList<>();
            if (environment != null) {
                environment.forEach((k, v) -> envList.add(k + "=" + v));
            }

            // Create container
            CreateContainerCmd createCmd = dockerClient.createContainerCmd(image)
                    .withHostConfig(hostConfig)
                    .withLabels(Map.of("cloudbalancer.task-id", context.taskId().toString()));

            if (command != null && !command.isEmpty()) {
                createCmd.withCmd(command);
            }
            if (!envList.isEmpty()) {
                createCmd.withEnv(envList);
            }
            if (Boolean.TRUE.equals(networkDisabled)) {
                createCmd.withNetworkDisabled(true);
            }

            CreateContainerResponse container = createCmd.exec();
            containerId = container.getId();

            // Track for cancellation
            runningContainers.put(context.taskId(), containerId);

            // Start container
            dockerClient.startContainerCmd(containerId).exec();

            // Wait for container to finish
            int exitCode = dockerClient.waitContainerCmd(containerId)
                    .start()
                    .awaitStatusCode(300, TimeUnit.SECONDS);

            // Collect logs
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(false)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            String payload = new String(frame.getPayload());
                            if (frame.getStreamType() == StreamType.STDOUT) {
                                stdout.append(payload);
                            } else if (frame.getStreamType() == StreamType.STDERR) {
                                stderr.append(payload);
                            }
                        }
                    })
                    .awaitCompletion(30, TimeUnit.SECONDS);

            long elapsed = System.currentTimeMillis() - start;
            return new ExecutionResult(exitCode, stdout.toString(), stderr.toString(), elapsed, false);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long elapsed = System.currentTimeMillis() - start;
            return new ExecutionResult(1, "", "Execution interrupted", elapsed, true);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return new ExecutionResult(1, "", e.getMessage(), elapsed, false);
        } finally {
            // Clean up container
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId)
                            .withForce(true)
                            .exec();
                } catch (Exception ignored) {
                    // Best effort removal
                }
            }
            runningContainers.remove(context.taskId());
        }
    }

    @Override
    public ValidationResult validate(Map<String, Object> spec) {
        if (!(spec.get("image") instanceof String image) || image.isBlank()) {
            return ValidationResult.invalid("Missing required field: image");
        }
        return ValidationResult.ok();
    }

    @Override
    public ResourceEstimate estimateResources(Map<String, Object> spec) {
        return new ResourceEstimate(1, 512, 120_000);
    }

    @Override
    public ExecutorCapabilities getCapabilities() {
        return new ExecutorCapabilities(
                true,
                false,
                new ResourceProfile(8, 16384, 51200, false, 3600, false),
                SecurityLevel.ISOLATED
        );
    }

    @Override
    public ExecutorType getExecutorType() {
        return ExecutorType.DOCKER;
    }

    @Override
    public void cancel(ExecutionHandle handle) {
        UUID taskId = UUID.fromString(handle.handleId());
        String containerId = runningContainers.get(taskId);
        if (containerId != null) {
            try {
                dockerClient.killContainerCmd(containerId).exec();
            } catch (Exception ignored) {
                // Container may have already stopped
            }
        }
    }
}
