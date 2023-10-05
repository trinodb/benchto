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
package io.trino.benchto.driver.listeners.profiler.perf;

import io.trino.benchto.driver.listeners.profiler.QueryProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.profiler.perf", value = "enabled", havingValue = "true")
public class PerfProfiler
        implements QueryProfiler
{
    private static final Logger LOG = LoggerFactory.getLogger(PerfProfiler.class);

    @Autowired
    PerfProfilerProperties profilerProperties;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    @Retryable(value = IOException.class, backoff = @Backoff(200), maxAttempts = 2)
    public void start(String workerName, String benchmarkName, String queryName, int sequenceId)
    {
        runPerf(workerName, profilerProperties.getShell2httpPort(), benchmarkName, queryName, sequenceId);
    }

    @Override
    @Retryable(value = IOException.class, backoff = @Backoff(200), maxAttempts = 2)
    public void stop(String workerName, String benchmarkName, String queryName, int sequenceId)
    {
        LOG.info("Sending SIGTERM at %s side to perf".formatted(workerName));
        stopPerf(workerName, profilerProperties.getShell2httpPort());
    }

    private void runPerf(String nodeName, int port, String benchmarkName, String queryName, int sequenceId)
    {
        String stdoutFile = Path.of(profilerProperties.getOutputPath().toString(), benchmarkName, "%s_%d_perf_stdout.txt".formatted(queryName, sequenceId)).toString();
        URI uri = UriComponentsBuilder
                .fromUriString("http://%s".formatted(nodeName))
                .port(port)
                .queryParam("stdout", URLEncoder.encode(stdoutFile, StandardCharsets.UTF_8))
                .path("/start-perf")
                .build()
                .toUri();
        restTemplate.getForObject(uri, Object.class);
    }

    private void stopPerf(String nodeName, int port)
    {
        URI uri = UriComponentsBuilder
                .fromUriString("http://%s".formatted(nodeName))
                .port(port)
                .path("/stop-perf")
                .build()
                .toUri();
        restTemplate.getForObject(uri, Object.class);
    }

    @Override
    public String toString()
    {
        return "perf";
    }
}
