SELECT cast(COUNT(orderkey) as decimal(18,2)), cast(SUM(quantity) as decimal(18,2)), cast(AVG(extendedprice) as decimal(18,2))
FROM "${schema}"."lineitem"
WHERE quantity < 2