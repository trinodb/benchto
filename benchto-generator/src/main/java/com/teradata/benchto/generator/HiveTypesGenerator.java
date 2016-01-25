/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.generator;

import com.google.common.base.Optional;
import com.teradata.benchto.generator.HiveObjectsGenerator.HiveObjectsGeneratorBuilder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
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
    public static final String REGEX_PATTERN = "mapreduce.hive-types-generator.regex-pattern";
    public static final String REGEX_MIN_LENGTH = "mapreduce.hive-types-generator.regex-min-length";
    public static final String REGEX_MAX_LENGTH = "mapreduce.hive-types-generator.regex-max-length";

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
        private int REGEX_CARDINALITY = 1000;

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
                struct.set(0, hiveObjectsGenerator.getNext(row.get()));
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
            Configuration configuration = context.getConfiguration();
            String format = configuration.get(FORMAT_PROPERTY_NAME);
            String hiveType = configuration.get(HIVE_TYPE_PROPERTY_NAME);

            try {
                List<String> columns = singletonList("value");
                List<TypeInfo> columnTypes = getTypeInfosFromTypeString(hiveType);
                serDe = getSerDeForFormat(format);

                Properties tableProperties = new Properties();
                tableProperties.setProperty("columns", columns.get(0));
                tableProperties.setProperty("columns.types", columnTypes.get(0).getTypeName());

                serDe.initialize(configuration, tableProperties);

                LOG.info("Initialized SerDe (" + serDe.getClass() + ") with properties: " + tableProperties);

                TypeInfo rowTypeInfo = getStructTypeInfo(columns, columnTypes);
                objectInspector = (StructObjectInspector) getStandardJavaObjectInspectorFromTypeInfo(rowTypeInfo);

                HiveObjectsGeneratorBuilder builder = new HiveObjectsGeneratorBuilder()
                        .withCardinality(DEFAULT_CARDINALITY)
                        .withType(hiveType);

                if (configuration.get(REGEX_PATTERN) != null) {
                    builder.withStringProducer(new RegexMatchingStringProducer(
                            configuration.get(REGEX_PATTERN),
                            configuration.getInt(REGEX_MIN_LENGTH, 0),
                            configuration.getInt(REGEX_MAX_LENGTH, 0)
                    )).withCardinality(REGEX_CARDINALITY);
                }

                hiveObjectsGenerator = builder.build();
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
        Options options = new Options();
        options.addOption(Option.builder("format")
                .required()
                .hasArg()
                .desc("file format (orc, parquet or text)")
                .build());
        options.addOption(Option.builder("type")
                .required()
                .hasArg()
                .desc("hive type to be generated (bigint, int, boolean, double, binary, date, timestamp, string, decimal or varchar)")
                .build());
        options.addOption(Option.builder("rows")
                .required()
                .hasArg()
                .desc("total row count")
                .build());
        options.addOption(Option.builder("mappers")
                .required()
                .hasArg()
                .desc("total mappers count")
                .build());
        options.addOption(Option.builder("path")
                .hasArg()
                .desc("base path for generating files, default is: /benchmarks/benchto/types")
                .build());
        options.addOption(Option.builder("regex")
                .numberOfArgs(3)
                .desc("generate varchars from regex pattern, arguments are: pattern, min length, max length")
                .build());

        CommandLine line;
        String format;
        String hiveType;
        long numberOfRows;
        long numberOfFiles;
        String basePath;
        Optional<String> regexPattern = Optional.absent();
        Optional<Integer> regexMinLength = Optional.absent();
        Optional<Integer> regexMaxLength = Optional.absent();
        try {
            line = new DefaultParser().parse(options, args);
            format = line.getOptionValue("format");
            hiveType = line.getOptionValue("type");
            numberOfRows = parseLong(line.getOptionValue("rows"));
            numberOfFiles = parseLong(line.getOptionValue("mappers"));
            basePath = line.getOptionValue("path", "/benchmarks/benchto/types");
            if (line.hasOption("regex")) {
                String[] values = line.getOptionValues("regex");
                regexPattern = Optional.of(values[0]);
                regexMinLength = Optional.of(parseInt(values[1]));
                regexMaxLength = Optional.of(parseInt(values[2]));
            }
        }
        catch (Exception e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("benchto-generator", options);
            throw e;
        }

        String jobName = format("GenerateData-%s-%s-%d", format, hiveType, numberOfRows);
        Path outputDir = new Path(format("%s/%s-%s/%d", basePath, format, hiveType, numberOfRows));
        Class<? extends OutputFormat> outputFormatClass = getOutputFormatClass(format);

        LOG.info("Generating " + numberOfRows + " " + hiveType + "s, directory: " + outputDir + ", number of files: " + numberOfFiles);

        Configuration configuration = new Configuration();
        configuration.set(FORMAT_PROPERTY_NAME, format);
        configuration.set(HIVE_TYPE_PROPERTY_NAME, hiveType);
        configuration.setLong(NUM_ROWS_PROPERTY_NAME, numberOfRows);
        configuration.setLong(NUM_MAPS, numberOfFiles);
        if (regexPattern.isPresent()) {
            configuration.set(REGEX_PATTERN, regexPattern.get());
            configuration.setInt(REGEX_MIN_LENGTH, regexMinLength.get());
            configuration.setInt(REGEX_MAX_LENGTH, regexMaxLength.get());
        }

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
