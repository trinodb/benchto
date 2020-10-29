CREATE TABLE query_info
(
  id             BIGSERIAL PRIMARY KEY NOT NULL,
  info           TEXT                  NOT NULL
);

ALTER TABLE executions ADD COLUMN query_info_id BIGINT;
ALTER TABLE executions ADD FOREIGN KEY (query_info_id) REFERENCES query_info (id);
