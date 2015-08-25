SELECT * FROM FOREIGN TABLE (
    SELECT COUNT(orderkey) AS c, SUM(quantity) AS q, AVG(extendedprice) AS e
    FROM "${database}"."${schema}"."lineitem"
    WHERE quantity < 51
)@"${serverType}_pushdown" presto_push_down