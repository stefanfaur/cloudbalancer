CREATE TABLE scaling_policy (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    min_workers INT NOT NULL DEFAULT 2,
    max_workers INT NOT NULL DEFAULT 20,
    cooldown_seconds INT NOT NULL DEFAULT 180,
    scale_up_step INT NOT NULL DEFAULT 1,
    scale_down_step INT NOT NULL DEFAULT 1,
    drain_time_seconds INT NOT NULL DEFAULT 60
);
