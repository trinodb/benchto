/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teradata.benchto.generator;

import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.common.type.HiveVarchar;
import org.apache.hadoop.io.Text;

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
        private ObjectProducer<String> stringProducer = new ObjectProducer<String>()
        {
            @Override
            public String generateNext()
            {
                return UUID.randomUUID().toString();
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
            this.stringProducer = stringProducer;
            return this;
        }

        public HiveObjectsGenerator build()
        {
            Object[] values;
            if ("bigint".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer<Long>()
                {
                    @Override
                    public Long generateNext()
                    {
                        return random.nextLong();
                    }
                });
            }
            else if ("int".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer<Integer>()
                {
                    @Override
                    public Integer generateNext()
                    {
                        return random.nextInt();
                    }
                });
            }
            else if ("boolean".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer<Boolean>()
                {
                    @Override
                    public Boolean generateNext()
                    {
                        return random.nextBoolean();
                    }
                });
            }
            else if ("double".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer<Double>()
                {
                    @Override
                    public Double generateNext()
                    {
                        return randomDouble();
                    }
                });
            }
            else if ("binary".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer<byte[]>()
                {
                    @Override
                    public byte[] generateNext()
                    {
                        return UUID.randomUUID().toString().getBytes();
                    }
                });
            }
            else if ("date".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer<Date>()
                {
                    @Override
                    public Date generateNext()
                    {
                        return new Date(randomTimestampMillis());
                    }
                });
            }
            else if ("timestamp".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer<Timestamp>()
                {
                    @Override
                    public Timestamp generateNext()
                    {
                        return new Timestamp(randomTimestampMillis());
                    }
                });
            }
            else if ("string".equals(hiveType)) {
                values = generateRandomArray(new ObjectProducer()
                {
                    @Override
                    public Object generateNext()
                    {
                        return new Text(stringProducer.generateNext());
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
                values = generateRandomArray(new ObjectProducer<HiveVarchar>()
                {
                    @Override
                    public HiveVarchar generateNext()
                    {
                        return new HiveVarchar(stringProducer.generateNext(), -1);
                    }
                });
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
