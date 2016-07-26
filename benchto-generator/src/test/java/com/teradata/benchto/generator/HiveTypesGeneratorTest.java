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

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.serde2.SerDe;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mrunit.mapreduce.MapDriver;
import org.apache.hadoop.mrunit.types.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.hadoop.mapreduce.MRJobConfig.NUM_MAPS;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class HiveTypesGeneratorTest
{

    private static final String[] FORMATS = {"text"}; // mrunit does not work with "orc"
    private static final String[] TYPES = {"bigint", "int", "boolean", "double", "binary", "date", "timestamp", "string", "decimal(38,8)", "varchar(255)"};

    @Parameters
    public static Collection<Object[]> data()
    {
        List<Object[]> parameters = new ArrayList<>(FORMATS.length * TYPES.length);
        for (String format : FORMATS) {
            for (String type : TYPES) {
                parameters.add(new String[] {format, type});
            }
        }
        return parameters;
    }

    @Parameter(0)
    public String format;

    @Parameter(1)
    public String type;

    private SerDe serDe;
    private StructObjectInspector objectInspector;
    private HiveObjectsGenerator hiveObjectsGenerator;

    @Test
    public void testMapper()
            throws Exception
    {
        Configuration serializationConfiguration = new Configuration();
        MapDriver mapDriver = MapDriver.newMapDriver(new HiveTypesGenerator.HiveTypesMapper())
                .withInput(new LongWritable(0L), NullWritable.get())
                .withInput(new LongWritable(1L), NullWritable.get())
                .withInput(new LongWritable(2L), NullWritable.get())
                .withInput(new LongWritable(3L), NullWritable.get())
                .withInput(new LongWritable(4L), NullWritable.get())
                .withOutputFormat(HiveTypesGenerator.getOutputFormatClass(format), HiveTypesGenerator.getInputFormatClass(format))
                .withOutputSerializationConfiguration(serializationConfiguration);

        mapDriver.getConfiguration().set(HiveTypesGenerator.FORMAT_PROPERTY_NAME, format);
        mapDriver.getConfiguration().set(HiveTypesGenerator.HIVE_TYPE_PROPERTY_NAME, type);
        mapDriver.getConfiguration().setLong(HiveTypesGenerator.NUM_ROWS_PROPERTY_NAME, 5L);
        mapDriver.getConfiguration().setInt(NUM_MAPS, 1);

        List<Pair<NullWritable, Writable>> output = mapDriver.run();

        extractMapperProperties(mapDriver);

        assertEquals(expectedSerializedRow(0), output.get(0).getSecond());
        assertEquals(expectedSerializedRow(1), output.get(1).getSecond());
        assertEquals(expectedSerializedRow(2), output.get(2).getSecond());
        assertEquals(expectedSerializedRow(3), output.get(3).getSecond());
        assertEquals(expectedSerializedRow(4), output.get(4).getSecond());
    }

    private void extractMapperProperties(MapDriver mapDriver)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        HiveTypesGenerator.HiveTypesMapper mapper = (HiveTypesGenerator.HiveTypesMapper) mapDriver.getMapper();
        serDe = (SerDe) FieldUtils.readDeclaredField(mapper, "serDe", true);
        objectInspector = (StructObjectInspector) FieldUtils.readDeclaredField(mapper, "objectInspector", true);
        hiveObjectsGenerator = (HiveObjectsGenerator) FieldUtils.readDeclaredField(mapper, "hiveObjectsGenerator", true);
    }

    private Writable expectedSerializedRow(long rowIndex)
            throws SerDeException
    {
        List<Object> struct = new ArrayList<>(1);
        struct.add(0, hiveObjectsGenerator.getNext((int) rowIndex));
        return serDe.serialize(struct, objectInspector);
    }
}
