-- V5: Add stdout/stderr columns for task execution output
ALTER TABLE tasks ADD COLUMN last_stdout TEXT;
ALTER TABLE tasks ADD COLUMN last_stderr TEXT;
