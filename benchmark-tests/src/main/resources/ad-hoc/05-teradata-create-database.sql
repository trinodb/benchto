-- must be run from "bteq .logon localhost/dbc,dbc"

CREATE DATABASE tpch_100gb AS PERMANENT = 1e13, SPOOL = 1e13;
GRANT CREATE TABLE ON tpch_100gb TO test_presto_user;
