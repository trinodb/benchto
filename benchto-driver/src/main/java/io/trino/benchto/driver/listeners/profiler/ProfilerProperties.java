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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "benchmark.feature.profiler")
@Configuration
public class ProfilerProperties
{
    private JMX jmx;
    private Profiler tool = Profiler.jfr;
    private boolean enabled;
    private Path outputPath;
    private String profiledCoordinator;
    private String profiledWorker;

    public JMX getJmx()
    {
        return jmx;
    }

    public void setJmx(JMX jmx)
    {
        this.jmx = jmx;
    }

    public Profiler getTool()
    {
        return tool;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void setTool(Profiler tool)
    {
        this.tool = tool;
    }

    public Path getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(Path outputPath)
    {
        this.outputPath = outputPath;
    }

    public String getProfiledCoordinator()
    {
        return profiledCoordinator;
    }

    public void setProfiledCoordinator(String profiledCoordinator)
    {
        this.profiledCoordinator = profiledCoordinator;
    }

    public String getProfiledWorker()
    {
        return profiledWorker;
    }

    public void setProfiledWorker(String profiledWorker)
    {
        this.profiledWorker = profiledWorker;
    }

    enum Profiler
    {
        jfr
    }

    static class JMX
    {
        private int port = 9090;

        public void setPort(int port)
        {
            this.port = port;
        }

        public int getPort()
        {
            return port;
        }
    }
}
