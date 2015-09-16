#!/bin/sh

# To generate data for 1/10/100m rows, copy benchto-generator-1.0.0-SNAPSHOT.jar
# to cluster and run following commands from hdfs user. It can take long time to
# generate it so it is recommended to use nohup:
#
# nohup ./10-generate-hive-type-data.sh 1000000000 150 &  # 1B rows, 150 mappers
# nohup ./10-generate-hive-type-data.sh 10000000000 200 & # 10B rows, 200 mappers

GENERATOR_HDFS_FILES_PATH=/benchmarks/benchto/types
GENERATOR_JAR_FILE=benchto-generator-1.0.0-SNAPSHOT.jar
DATASET_SIZE=$1
MAPPERS_COUNT=$2

for format in 'text' 'orc'; do
    for type in 'bigint' 'int' 'double' 'decimal(38,8)' 'decimal(38,19)' 'decimal(12,4)' 'boolean' 'varchar(255)' 'date' 'timestamp' 'binary'; do
        hadoop jar ${GENERATOR_JAR_FILE} ${format} ${type} ${DATASET_SIZE} ${MAPPERS_COUNT} &
    done
done
