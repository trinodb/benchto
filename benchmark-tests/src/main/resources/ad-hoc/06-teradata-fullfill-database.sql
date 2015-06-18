-- must be run from "bteq .logon localhost/test_presto_user,test_presto_password"

CREATE TABLE "tpch_100gb"."customer" AS (SELECT * FROM "sf100"."customer"@"tpch") WITH DATA;
CREATE TABLE "tpch_100gb"."lineitem" AS (SELECT * FROM "sf100"."lineitem"@"tpch") WITH DATA;
CREATE TABLE "tpch_100gb"."nation" AS (SELECT * FROM "sf100"."nation"@"tpch") WITH DATA;
CREATE TABLE "tpch_100gb"."orders" AS (SELECT * FROM "sf100"."orders"@"tpch") WITH DATA;
CREATE TABLE "tpch_100gb"."part" AS (SELECT * FROM "sf100"."part"@"tpch") WITH DATA;
CREATE TABLE "tpch_100gb"."partsupp" AS (SELECT * FROM "sf100"."partsupp"@"tpch") WITH DATA;
CREATE TABLE "tpch_100gb"."region" AS (SELECT * FROM "sf100"."region"@"tpch") WITH DATA;
CREATE TABLE "tpch_100gb"."supplier" AS (SELECT * FROM "sf100"."supplier"@"tpch") WITH DATA;