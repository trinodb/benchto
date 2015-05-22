/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class BenchmarkProperties
        implements InitializingBean
{

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSS");

    @Value("${runs:3}")
    private int runs;

    @Value("${sql:sql}")
    private String sqlDir;

    @Value("${executionSequenceId:}")
    private String executionSequenceId;

    @Override
    public void afterPropertiesSet()
            throws Exception
    {
        if (Strings.isNullOrEmpty(executionSequenceId)) {
            executionSequenceId = LocalDateTime.now(ZoneId.of("UTC")).format(DATE_TIME_FORMATTER);
        }
    }

    public int getRuns()
    {
        return runs;
    }

    public String getSqlDir()
    {
        return sqlDir;
    }

    public String getExecutionSequenceId()
    {
        return executionSequenceId;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("runs", runs)
                .add("sqlDir", sqlDir)
                .add("executionSequenceId", executionSequenceId)
                .toString();
    }
}
