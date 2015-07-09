-- create benchmark_runs_variables table
CREATE TABLE benchmark_runs_variables
(
  benchmark_run_id BIGINT       NOT NULL,
  name             VARCHAR(255) NOT NULL,
  value            VARCHAR(255) NOT NULL,
  PRIMARY KEY (benchmark_run_id, name)
);
ALTER TABLE benchmark_runs_variables ADD FOREIGN KEY (benchmark_run_id) REFERENCES benchmark_runs (id);

-- add variables_name column to benchmark_runs
ALTER TABLE benchmark_runs ADD COLUMN unique_name VARCHAR(1024) DEFAULT NULL;

-- copy name to variables_name
UPDATE benchmark_runs
SET unique_name = name;

-- set variables_name NOT NULL
ALTER TABLE benchmark_runs ALTER COLUMN unique_name SET NOT NULL;