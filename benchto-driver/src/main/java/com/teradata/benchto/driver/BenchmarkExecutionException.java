/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver;

public class BenchmarkExecutionException
        extends RuntimeException
{

    public BenchmarkExecutionException(String message)
    {
        super(message);
    }

    public BenchmarkExecutionException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public BenchmarkExecutionException(Throwable cause)
    {
        super(cause);
    }
}
