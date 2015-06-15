SELECT
  c_name,
  c_custkey,
  o_orderkey,
  o_orderdate,
  o_totalprice,
  sum(l_quantity)
FROM
  customer@presto_server,
  orders@presto_server,
  lineitem@presto_server
WHERE
  o_orderkey IN (
    SELECT l_orderkey
    FROM
      lineitem@presto_server
    GROUP BY
      l_orderkey
    HAVING
      sum(l_quantity) > 300
  )
  AND c_custkey = o_custkey
  AND o_orderkey = l_orderkey
GROUP BY
  c_name,
  c_custkey,
  o_orderkey,
  o_orderdate,
  o_totalprice
ORDER BY
  o_totalprice DESC,
  o_orderdate
SAMPLE 100
