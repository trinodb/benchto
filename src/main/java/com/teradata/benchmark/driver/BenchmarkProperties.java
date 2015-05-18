/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.google.common.base.MoreObjects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BenchmarkProperties
{

    @Value("${runs:3}")
    private int runs;

    @Value("${sql:sql}")
    private String sqlDir;

    public int getRuns()
    {
        return runs;
    }

    public String getSqlDir()
    {
        return sqlDir;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("runs", runs)
                .add("sqlDir", sqlDir)
                .toString();
    }
}
