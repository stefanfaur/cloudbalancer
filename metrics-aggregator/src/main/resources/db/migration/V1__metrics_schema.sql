-- V1: Metrics schema — TimescaleDB hypertables for worker metrics and heartbeats

CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE worker_metrics (
    id BIGSERIAL,
    worker_id VARCHAR(255) NOT NULL,
    cpu_usage_percent DOUBLE PRECISION NOT NULL,
    heap_used_mb BIGINT NOT NULL,
    heap_max_mb BIGINT NOT NULL,
    thread_count INT NOT NULL,
    active_task_count INT NOT NULL,
    completed_task_count BIGINT NOT NULL,
    failed_task_count BIGINT NOT NULL,
    avg_execution_duration_ms DOUBLE PRECISION NOT NULL,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL
);

SELECT create_hypertable('worker_metrics', 'reported_at');

SELECT add_retention_policy('worker_metrics', INTERVAL '7 days');

CREATE TABLE worker_heartbeats (
    id BIGSERIAL,
    worker_id VARCHAR(255) NOT NULL,
    health_state VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

SELECT create_hypertable('worker_heartbeats', 'timestamp');

SELECT add_retention_policy('worker_heartbeats', INTERVAL '3 days');

CREATE TABLE task_metrics (
    task_id UUID NOT NULL PRIMARY KEY,
    submitted_at TIMESTAMP WITH TIME ZONE,
    assigned_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    queue_wait_ms BIGINT,
    execution_duration_ms BIGINT,
    turnaround_ms BIGINT
);
