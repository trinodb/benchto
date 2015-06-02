-- create execution_attributes and benchmark_runs_attributes
CREATE TABLE execution_attributes
(
  execution_id BIGINT         NOT NULL,
  name         VARCHAR(255)   NOT NULL,
  value        VARCHAR(64000) NOT NULL,
  PRIMARY KEY (execution_id, name)
);

ALTER TABLE execution_attributes ADD FOREIGN KEY (execution_id) REFERENCES executions (id);

CREATE TABLE benchmark_runs_attributes
(
  benchmark_run_id BIGINT         NOT NULL,
  name             VARCHAR(255)   NOT NULL,
  value            VARCHAR(64000) NOT NULL,
  PRIMARY KEY (benchmark_run_id, name)
);

ALTER TABLE benchmark_runs_attributes ADD FOREIGN KEY (benchmark_run_id) REFERENCES benchmark_runs (id);