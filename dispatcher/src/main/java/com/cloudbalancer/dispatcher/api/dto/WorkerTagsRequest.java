package com.cloudbalancer.dispatcher.api.dto;

import java.util.Set;

public record WorkerTagsRequest(Set<String> tags) {}
