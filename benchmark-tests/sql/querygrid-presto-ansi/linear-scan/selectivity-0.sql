SELECT COUNT(orderkey), SUM(quantity), AVG(extendedprice)
FROM "${schema}"."lineitem"@"${database}"
WHERE quantity < 1