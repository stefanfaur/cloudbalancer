-- V3: Add retry engine support
ALTER TABLE tasks ADD COLUMN retry_eligible_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tasks ADD COLUMN current_execution_id UUID;
CREATE INDEX idx_tasks_retry_eligible_at ON tasks(retry_eligible_at) WHERE state IN ('FAILED', 'TIMED_OUT');
