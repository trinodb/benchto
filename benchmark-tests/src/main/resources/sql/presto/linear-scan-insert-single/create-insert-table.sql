DROP TABLE IF EXISTS hive.default.${table};
CREATE TABLE hive.default.${table} AS SELECT * FROM "tpch"."sf10"."lineitem" LIMIT 0;
