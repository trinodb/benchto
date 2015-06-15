SELECT
  o_year,
  sum(CASE
      WHEN nation = 'BRAZIL'
        THEN volume
      ELSE 0
      END) / sum(volume) AS mkt_share
FROM (
       SELECT
         extract(YEAR FROM o_orderdate)     AS o_year,
         l_extendedprice * (1 - l_discount) AS volume,
         n2.n_name                          AS nation
       FROM
         part@presto_server,
         supplier@presto_server,
         lineitem@presto_server,
         orders@presto_server,
         customer@presto_server,
         nation@presto_server n1,
         nation@presto_server n2,
         region@presto_server
       WHERE
         p_partkey = l_partkey
         AND s_suppkey = l_suppkey
         AND l_orderkey = o_orderkey
         AND o_custkey = c_custkey
         AND c_nationkey = n1.n_nationkey
         AND n1.n_regionkey = r_regionkey
         AND r_name = 'AMERICA'
         AND s_nationkey = n2.n_nationkey
         AND o_orderdate BETWEEN DATE '1995-01-01' AND DATE '1996-12-31'
         AND p_type = 'ECONOMY ANODIZED STEEL'
     ) AS all_nations
GROUP BY
  o_year
ORDER BY
  o_year
