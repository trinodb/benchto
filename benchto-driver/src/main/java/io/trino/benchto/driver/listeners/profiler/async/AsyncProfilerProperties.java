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
package io.trino.benchto.driver.listeners.profiler.async;

import io.trino.benchto.driver.listeners.profiler.Jmx;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

@ConfigurationProperties(prefix = "benchmark.feature.profiler.async")
@Configuration
public class AsyncProfilerProperties
{
    private Jmx jmx;
    private boolean enabled;
    private Path outputPath;
    private List<AsyncProfilerProperties.Event> events;
    private String asyncLibraryPath;

    public Jmx getJmx()
    {
        return jmx;
    }

    public void setJmx(Jmx jmx)
    {
        this.jmx = jmx;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public Path getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(Path outputPath)
    {
        this.outputPath = outputPath;
    }

    public void setEvents(List<AsyncProfilerProperties.Event> events)
    {
        this.events = events;
    }

    public List<AsyncProfilerProperties.Event> getEvents()
    {
        return events;
    }

    public String getAsyncLibraryPath()
    {
        return asyncLibraryPath;
    }

    public void setAsyncLibraryPath(String asyncLibraryPath)
    {
        this.asyncLibraryPath = asyncLibraryPath;
    }

    enum Event
    {
        cpu,
        wall
    }
}
