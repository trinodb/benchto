/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.benchto.driver;

import io.trino.benchto.driver.utils.TimeUtils;

import java.time.Duration;
import java.time.ZonedDateTime;

import static com.google.common.base.Preconditions.checkState;
import static java.time.temporal.ChronoUnit.NANOS;

public abstract class Measurable
{
    protected long start;
    protected long end;
    protected ZonedDateTime utcStart;
    protected ZonedDateTime utcEnd;

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

    public abstract String getEnvironment();

    public abstract boolean isSuccessful();

    public abstract String toString();

    public abstract static class MeasuredBuilder<T extends Measurable, B extends MeasuredBuilder>
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
