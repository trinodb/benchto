/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.utils;

import com.teradata.benchmark.driver.domain.QueryExecutionResult;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionUtils
{

    public static String stackTraceToString(QueryExecutionResult queryExecutionResult)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        queryExecutionResult.getFailureCause().printStackTrace(pw);
        return sw.toString();
    }

    private ExceptionUtils()
    {
    }
}
