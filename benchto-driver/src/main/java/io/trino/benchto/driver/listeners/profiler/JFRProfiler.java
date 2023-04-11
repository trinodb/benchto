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
package io.trino.benchto.driver.listeners.profiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.io.IOException;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.profiler", value = "tool", havingValue = "jfr")
public class JFRProfiler
        implements QueryProfiler
{
    private static final Logger LOG = LoggerFactory.getLogger(JFRProfiler.class);

    @Autowired
    ProfilerProperties profilerProperties;
    private static final String[] commandSignature = new String[] {"[Ljava.lang.String;"};

    @Override
    @Retryable(value = IOException.class, backoff = @Backoff(200), maxAttempts = 2)
    public void start(String workerName, String benchmarkName, String queryName, int sequenceId)
    {
        String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", workerName, profilerProperties.getJmx().getPort());
        try (JMXConnector jmxConnector = JMXConnectorFactory.connect(new JMXServiceURL(url), null)) {
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            String sessionName = createProfilingSessionName(benchmarkName, queryName, sequenceId);

            String jfrRecodingFile = Path.of(profilerProperties.getOutputPath().toString(), benchmarkName, "%s_%d.jfr".formatted(queryName, sequenceId)).toString();
            Object[] args = new Object[] {
                    new String[] {
                            "dumponexit=true",
                            "filename=%s".formatted(jfrRecodingFile),
                            "name=%s".formatted(sessionName)
                    }
            };
            LOG.info("Starting recording JFR profile for query=%s, sequenceId=%d, jfr=%s at side %s".formatted(queryName, sequenceId, jfrRecodingFile, workerName));
            Object result = mBeanServerConnection.invoke(
                    new ObjectName("com.sun.management:type=DiagnosticCommand"),
                    "jfrStart", args,
                    commandSignature);
            LOG.info("Result of starting is: '%s' at %s side".formatted(result, workerName));
        }
        catch (Exception e) {
            LOG.error("Starting JFR profiler for worker failed at %s side".formatted(workerName), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @Retryable(value = IOException.class, backoff = @Backoff(200), maxAttempts = 2)
    public void stop(String workerName, String benchmarkName, String queryName, int sequenceId)
    {
        String sessionName = createProfilingSessionName(benchmarkName, queryName, sequenceId);
        String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", workerName, profilerProperties.getJmx().getPort());

        try (JMXConnector jmxConnector = JMXConnectorFactory.connect(new JMXServiceURL(url), null)) {
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            Object[] args = new Object[] {
                    new String[] {
                            "name=%s".formatted(sessionName)
                    }
            };

            LOG.info("Stopping recording JFR profile for session %s at %s side".formatted(sessionName, workerName));
            Object result = mBeanServerConnection.invoke(new ObjectName("com.sun.management:type=DiagnosticCommand"), "jfrStop", args, commandSignature);
            LOG.info("Result of stopping is: '%s' at %s side".formatted(result, workerName));
        }
        catch (Exception e) {
            LOG.error("Stopping JFR profiler for worker %s failed at %s side".formatted(workerName, workerName), e);
            throw new RuntimeException(e);
        }
    }

    private String createProfilingSessionName(String benchmarkName, String queryName, int sequenceId)
    {
        return "%s__%s_%d".formatted(benchmarkName, queryName, sequenceId);
    }
}
