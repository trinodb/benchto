CREATE SCHEMA IF NOT EXISTS memory.insert_test;
CREATE TABLE IF NOT EXISTS memory.insert_test.nation AS SELECT * from tpch.tiny.nation;
