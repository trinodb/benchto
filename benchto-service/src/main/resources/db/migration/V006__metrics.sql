CREATE TABLE metrics
(
    id   BIGSERIAL PRIMARY KEY NOT NULL,
    name VARCHAR(64)           NOT NULL,
    unit VARCHAR(16)           NOT NULL
);

CREATE TABLE metric_attributes
(
    metric_id BIGINT       NOT NULL,
    name      VARCHAR(255) NOT NULL,
    value     VARCHAR      NOT NULL,
    PRIMARY KEY (metric_id, name)
);

ALTER TABLE metric_attributes
    ADD FOREIGN KEY (metric_id) REFERENCES metrics (id);

INSERT INTO metrics (name, unit)
VALUES ('network', 'BYTES'),
       ('network', 'BYTES'),
       ('network', 'BYTES'),
       ('cpu', 'PERCENT'),
       ('cpu', 'PERCENT'),
       ('memory', 'PERCENT'),
       ('memory', 'PERCENT'),
       ('analysisTime', 'MILLISECONDS'),
       ('finishingTime', 'MILLISECONDS'),
       ('internalNetworkInputDataSize', 'BYTES'),
       ('outputDataSize', 'BYTES'),
       ('peakTotalMemoryReservation', 'BYTES'),
       ('physicalInputDataSize', 'BYTES'),
       ('physicalWrittenDataSize', 'BYTES'),
       ('planningTime', 'MILLISECONDS'),
       ('processedInputDataSize', 'BYTES'),
       ('rawInputDataSize', 'BYTES'),
       ('totalBlockedTime', 'MILLISECONDS'),
       ('totalCpuTime', 'MILLISECONDS'),
       ('totalScheduledTime', 'MILLISECONDS'),
       ('duration', 'MILLISECONDS'),
       ('throughput', 'QUERY_PER_SECOND')
;

INSERT INTO metric_attributes (metric_id, name, "value")
SELECT id, 'scope', 'query'
FROM metrics
WHERE name IN (
               'analysisTime', 'finishingTime', 'internalNetworkInputDataSize', 'outputDataSize', 'peakTotalMemoryReservation',
               'physicalInputDataSize', 'physicalWrittenDataSize', 'planningTime', 'processedInputDataSize',
               'rawInputDataSize', 'totalBlockedTime', 'totalCpuTime', 'totalScheduledTime');

INSERT INTO metric_attributes (metric_id, name, "value")
SELECT id, 'scope', 'cluster'
FROM metrics
WHERE name IN ('cpu', 'memory', 'network');

INSERT INTO metric_attributes (metric_id, name, "value")
SELECT id, 'aggregate', 'max'
FROM metrics
WHERE name IN ('cpu', 'memory', 'network')
  AND id IN (SELECT min(id) FROM metrics m GROUP BY name);

INSERT INTO metric_attributes (metric_id, name, "value")
SELECT id, 'aggregate', 'mean'
FROM metrics
WHERE name IN ('cpu', 'memory', 'network')
  AND id IN (SELECT min(id) FROM metrics m WHERE id NOT IN (SELECT metric_id FROM metric_attributes WHERE name = 'aggregate') GROUP BY name);

INSERT INTO metric_attributes (metric_id, name, "value")
SELECT id, 'aggregate', 'total'
FROM metrics
WHERE name IN ('network')
  AND id IN (SELECT min(id) FROM metrics m WHERE id NOT IN (SELECT metric_id FROM metric_attributes WHERE name = 'aggregate') GROUP BY name);

INSERT INTO metric_attributes (metric_id, name, "value")
SELECT id, 'scope', 'driver'
FROM metrics
WHERE name IN ('duration', 'throughput');

INSERT INTO metrics (name, unit)
SELECT DISTINCT name, unit
FROM measurements
EXCEPT
SELECT *
FROM (SELECT name, unit
      FROM metrics
      UNION
      SELECT COALESCE(scope.value || '-', '') || m.name || COALESCE('_' || aggregate.value, ''), unit
      FROM metrics m
               LEFT JOIN metric_attributes scope on m.id = scope.metric_id AND scope.name = 'scope' AND scope.value IN ('query', 'cluster')
               LEFT JOIN metric_attributes aggregate on m.id = aggregate.metric_id AND aggregate.name = 'aggregate') m;

ALTER TABLE measurements
    ADD COLUMN metric_id BIGINT;
ALTER TABLE measurements
    ADD FOREIGN KEY (metric_id) REFERENCES metrics (id);

UPDATE measurements
SET metric_id = (SELECT m.id
                 FROM metrics m
                          LEFT JOIN metric_attributes scope on m.id = scope.metric_id AND scope.name = 'scope' AND scope.value IN ('query', 'cluster')
                          LEFT JOIN metric_attributes aggregate on m.id = aggregate.metric_id AND aggregate.name = 'aggregate'
                 WHERE COALESCE(scope.value || '-', '') || m.name || COALESCE('_' || aggregate.value, '') = measurements.name
                   AND m.unit = measurements.unit)
WHERE metric_id IS NULL;

UPDATE measurements
SET name = (SELECT m.name || COALESCE(' {' || NULLIF(array_to_string(array_agg(a.name || '=' || a.value ORDER BY a.name), ','), '') || '}', '')
            FROM metrics m
                     LEFT JOIN metric_attributes a on m.id = a.metric_id
            WHERE m.id = measurements.metric_id
            GROUP BY m.name);
