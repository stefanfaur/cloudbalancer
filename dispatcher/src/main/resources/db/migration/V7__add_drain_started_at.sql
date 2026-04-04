-- V7: Add drain_started_at column to workers table for scale-down reconciliation
ALTER TABLE workers ADD COLUMN drain_started_at TIMESTAMP WITH TIME ZONE;
