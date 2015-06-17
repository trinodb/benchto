package com.teradata.benchmark.driver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class TestConfig
{

    @Primary
    @Bean(name = "defaultTaskExecutor")
    public TaskExecutor taskExecutor()
    {
        // MockRestServiceServer expects calls in particular order,
        // we need to use sync task executor
        return new SyncTaskExecutor();
    }
}
