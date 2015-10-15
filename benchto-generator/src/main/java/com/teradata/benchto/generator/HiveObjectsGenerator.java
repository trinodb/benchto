/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.generator;

import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.common.type.HiveVarchar;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Random;
import java.util.UUID;

class HiveObjectsGenerator
{

    private final Object[] values;

    private HiveObjectsGenerator(Object[] values)
    {
        this.values = values;
    }

    public Object getNext(long index)
    {
        return values[(int) (index % values.length)];
    }

    public static class HiveObjectsGeneratorBuilder
    {

        private Random random = new Random(1410L);
        private int cardinality;
        private String hiveType;
        private ObjectProducer<HiveVarchar> hiveVarcharProducer = new ObjectProducer<HiveVarchar>()
        {
            @Override
            public HiveVarchar generateNext()
            {
                return new HiveVarchar(UUID.randomUUID().toString(), -1);
            }
        };

        public HiveObjectsGeneratorBuilder withType(String type)
        {
            this.hiveType = type;
            return this;
        }

        public HiveObjectsGeneratorBuilder withCardinality(int cardinality)
        {
            this.cardinality = cardinality;
            return this;
        }

        public HiveObjectsGeneratorBuilder withStringProducer(final ObjectProducer<String> stringProducer)
        {
            hiveVarcharProducer = new ObjectProducer<HiveVarchar>()
            {
                @Override
                public HiveVarchar generateNext()
                {
                    return new HiveVarchar(stringProducer.generateNext(), -1);
                }
            };
            return this;
        }

        public HiveObjectsGenerator build()
        {
            Object[] values;
            if ("bigint".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer()
                {
                    @Override
                    public Object generateNext()
                    {
                        return random.nextLong();
                    }
                });
            }
            else if ("int".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer()
                {
                    @Override
                    public Object generateNext()
                    {
                        return random.nextInt();
                    }
                });
            }
            else if ("boolean".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer()
                {
                    @Override
                    public Object generateNext()
                    {
                        return random.nextBoolean();
                    }
                });
            }
            else if ("double".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer()
                {
                    @Override
                    public Object generateNext()
                    {
                        return randomDouble();
                    }
                });
            }
            else if ("binary".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer()
                {
                    @Override
                    public Object generateNext()
                    {
                        return UUID.randomUUID().toString().getBytes();
                    }
                });
            }
            else if ("date".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer()
                {
                    @Override
                    public Object generateNext()
                    {
                        return new Date(randomTimestampMillis());
                    }
                });
            }
            else if ("timestamp".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer()
                {
                    @Override
                    public Object generateNext()
                    {
                        return new Timestamp(randomTimestampMillis());
                    }
                });
            }
            else if (hiveType.startsWith("decimal")) {
                values = generateRandomArray(new ObjectProducer()
                {
                    @Override
                    public Object generateNext()
                    {
                        return HiveDecimal.create(new BigDecimal(randomDouble(), MathContext.DECIMAL128));
                    }
                });
            }
            else if (hiveType.startsWith("varchar")) {
                values = generateRandomArray(hiveVarcharProducer);
            }
            else {
                throw new IllegalArgumentException("Unsupported type " + hiveType);
            }
            return new HiveObjectsGenerator(values);
        }

        private Object[] generateRandomArray(ObjectProducer objectProducer)
        {
            Object[] values = new Object[cardinality];
            for (int i = 0; i < values.length; ++i) {
                values[i] = objectProducer.generateNext();
            }
            return values;
        }

        private Double randomDouble()
        {
            double lower = -1000000.00;
            double upper = 1000000.00;
            return random.nextDouble() * (upper - lower) + lower;
        }

        private long randomTimestampMillis()
        {
            return (long) random.nextInt(1581724800) * 1000L; // 1970-2020
        }
    }
}
