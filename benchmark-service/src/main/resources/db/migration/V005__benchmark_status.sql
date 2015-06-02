-- add status column to benchmark_runs and executions
ALTER TABLE benchmark_runs ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'STARTED';
ALTER TABLE benchmark_runs ALTER COLUMN status DROP DEFAULT;

ALTER TABLE executions ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'STARTED';
ALTER TABLE executions ALTER COLUMN status DROP DEFAULT;

