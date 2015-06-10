package com.teradata.benchmark.driver.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class TimeUtils
{

    public static ZonedDateTime nowUtc()
    {
        return ZonedDateTime.now(ZoneId.of("UTC"));
    }

    private TimeUtils()
    {
    }
}
