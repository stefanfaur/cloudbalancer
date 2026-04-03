package com.cloudbalancer.dispatcher.api.dto;

import com.cloudbalancer.common.model.Priority;
import java.util.List;
import java.util.UUID;

public record BulkReprioritizeRequest(List<UUID> taskIds, Priority priority) {}
