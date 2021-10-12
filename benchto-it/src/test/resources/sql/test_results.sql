select
       max(totalprice) maxprice,
       min(orderdate) firstorder,
       region.name
from
     tpch.sf1.orders orders
         join tpch.sf1.customer customer on orders.custkey = customer.custkey
         join tpch.sf1.nation nation on customer.nationkey = nation.nationkey
         join tpch.sf1.region region on nation.regionkey = region.regionkey
group by region.name
order by region.name;
