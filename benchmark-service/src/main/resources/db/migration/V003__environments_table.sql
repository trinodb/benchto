-- create environments and environment_attributes
CREATE TABLE environments
(
  id      BIGSERIAL PRIMARY KEY NOT NULL,
  name    VARCHAR(64)           NOT NULL,
  version BIGINT                NOT NULL,
  started TIMESTAMP             NOT NULL
);

CREATE TABLE environment_attributes
(
  environment_id BIGINT         NOT NULL,
  name           VARCHAR(255)   NOT NULL,
  value          VARCHAR(64000) NOT NULL,
  PRIMARY KEY (environment_id, name)
);

ALTER TABLE environment_attributes ADD FOREIGN KEY (environment_id) REFERENCES environments (id);
CREATE UNIQUE INDEX idx_uk_environments_name ON environments (name);

-- create default environment
INSERT INTO environments (name, version, started) VALUES ('DEFAULT', 0, CURRENT_TIMESTAMP);

-- add environment_id column to benchmark_runs
ALTER TABLE benchmark_runs ADD COLUMN environment_id BIGINT;

-- set DEFAULT environment for existing benchmark_runs
UPDATE benchmark_runs
SET environment_id = (SELECT id
                      FROM environments
                      WHERE name = 'DEFAULT');

-- make environment mandatory
ALTER TABLE benchmark_runs ALTER COLUMN environment_id SET NOT NULL;

-- setup foreign key
ALTER TABLE benchmark_runs ADD FOREIGN KEY (environment_id) REFERENCES environments (id);