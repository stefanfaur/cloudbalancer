package com.cloudbalancer.common.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DrainCommand.class, name = "DRAIN")
})
public sealed interface WorkerCommand permits DrainCommand {
    String workerId();
    String commandType();
}
