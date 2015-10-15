# benchto-generator

Map reduce Hive types data generator. Generator creates separate hdfs files for each type. You can
configure types, format and number of files. By default data is generated under `/benchmarks/benchto/types` directory.
The directory can be changed using `-path` parameter.

Supported types:
* int, bigint, decimal(X,Y)
* double
* boolean
* varchar
* date, timestamp
* binary

Supported formats:
* text
* orc

## Data generation

Sample usage:

```
$ hdfs dfs -rm -r /benchmarks/benchto/types/
$ hadoop jar benchto-generator-1.0.0-SNAPSHOT.jar -format text -type bigint -rows 1000000 -mappers 4
$ hdfs dfs -ls -R /benchmarks/benchto/types
drwxr-xr-x   - hdfs supergroup          0 2015-09-10 13:24 /benchmarks/benchto/types/orc-bigint
drwxr-xr-x   - hdfs supergroup          0 2015-09-10 13:24 /benchmarks/benchto/types/orc-bigint/1000000
-rw-r--r--   1 hdfs supergroup          0 2015-09-10 13:24 /benchmarks/benchto/types/orc-bigint/1000000/_SUCCESS
-rw-r--r--   1 hdfs supergroup    2001740 2015-09-10 13:24 /benchmarks/benchto/types/orc-bigint/1000000/part-m-00000
-rw-r--r--   1 hdfs supergroup    2001750 2015-09-10 13:24 /benchmarks/benchto/types/orc-bigint/1000000/part-m-00001
-rw-r--r--   1 hdfs supergroup    2001736 2015-09-10 13:24 /benchmarks/benchto/types/orc-bigint/1000000/part-m-00002
-rw-r--r--   1 hdfs supergroup    2001758 2015-09-10 13:24 /benchmarks/benchto/types/orc-bigint/1000000/part-m-00003
...
```

For varchar type rows can be generated from a regular expression. Rows will be randomized strings matching 
the regular expression. This can be enabled by providing `-regex pattern min_row_length max_row_length` parameter, e.g:

```
-regex "a*" 100 120
```

## Hive schema

Once data is generated you can create Hive schema:

```
hive> CREATE DATABASE types_1m_orc;
hive> CREATE EXTERNAL TABLE types_1m_orc.bigint (value BIGINT)
      STORED AS ORC LOCATION '/benchmarks/benchto/types/orc-bigint/1000000';
```

Testing created schema:

```
hive> DESCRIBE FORMATTED types_1m_text.varchar_255;
OK
# col_name            	data_type           	comment             
	 	 
value               	varchar(255)        	                    
	 	 
# Detailed Table Information	 	 
Database:           	types_100m_text     	 
Owner:              	hdfs                	 
CreateTime:         	Thu Sep 10 12:31:02 UTC 2015	 
LastAccessTime:     	UNKNOWN             	 
Protect Mode:       	None                	 
Retention:          	0                   	 
Location:           	hdfs://hadoop-master:8020/benchmarks/benchto/types/varchar(255)/1000000	 
Table Type:         	EXTERNAL_TABLE      	 
...
hive> SELECT * FROM types_1m_text.varchar_255 LIMIT 5;
OK
40ee3140-5fcd-46f2-a909-08b91cedecd8
29daf8f6-8bad-4a9b-b209-ad3705cfd28e
fbe0cec9-8088-4464-8e93-30529955f7b4
52ac9de3-73a3-40a6-87bc-3732a6da574d
8c7f06ca-a128-4b5e-a01a-464d7549d685
Time taken: 0.121 seconds, Fetched: 5 row(s)
```
