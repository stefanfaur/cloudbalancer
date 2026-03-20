-- V2: Add lifecycle timestamps to tasks table for per-task metrics
ALTER TABLE tasks ADD COLUMN assigned_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tasks ADD COLUMN started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tasks ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;
