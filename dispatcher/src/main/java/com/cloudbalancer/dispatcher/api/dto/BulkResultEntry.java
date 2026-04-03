package com.cloudbalancer.dispatcher.api.dto;

import java.util.UUID;

public record BulkResultEntry(UUID taskId, boolean success, String reason) {}
