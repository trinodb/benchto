INSERT INTO hive.default.${table}_${execution_sequence_id} SELECT *
FROM "${database}"."${schema}"."lineitem"
WHERE quantity < 6