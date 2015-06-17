SELECT
  c.name,
  c.custkey,
  o.orderkey,
  o.orderdate,
  o.totalprice,
  sum(l.quantity)
FROM
  "${schema}"."customer"@"${database}" AS c,
  "${schema}"."orders"@"${database}" AS o,
  "${schema}"."lineitem"@"${database}" AS l
WHERE
  o.orderkey IN (
    SELECT l.orderkey
    FROM
      "${schema}"."lineitem"@"${database}" as l
    GROUP BY
      l.orderkey
    HAVING
      sum(l.quantity) > 300
  )
  AND c.custkey = o.custkey
  AND o.orderkey = l.orderkey
GROUP BY
  c.name,
  c.custkey,
  o.orderkey,
  o.orderdate,
  o.totalprice
ORDER BY
  o.totalprice DESC,
  o.orderdate
SAMPLE 100
