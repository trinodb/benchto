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
package io.trino.benchto.service;

import com.google.common.collect.ImmutableMap;
import io.trino.benchto.service.category.IntegrationTest;
import io.trino.benchto.service.model.BenchmarkRun;
import io.trino.benchto.service.model.Environment;
import io.trino.benchto.service.model.Status;
import io.trino.benchto.service.repo.BenchmarkRunRepo;
import io.trino.benchto.service.repo.EnvironmentRepo;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;

import static io.trino.benchto.service.utils.TimeUtils.currentDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@Category(IntegrationTest.class)
public class CleanerServiceTest
        extends IntegrationTestBase
{
    private static final String UNIQUE_NAME = "unique name";
    private static final String SEQUENCE_ID = "sequencId";

    @Autowired
    private BenchmarkRunRepo benchmarkRunRepo;

    @Autowired
    private CleanerService cleanerService;

    @Autowired
    private EnvironmentRepo environmentRepo;

    @Test
    public void cleanUpStaleBenchmarks()
    {
        Environment environment = new Environment();
        environmentRepo.save(environment);

        ZonedDateTime currentDate = currentDateTime();
        BenchmarkRun staleBenchmark = new BenchmarkRun("stale benchmark test", SEQUENCE_ID, ImmutableMap.of(), UNIQUE_NAME);
        staleBenchmark.setStatus(Status.STARTED);
        staleBenchmark.setStarted(currentDate.minusHours(CleanerService.BENCHMARK_TIMEOUT_HOURS).minusMinutes(1));
        staleBenchmark.setEnvironment(environment);
        benchmarkRunRepo.save(staleBenchmark);

        cleanerService.cleanUpStaleBenchmarks();

        BenchmarkRun benchmarkRun = benchmarkRunRepo.findByUniqueNameAndSequenceId(UNIQUE_NAME, SEQUENCE_ID);
        assertThat(benchmarkRun.getStatus()).isEqualTo(Status.FAILED);
        assertThat(benchmarkRun.getEnded()).isAfter(currentDate);
    }
}
