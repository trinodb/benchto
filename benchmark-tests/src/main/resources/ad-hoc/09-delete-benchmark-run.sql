-- you can provide arbitrary predicate in where statement below, all child
-- entities of benchmark run will be deleted
CREATE TEMPORARY TABLE delete_benchmark_run_ids AS
  SELECT br.id
  FROM benchmark_runs br
  WHERE br.unique_name = 'presto_linear-scan_database=hive_schema=tpch_10tb_orc_selectivity=2'
        AND br.sequence_id = '2015-08-13T13:15:15:294';

CREATE TEMPORARY TABLE delete_measurement_ids AS
  SELECT m.id
  FROM measurements m
    INNER JOIN benchmark_run_measurements brm ON m.id = brm.measurement_id
    INNER JOIN delete_benchmark_run_ids dbrm ON dbrm.id = brm.benchmark_run_id;

DELETE FROM benchmark_run_measurements
WHERE measurement_id IN (SELECT id
                         FROM delete_measurement_ids);
DELETE FROM measurements
WHERE id IN (SELECT id
             FROM delete_measurement_ids);

DELETE FROM benchmark_runs_attributes
WHERE benchmark_run_id IN (SELECT br.id
                           FROM delete_benchmark_run_ids br);

DELETE FROM execution_attributes
WHERE execution_id IN (SELECT e.id
                       FROM executions e
                       WHERE e.benchmark_run_id IN (SELECT br.id
                                                    FROM delete_benchmark_run_ids br));

CREATE TEMPORARY TABLE delete_execution_measurement_ids AS
  SELECT m.id
  FROM measurements m
    INNER JOIN execution_measurements em ON m.id = em.measurement_id
    INNER JOIN executions e ON e.id = em.execution_id
    INNER JOIN delete_benchmark_run_ids dbrm ON dbrm.id = e.benchmark_run_id;

DELETE FROM execution_measurements
WHERE measurement_id IN (SELECT id
                         FROM delete_execution_measurement_ids);
DELETE FROM measurements
WHERE id IN (SELECT id
             FROM delete_execution_measurement_ids);

DELETE FROM executions
WHERE benchmark_run_id IN (SELECT br.id
                           FROM delete_benchmark_run_ids br);

DELETE FROM benchmark_runs_variables
WHERE benchmark_run_id IN (SELECT br.id
                           FROM delete_benchmark_run_ids br);

DELETE FROM benchmark_runs
WHERE id IN (SELECT br.id
             FROM delete_benchmark_run_ids br);

DROP TABLE delete_benchmark_run_ids;
DROP TABLE delete_measurement_ids;
DROP TABLE delete_execution_measurement_ids;