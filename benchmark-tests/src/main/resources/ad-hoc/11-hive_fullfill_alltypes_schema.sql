CREATE DATABASE types_100m_orc;
CREATE DATABASE types_100m_text;
CREATE DATABASE types_1b_orc;
CREATE DATABASE types_1b_text;

CREATE EXTERNAL TABLE types_100m_text.boolean       (value BOOLEAN)        ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-boolean/100000000';
CREATE EXTERNAL TABLE types_100m_text.int           (value INT)            ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-int/100000000';
CREATE EXTERNAL TABLE types_100m_text.bigint        (value BIGINT)         ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-bigint/100000000';
CREATE EXTERNAL TABLE types_100m_text.double        (value DOUBLE)         ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-double/100000000';
CREATE EXTERNAL TABLE types_100m_text.varchar_255   (value VARCHAR(255))   ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-varchar(255)/100000000';
CREATE EXTERNAL TABLE types_100m_text.binary        (value BINARY)         ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-binary/100000000';
CREATE EXTERNAL TABLE types_100m_text.date          (value DATE)           ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-date/100000000';
CREATE EXTERNAL TABLE types_100m_text.timestamp     (value TIMESTAMP)      ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-timestamp/100000000';
CREATE EXTERNAL TABLE types_100m_text.decimal_38_8  (value DECIMAL(38,8))  ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-decimal(38,8)/100000000';

CREATE EXTERNAL TABLE types_100m_orc.boolean       (value BOOLEAN)        STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-boolean/100000000';
CREATE EXTERNAL TABLE types_100m_orc.int           (value INT)            STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-int/100000000';
CREATE EXTERNAL TABLE types_100m_orc.bigint        (value BIGINT)         STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-bigint/100000000';
CREATE EXTERNAL TABLE types_100m_orc.double        (value DOUBLE)         STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-double/100000000';
CREATE EXTERNAL TABLE types_100m_orc.varchar_255   (value VARCHAR(255))   STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-varchar(255)/100000000';
CREATE EXTERNAL TABLE types_100m_orc.binary        (value BINARY)         STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-binary/100000000';
CREATE EXTERNAL TABLE types_100m_orc.date          (value DATE)           STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-date/100000000';
CREATE EXTERNAL TABLE types_100m_orc.timestamp     (value TIMESTAMP)      STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-timestamp/100000000';
CREATE EXTERNAL TABLE types_100m_orc.decimal_38_8  (value DECIMAL(38,8))  STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-decimal(38,8)/100000000';

CREATE EXTERNAL TABLE types_1b_text.boolean       (value BOOLEAN)        ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-boolean/1000000000';
CREATE EXTERNAL TABLE types_1b_text.int           (value INT)            ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-int/1000000000';
CREATE EXTERNAL TABLE types_1b_text.bigint        (value BIGINT)         ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-bigint/1000000000';
CREATE EXTERNAL TABLE types_1b_text.double        (value DOUBLE)         ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-double/1000000000';
CREATE EXTERNAL TABLE types_1b_text.varchar_255   (value VARCHAR(255))   ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-varchar(255)/1000000000';
CREATE EXTERNAL TABLE types_1b_text.binary        (value BINARY)         ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-binary/1000000000';
CREATE EXTERNAL TABLE types_1b_text.date          (value DATE)           ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-date/1000000000';
CREATE EXTERNAL TABLE types_1b_text.timestamp     (value TIMESTAMP)      ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-timestamp/1000000000';
CREATE EXTERNAL TABLE types_1b_text.decimal_38_8  (value DECIMAL(38,8))  ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' STORED AS TEXTFILE LOCATION '/benchmarks/benchto/types/text-decimal(38,8)/1000000000';

CREATE EXTERNAL TABLE types_1b_orc.boolean       (value BOOLEAN)        STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-boolean/1000000000';
CREATE EXTERNAL TABLE types_1b_orc.int           (value INT)            STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-int/1000000000';
CREATE EXTERNAL TABLE types_1b_orc.bigint        (value BIGINT)         STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-bigint/1000000000';
CREATE EXTERNAL TABLE types_1b_orc.double        (value DOUBLE)         STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-double/1000000000';
CREATE EXTERNAL TABLE types_1b_orc.varchar_255   (value VARCHAR(255))   STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-varchar(255)/1000000000';
CREATE EXTERNAL TABLE types_1b_orc.binary        (value BINARY)         STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-binary/1000000000';
CREATE EXTERNAL TABLE types_1b_orc.date          (value DATE)           STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-date/1000000000';
CREATE EXTERNAL TABLE types_1b_orc.timestamp     (value TIMESTAMP)      STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-timestamp/1000000000';
CREATE EXTERNAL TABLE types_1b_orc.decimal_38_8  (value DECIMAL(38,8))  STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-decimal(38,8)/1000000000';
