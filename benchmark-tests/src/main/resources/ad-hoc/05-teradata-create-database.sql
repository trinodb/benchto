-- must be run from "bteq .logon localhost/dbc,dbc"

CREATE DATABASE tpch_10gb AS PERMANENT = 1e11, SPOOL = 1e11;
GRANT CREATE TABLE ON tpch_10gb TO test_presto_user;

CREATE DATABASE tpch_100gb AS PERMANENT = 1e12, SPOOL = 1e12;
GRANT CREATE TABLE ON tpch_100gb TO test_presto_user;


CREATE DATABASE tpch_1tb AS PERMANENT = 1e13, SPOOL = 1e13;
GRANT CREATE TABLE ON tpch_1tb TO test_presto_user;
