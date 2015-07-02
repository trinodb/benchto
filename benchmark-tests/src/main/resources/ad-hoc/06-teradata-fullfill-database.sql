-- must be run from "bteq .logon localhost/test_presto_user,test_presto_password"

CREATE TABLE "tpch_10gb"."customer" AS (SELECT * FROM "tpch_10gb_text"."customer"@"hive") WITH DATA;
CREATE TABLE "tpch_10gb"."lineitem" AS (SELECT * FROM "tpch_10gb_text"."lineitem"@"hive") WITH DATA;
CREATE TABLE "tpch_10gb"."nation" AS (SELECT * FROM "tpch_10gb_text"."nation"@"hive") WITH DATA;
CREATE TABLE "tpch_10gb"."orders" AS (SELECT * FROM "tpch_10gb_text"."orders"@"hive") WITH DATA;
CREATE TABLE "tpch_10gb"."part" AS (SELECT * FROM "tpch_10gb_text"."part"@"hive") WITH DATA;
CREATE TABLE "tpch_10gb"."partsupp" AS (SELECT * FROM "tpch_10gb_text"."partsupp"@"hive") WITH DATA;
CREATE TABLE "tpch_10gb"."region" AS (SELECT * FROM "tpch_10gb_text"."region"@"hive") WITH DATA;
CREATE TABLE "tpch_10gb"."supplier" AS (SELECT * FROM "tpch_10gb_text"."supplier"@"hive") WITH DATA;

CREATE TABLE "tpch_100gb"."customer" AS (SELECT * FROM "tpch_100gb_text"."customer"@"hive") WITH DATA;
CREATE TABLE "tpch_100gb"."lineitem" AS (SELECT * FROM "tpch_100gb_text"."lineitem"@"hive") WITH DATA;
CREATE TABLE "tpch_100gb"."nation" AS (SELECT * FROM "tpch_100gb_text"."nation"@"hive") WITH DATA;
CREATE TABLE "tpch_100gb"."orders" AS (SELECT * FROM "tpch_100gb_text"."orders"@"hive") WITH DATA;
CREATE TABLE "tpch_100gb"."part" AS (SELECT * FROM "tpch_100gb_text"."part"@"hive") WITH DATA;
CREATE TABLE "tpch_100gb"."partsupp" AS (SELECT * FROM "tpch_100gb_text"."partsupp"@"hive") WITH DATA;
CREATE TABLE "tpch_100gb"."region" AS (SELECT * FROM "tpch_100gb_text"."region"@"hive") WITH DATA;
CREATE TABLE "tpch_100gb"."supplier" AS (SELECT * FROM "tpch_100gb_text"."supplier"@"hive") WITH DATA;

CREATE TABLE "tpch_1tb"."customer" AS (SELECT * FROM "tpch_1tb_text"."customer"@"hive") WITH DATA;
CREATE TABLE "tpch_1tb"."lineitem" AS (SELECT * FROM "tpch_1tb_text"."lineitem"@"hive") WITH DATA;
CREATE TABLE "tpch_1tb"."nation" AS (SELECT * FROM "tpch_1tb_text"."nation"@"hive") WITH DATA;
CREATE TABLE "tpch_1tb"."orders" AS (SELECT * FROM "tpch_1tb_text"."orders"@"hive") WITH DATA;
CREATE TABLE "tpch_1tb"."part" AS (SELECT * FROM "tpch_1tb_text"."part"@"hive") WITH DATA;
CREATE TABLE "tpch_1tb"."partsupp" AS (SELECT * FROM "tpch_1tb_text"."partsupp"@"hive") WITH DATA;
CREATE TABLE "tpch_1tb"."region" AS (SELECT * FROM "tpch_1tb_text"."region"@"hive") WITH DATA;
CREATE TABLE "tpch_1tb"."supplier" AS (SELECT * FROM "tpch_1tb_text"."supplier"@"hive") WITH DATA;
