<#list 0..<concurrency_level?number as execution_sequence_id>
DROP TABLE IF EXISTS hive.default.${table}_${execution_sequence_id} ${"\x003B"}
</#list>
