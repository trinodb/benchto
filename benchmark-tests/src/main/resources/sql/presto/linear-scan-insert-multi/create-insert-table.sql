<#list 0..<concurrency_level?number as execution_sequence_id>
DROP TABLE IF EXISTS hive.default.${table}_${execution_sequence_id} ${"\x003B"}
CREATE TABLE hive.default.${table}_${execution_sequence_id} AS SELECT * FROM "tpch"."sf10"."lineitem" LIMIT 0 ${"\x003B"}
</#list>
