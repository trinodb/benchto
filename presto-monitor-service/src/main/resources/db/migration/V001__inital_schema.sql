CREATE TABLE snapshots
(
  id          BIGSERIAL PRIMARY KEY NOT NULL,
  timestamp   TIMESTAMP             NOT NULL
);

CREATE TABLE documents
(
  id          BIGSERIAL PRIMARY KEY NOT NULL,
  name        VARCHAR(64)           NOT NULL,
  timestamp   TIMESTAMP             NOT NULL,
  content     TEXT                  NOT NULL,
  snapshot_id BIGINT                NOT NULL
);

ALTER TABLE documents ADD FOREIGN KEY (snapshot_id) REFERENCES snapshots (id);

CREATE UNIQUE INDEX idx_uk_documents_name ON documents (name, timestamp);

