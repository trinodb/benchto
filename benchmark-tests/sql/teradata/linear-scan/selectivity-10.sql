SELECT COUNT(orderkey), SUM(quantity), AVG(extendedprice)
FROM "${schema}"."lineitem"
WHERE quantity < 6