-- V4: Add worker recovery tracking
ALTER TABLE workers ADD COLUMN recovery_started_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_workers_recovery_started_at ON workers(recovery_started_at) WHERE health_state = 'RECOVERING';
