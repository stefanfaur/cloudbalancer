ALTER TABLE workers ADD COLUMN agent_id VARCHAR(255);
ALTER TABLE workers ADD COLUMN stopping_started_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_workers_agent_id ON workers(agent_id);
