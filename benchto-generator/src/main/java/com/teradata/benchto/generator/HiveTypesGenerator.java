/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.generator;

import com.teradata.benchto.generator.HiveObjectsGenerator.HiveObjectsGeneratorBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.io.orc.OrcNewInputFormat;
import org.apache.hadoop.hive.ql.io.orc.OrcNewOutputFormat;
import org.apache.hadoop.hive.ql.io.orc.OrcSerde;
import org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe;
import org.apache.hadoop.hive.ql.io.parquet.write.DataWritableWriteSupport;
import org.apache.hadoop.hive.serde2.SerDe;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import parquet.hadoop.ParquetOutputFormat;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.facebook.presto.hive.$internal.com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.hadoop.hive.serde2.typeinfo.TypeInfoFactory.getStructTypeInfo;
import static org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils.getStandardJavaObjectInspectorFromTypeInfo;
import static org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils.getTypeInfosFromTypeString;
import static org.apache.hadoop.mapreduce.MRJobConfig.NUM_MAPS;

public class HiveTypesGenerator
        extends Configured
        implements Tool
{
    private static final Logger LOG = Logger.getLogger(HiveTypesGenerator.class);

    public static final String FORMAT_PROPERTY_NAME = "mapreduce.hive-types-generator.format";
    public static final String HIVE_TYPE_PROPERTY_NAME = "mapreduce.hive-types-generator.type";
    public static final String NUM_ROWS_PROPERTY_NAME = "mapreduce.hive-types-generator.num-rows";

    /**
     * An input format that assigns longs from 0 to rowCount to each mapper.
     */
    public static class CounterInputFormat
            extends InputFormat<LongWritable, NullWritable>
    {

        /**
         * An input split consisting of a numbers form 0 to rowCount.
         */
        static class CounterInputSplit
                extends InputSplit
                implements Writable
        {
            private long rowCount;

            public CounterInputSplit() { }

            public CounterInputSplit(long rowCount)
            {
                this.rowCount = rowCount;
            }

            public long getLength()
                    throws IOException
            {
                return 0;
            }

            public String[] getLocations()
                    throws IOException
            {
                return new String[] {};
            }

            public void readFields(DataInput in)
                    throws IOException
            {
                rowCount = WritableUtils.readVLong(in);
            }

            public void write(DataOutput out)
                    throws IOException
            {
                WritableUtils.writeVLong(out, rowCount);
            }
        }

        /**
         * A record reader which generate longs from 0 to rowCount.
         */
        static class CounterRecordReader
                extends RecordReader<LongWritable, NullWritable>
        {
            private long currentRow;
            private long rowCount;
            private LongWritable key = null;

            public void initialize(InputSplit split, TaskAttemptContext context)
                    throws IOException, InterruptedException
            {
                currentRow = 0;
                rowCount = ((CounterInputSplit) split).rowCount;
            }

            public void close()
                    throws IOException
            {
                // NOTHING
            }

            public LongWritable getCurrentKey()
            {
                return key;
            }

            public NullWritable getCurrentValue()
            {
                return NullWritable.get();
            }

            public float getProgress()
                    throws IOException
            {
                return currentRow / (float) rowCount;
            }

            public boolean nextKeyValue()
            {
                if (key == null) {
                    key = new LongWritable();
                }
                if (currentRow < rowCount) {
                    key.set(currentRow);
                    currentRow += 1;
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        public RecordReader<LongWritable, NullWritable> createRecordReader(InputSplit split, TaskAttemptContext context)
                throws IOException
        {
            return new CounterRecordReader();
        }

        public List<InputSplit> getSplits(JobContext job)
        {
            long totalRows = job.getConfiguration().getLong(NUM_ROWS_PROPERTY_NAME, 0);
            int numSplits = job.getConfiguration().getInt(NUM_MAPS, 1);

            List<InputSplit> splits = new ArrayList<>();
            long splitAllocatedRows = 0;
            for (int split = 0; split < numSplits; ++split) {
                long splitAllocation = (long) Math.ceil(totalRows * (double) (split + 1) / numSplits);
                splits.add(new CounterInputSplit(splitAllocation - splitAllocatedRows));
                splitAllocatedRows = splitAllocation;
            }
            return splits;
        }
    }

    public static class HiveTypesMapper
            extends Mapper<LongWritable, NullWritable, NullWritable, Writable>
    {

        private int DEFAULT_CARDINALITY = 100000;

        @SuppressWarnings("deprecated")
        private SerDe serDe;
        private StructObjectInspector objectInspector;
        private HiveObjectsGenerator hiveObjectsGenerator;
        private List<Object> struct;

        public void map(LongWritable row, NullWritable ignored,
                Mapper.Context context)
                throws IOException, InterruptedException
        {
            try {
                struct.set(0, hiveObjectsGenerator.getNext((int) row.get()));
                Writable serializedRow = serDe.serialize(struct, objectInspector);
                context.write(NullWritable.get(), serializedRow);
            }
            catch (SerDeException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException
        {
            String format = context.getConfiguration().get(FORMAT_PROPERTY_NAME);
            String hiveType = context.getConfiguration().get(HIVE_TYPE_PROPERTY_NAME);

            try {
                List<String> columns = singletonList("value");
                List<TypeInfo> columnTypes = getTypeInfosFromTypeString(hiveType);
                serDe = getSerDeForFormat(format);

                Properties tableProperties = new Properties();
                tableProperties.setProperty("columns", columns.get(0));
                tableProperties.setProperty("columns.types", columnTypes.get(0).getTypeName());

                serDe.initialize(context.getConfiguration(), tableProperties);

                LOG.info("Initialized SerDe (" + serDe.getClass() + ") with properties: " + tableProperties);

                TypeInfo rowTypeInfo = getStructTypeInfo(columns, columnTypes);
                objectInspector = (StructObjectInspector) getStandardJavaObjectInspectorFromTypeInfo(rowTypeInfo);

                hiveObjectsGenerator = new HiveObjectsGeneratorBuilder()
                        .withCardinality(DEFAULT_CARDINALITY)
                        .withType(hiveType)
                        .build();

                struct = new ArrayList<>(1);
                struct.add(0, null);
            }
            catch (SerDeException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public int run(String[] args)
            throws Exception
    {
        checkArgument(args[0] != null && args[1] != null && args[2] != null && args[3] != null,
                "usage: hadoop jar benchto-generator-1.0.0-SNAPSHOT.jar FORMAT TYPE ROW_COUNT MAPPERS_COUNT");

        String format = args[0];
        String hiveType = args[1];
        long numberOfRows = Long.parseLong(args[2]);
        long numberOfFiles = Long.parseLong(args[3]);
        String jobName = format("GenerateData-%s-%s-%d", format, hiveType, numberOfRows);
        Path outputDir = new Path(format("/benchmarks/benchto/types/%s-%s/%d", format, hiveType, numberOfRows));
        Class<? extends OutputFormat> outputFormatClass = getOutputFormatClass(format);

        LOG.info("Generating " + numberOfRows + " " + hiveType + "s, directory: " + outputDir + ", number of files: " + numberOfFiles);

        Configuration configuration = new Configuration();
        configuration.set(FORMAT_PROPERTY_NAME, format);
        configuration.set(HIVE_TYPE_PROPERTY_NAME, hiveType);
        configuration.setLong(NUM_ROWS_PROPERTY_NAME, numberOfRows);
        configuration.setLong(NUM_MAPS, numberOfFiles);

        Job generatorJob = Job.getInstance(configuration, jobName);
        FileOutputFormat.setOutputPath(generatorJob, outputDir);
        ParquetOutputFormat.setWriteSupportClass(generatorJob, DataWritableWriteSupport.class);
        generatorJob.setJarByClass(HiveTypesGenerator.class);
        generatorJob.setMapperClass(HiveTypesMapper.class);
        generatorJob.setNumReduceTasks(0);
        generatorJob.setOutputKeyClass(NullWritable.class);
        generatorJob.setOutputValueClass(Writable.class);
        generatorJob.setInputFormatClass(CounterInputFormat.class);
        generatorJob.setOutputFormatClass(outputFormatClass);

        return generatorJob.waitForCompletion(true) ? 0 : 1;
    }

    @SuppressWarnings("deprecated")
    public static SerDe getSerDeForFormat(String format)
            throws SerDeException
    {
        if ("text".equals(format)) {
            return new LazySimpleSerDe();
        }
        else if ("orc".equals(format)) {
            return new OrcSerde(); // VectorizedOrcSerDe???
        }
        else if ("parquet".equals(format)) {
            return new ParquetHiveSerDe();
        }
        throw new IllegalArgumentException("Unsupported format " + format);
    }

    public static Class<? extends OutputFormat> getOutputFormatClass(String format)
    {
        if ("text".equals(format)) {
            return TextOutputFormat.class;
        }
        else if ("orc".equals(format)) {
            return OrcNewOutputFormat.class;
        }
        else if ("parquet".equals(format)) {
            // return ParquetOutputFormat.class; // not working!
        }
        throw new IllegalArgumentException("Unsupported format " + format);
    }

    public static Class<? extends InputFormat> getInputFormatClass(String format)
    {
        if ("text".equals(format)) {
            return TextInputFormat.class;
        }
        else if ("orc".equals(format)) {
            return OrcNewInputFormat.class;
        }
        else if ("parquet".equals(format)) {
            // return ParquetInputFormat.class; // not working!
        }
        throw new IllegalArgumentException("Unsupported format " + format);
    }

    public static void main(String[] args)
            throws Exception
    {
        int response = ToolRunner.run(new HiveTypesGenerator(), args);
        System.exit(response);
    }
}
