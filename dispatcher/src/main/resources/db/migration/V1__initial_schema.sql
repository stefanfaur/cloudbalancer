-- V1: Initial schema — migrates H2 auth tables to PostgreSQL and adds Phase 4 tables

-- Auth tables (migrated from H2)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Task store (hybrid: columns for queryable fields, jsonb for complex structures)
CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    state VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    executor_type VARCHAR(50) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    assigned_worker_id VARCHAR(255),
    descriptor JSONB NOT NULL,
    execution_history JSONB NOT NULL DEFAULT '[]'
);

CREATE INDEX idx_tasks_state ON tasks(state);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_assigned_worker ON tasks(assigned_worker_id);

-- Worker registry with resource ledger
CREATE TABLE workers (
    id VARCHAR(255) PRIMARY KEY,
    health_state VARCHAR(50) NOT NULL,
    capabilities JSONB NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    allocated_cpu INT NOT NULL DEFAULT 0,
    allocated_memory_mb INT NOT NULL DEFAULT 0,
    allocated_disk_mb INT NOT NULL DEFAULT 0,
    active_task_count INT NOT NULL DEFAULT 0
);

-- Scheduling config (singleton row)
CREATE TABLE scheduling_config (
    id BIGSERIAL PRIMARY KEY,
    strategy_name VARCHAR(50) NOT NULL,
    weights JSONB NOT NULL DEFAULT '{}'
);
