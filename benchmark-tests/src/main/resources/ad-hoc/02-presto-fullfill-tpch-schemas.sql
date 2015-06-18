-- try to drop all the tables first

-- orc
DROP TABLE "hive"."tpch_100gb_orc"."customer";
DROP TABLE "hive"."tpch_100gb_orc"."lineitem";
DROP TABLE "hive"."tpch_100gb_orc"."nation";
DROP TABLE "hive"."tpch_100gb_orc"."orders";
DROP TABLE "hive"."tpch_100gb_orc"."part";
DROP TABLE "hive"."tpch_100gb_orc"."partsupp";
DROP TABLE "hive"."tpch_100gb_orc"."region";
DROP TABLE "hive"."tpch_100gb_orc"."supplier";

-- text

DROP TABLE "hive"."tpch_100gb_text"."customer";
DROP TABLE "hive"."tpch_100gb_text"."lineitem";
DROP TABLE "hive"."tpch_100gb_text"."nation";
DROP TABLE "hive"."tpch_100gb_text"."orders";
DROP TABLE "hive"."tpch_100gb_text"."part";
DROP TABLE "hive"."tpch_100gb_text"."partsupp";
DROP TABLE "hive"."tpch_100gb_text"."region";
DROP TABLE "hive"."tpch_100gb_text"."supplier";


-- then let's fullfill schemas with tables

-- orc
SET SESSION hive.storage_format='ORC';
CREATE TABLE "hive"."tpch_100gb_orc"."customer" AS SELECT * FROM "tpch"."sf100"."customer";
CREATE TABLE "hive"."tpch_100gb_orc"."lineitem" AS SELECT * FROM "tpch"."sf100"."lineitem";
CREATE TABLE "hive"."tpch_100gb_orc"."nation" AS SELECT * FROM "tpch"."sf100"."nation";
CREATE TABLE "hive"."tpch_100gb_orc"."orders" AS SELECT * FROM "tpch"."sf100"."orders";
CREATE TABLE "hive"."tpch_100gb_orc"."part" AS SELECT * FROM "tpch"."sf100"."part";
CREATE TABLE "hive"."tpch_100gb_orc"."partsupp" AS SELECT * FROM "tpch"."sf100"."partsupp";
CREATE TABLE "hive"."tpch_100gb_orc"."region" AS SELECT * FROM "tpch"."sf100"."region";
CREATE TABLE "hive"."tpch_100gb_orc"."supplier" AS SELECT * FROM "tpch"."sf100"."supplier";


-- text
-- lets create tables in text format from already generated orc, it is much more faster than generate it once more
SET SESSION hive.storage_format='TEXTFILE';
CREATE TABLE "hive"."tpch_100gb_text"."customer" AS SELECT * FROM "hive"."tpch_100gb_orc"."customer";
CREATE TABLE "hive"."tpch_100gb_text"."lineitem" AS SELECT * FROM "hive"."tpch_100gb_orc"."lineitem";
CREATE TABLE "hive"."tpch_100gb_text"."nation" AS SELECT * FROM "hive"."tpch_100gb_orc"."nation";
CREATE TABLE "hive"."tpch_100gb_text"."orders" AS SELECT * FROM "hive"."tpch_100gb_orc"."orders";
CREATE TABLE "hive"."tpch_100gb_text"."part" AS SELECT * FROM "hive"."tpch_100gb_orc"."part";
CREATE TABLE "hive"."tpch_100gb_text"."partsupp" AS SELECT * FROM "hive"."tpch_100gb_orc"."partsupp";
CREATE TABLE "hive"."tpch_100gb_text"."region" AS SELECT * FROM "hive"."tpch_100gb_orc"."region";
CREATE TABLE "hive"."tpch_100gb_text"."supplier" AS SELECT * FROM "hive"."tpch_100gb_orc"."supplier";