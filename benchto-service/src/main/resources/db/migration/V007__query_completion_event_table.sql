CREATE TABLE query_completion_event
(
  id             BIGSERIAL PRIMARY KEY NOT NULL,
  event          jsonb                 NOT NULL
);

ALTER TABLE executions ADD COLUMN query_completion_event_id BIGINT;
ALTER TABLE executions ADD FOREIGN KEY (query_completion_event_id) REFERENCES query_completion_event (id);
