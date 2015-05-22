CREATE TABLE benchmark_runs
(
  id          BIGSERIAL PRIMARY KEY NOT NULL,
  name        VARCHAR(64)           NOT NULL,
  sequence_id VARCHAR(64)           NOT NULL
);

CREATE TABLE executions
(
  id               BIGSERIAL PRIMARY KEY NOT NULL,
  sequence_id      VARCHAR(64)           NOT NULL,
  benchmark_run_id BIGINT                NOT NULL
);

CREATE TABLE measurements
(
  id    BIGSERIAL PRIMARY KEY NOT NULL,
  name  VARCHAR(64)           NOT NULL,
  unit  VARCHAR(16)           NOT NULL,
  value DOUBLE PRECISION      NOT NULL
);

CREATE TABLE benchmark_run_measurements
(
  benchmark_run_id BIGINT NOT NULL,
  measurement_id   BIGINT NOT NULL,
  PRIMARY KEY (benchmark_run_id, measurement_id)
);

CREATE TABLE execution_measurements
(
  execution_id   BIGINT NOT NULL,
  measurement_id BIGINT NOT NULL,
  PRIMARY KEY (execution_id, measurement_id)
);

ALTER TABLE benchmark_run_measurements ADD FOREIGN KEY (benchmark_run_id) REFERENCES benchmark_runs (id);
ALTER TABLE benchmark_run_measurements ADD FOREIGN KEY (measurement_id) REFERENCES measurements (id);

ALTER TABLE execution_measurements ADD FOREIGN KEY (execution_id) REFERENCES executions (id);
ALTER TABLE execution_measurements ADD FOREIGN KEY (measurement_id) REFERENCES measurements (id);

ALTER TABLE executions ADD FOREIGN KEY (benchmark_run_id) REFERENCES benchmark_runs (id);

CREATE UNIQUE INDEX idx_uk_benchmarks_name_seq_id ON benchmark_runs (name, sequence_id);
CREATE UNIQUE INDEX idx_uk_benchmark_measurements_mes_id ON benchmark_run_measurements (measurement_id);
CREATE UNIQUE INDEX idx_uk_execution_measurements_mes_id ON execution_measurements (measurement_id);
