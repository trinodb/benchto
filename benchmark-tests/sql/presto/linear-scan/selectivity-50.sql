SELECT COUNT(orderkey), SUM(quantity), AVG(extendedprice)
FROM "${database}"."${schema}"."lineitem"
WHERE quantity < 26