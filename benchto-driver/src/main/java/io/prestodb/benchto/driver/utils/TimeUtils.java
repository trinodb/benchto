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
package io.prestodb.benchto.driver.utils;

import io.prestodb.benchto.driver.BenchmarkExecutionException;

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
