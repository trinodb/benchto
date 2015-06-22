/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.domain;

import com.teradata.benchmark.driver.utils.TimeUtils;

import java.time.Duration;
import java.time.ZonedDateTime;

import static com.google.common.base.Preconditions.checkState;
import static java.time.temporal.ChronoUnit.NANOS;

public abstract class Measurable
{
    protected long start, end;
    protected ZonedDateTime utcStart, utcEnd;

    public ZonedDateTime getUtcStart()
    {
        return utcStart;
    }

    public ZonedDateTime getUtcEnd()
    {
        return utcEnd;
    }

    public Duration getQueryDuration()
    {
        return Duration.of(end - start, NANOS);
    }

    public abstract Benchmark getBenchmark();

    @Override
    public String toString()
    {
        throw new UnsupportedOperationException("Expected to be implemented by inheriting class");
    }

    public static abstract class MeasuredBuilder<T extends Measurable, B extends MeasuredBuilder>
    {
        protected final T object;

        public MeasuredBuilder(T object)
        {
            this.object = object;
        }

        public B startTimer()
        {
            object.start = System.nanoTime();
            object.utcStart = TimeUtils.nowUtc();
            return (B) this;
        }

        public B endTimer()
        {
            checkState(object.start > 0, "Expected startTimer() to be called before.");
            object.end = System.nanoTime();
            object.utcEnd = TimeUtils.nowUtc();
            return (B) this;
        }

        public T build()
        {
            return object;
        }
    }
}
