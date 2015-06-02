/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.utils;

import com.teradata.benchmark.driver.sql.QueryExecution;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionUtils
{

    public static String stackTraceToString(QueryExecution queryExecution)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        queryExecution.getFailureCause().printStackTrace(pw);
        return sw.toString();
    }

    private ExceptionUtils()
    {
    }
}
