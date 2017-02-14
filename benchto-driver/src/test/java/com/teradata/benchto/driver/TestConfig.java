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
package com.teradata.benchto.driver;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.teradata.benchto.driver.concurrent.ExecutorServiceFactory;
import com.teradata.benchto.driver.macro.MacroService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import static com.facebook.presto.jdbc.internal.guava.util.concurrent.MoreExecutors.newDirectExecutorService;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static org.mockito.Mockito.spy;

public class TestConfig
{

    @Primary
    @Bean
    public AsyncTaskExecutor defaultTaskExecutor()
    {
        // MockRestServiceServer expects calls in particular order,
        // we need to use sync task executor
        return new TaskExecutorAdapter(MoreExecutors.directExecutor());
    }

    @Primary
    @Bean
    public MacroService macroExecutionDriver(MacroService macroService)
    {
        return spy(macroService);
    }

    @Primary
    @Bean
    public ExecutorServiceFactory getDirectTestExecutorServiceFactory()
    {
        return new ExecutorServiceFactory()
        {
            @Override
            public ListeningExecutorService create(int concurrency)
            {
                // no concurrency in tests
                return listeningDecorator(newDirectExecutorService());
            }
        };
    }
}
