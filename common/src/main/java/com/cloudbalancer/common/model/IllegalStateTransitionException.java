package com.cloudbalancer.common.model;

public class IllegalStateTransitionException extends RuntimeException {
    private final TaskState from;
    private final TaskState to;

    public IllegalStateTransitionException(TaskState from, TaskState to) {
        super("Invalid state transition: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public TaskState getFrom() { return from; }
    public TaskState getTo() { return to; }
}
