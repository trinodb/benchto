INSERT INTO hive.default.${table} SELECT *
FROM "${database}"."${schema}"."lineitem"
WHERE quantity < 2