DELETE FROM benchmark_run_measurements brm
USING benchmark_runs br
WHERE brm.benchmark_run_id = br.id AND br.name = ?
      AND br.sequence_id = ?;
DELETE FROM benchmark_runs_attributes bra
USING benchmark_runs br
WHERE bra.benchmark_run_id = br.id AND br.name = ?
      AND br.sequence_id = ?;
DELETE FROM execution_measurements em
USING benchmark_runs br, executions e
WHERE e.benchmark_run_id = br.id AND em.execution_id = e.id AND br.name = ?
      AND br.sequence_id = ?;
DELETE FROM execution_attributes ea
USING benchmark_runs br, executions e
WHERE e.benchmark_run_id = br.id AND ea.execution_id = e.id AND br.name = ?
      AND br.sequence_id = ?;
DELETE FROM executions e
USING benchmark_runs br
WHERE e.benchmark_run_id = br.id AND br.name = ?
      AND br.sequence_id = ?;
DELETE FROM benchmark_runs
WHERE name = ?
      AND sequence_id = ?;
