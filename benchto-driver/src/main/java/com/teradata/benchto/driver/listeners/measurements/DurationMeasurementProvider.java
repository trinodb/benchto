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
package com.teradata.benchto.driver.listeners.measurements;

import com.google.common.collect.ImmutableList;
import com.teradata.benchto.driver.Measurable;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.execution.QueryExecutionResult;
import com.teradata.benchto.driver.service.Measurement;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.teradata.benchto.driver.service.Measurement.measurement;

@Component
public class DurationMeasurementProvider
        implements PostExecutionMeasurementProvider
{
    @Override
    public List<Measurement> loadMeasurements(Measurable measurable)
    {
        if (shouldMeasureDuration(measurable)) {
            return ImmutableList.of(measurement("duration", "MILLISECONDS", measurable.getQueryDuration().toMillis()));
        }
        else {
            return ImmutableList.of();
        }
    }

    private boolean shouldMeasureDuration(Measurable measurable)
    {
        if (measurable instanceof QueryExecutionResult) {
            return true;
        }
        else if (measurable instanceof BenchmarkExecutionResult && measurable.getBenchmark().isConcurrent()) {
            return true;
        }
        return false;
    }
}
