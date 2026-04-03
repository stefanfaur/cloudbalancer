package com.cloudbalancer.dispatcher.api.dto;

import com.cloudbalancer.common.model.TaskEnvelope;
import java.util.List;

public record TaskPageResponse(List<TaskEnvelope> tasks, long total, int offset, int limit) {}
