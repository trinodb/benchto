/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchto.driver.utils;

import com.teradata.benchto.driver.BenchmarkExecutionException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public final class TimeUtils
{

    public static ZonedDateTime nowUtc()
    {
        return ZonedDateTime.now(ZoneId.of("UTC"));
    }

    public static void sleep(long timeout, TimeUnit timeUnit)
    {
        try {
            Thread.sleep(timeUnit.toMillis(timeout));
        }
        catch (InterruptedException e) {
            throw new BenchmarkExecutionException(e);
        }
    }

    private TimeUtils()
    {
    }
}
