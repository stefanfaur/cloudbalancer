package com.cloudbalancer.dispatcher.api.dto;

import java.util.List;
import java.util.UUID;

public record BulkTaskRequest(List<UUID> taskIds) {}
