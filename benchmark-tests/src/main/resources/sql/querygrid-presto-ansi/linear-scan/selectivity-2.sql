SELECT CAST(COUNT(orderkey) as bigint), CAST(SUM(quantity) as bigint), CAST(AVG(extendedprice) as bigint)
FROM "${schema}"."lineitem"@"${database}"
WHERE quantity < 2
