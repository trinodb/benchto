/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionDriver;
import com.teradata.benchmark.driver.listeners.BenchmarkServiceExecutionListener;
import com.teradata.benchmark.driver.listeners.GraphiteEventExecutionListener;
import com.teradata.benchmark.driver.listeners.LoggingBenchmarkExecutionListener;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkExecutionListener;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkStatusReporter;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableAutoConfiguration(exclude = {
        FreeMarkerAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
@ComponentScan(basePackages = "com.teradata.benchmark")
public class DriverApp
{

    public static void main(String[] args)
    {
        ConfigurableApplicationContext ctx = SpringApplication.run(DriverApp.class, args);
        BenchmarkExecutionDriver benchmarkExecutionDriver = ctx.getBean(BenchmarkExecutionDriver.class);

        boolean successful = benchmarkExecutionDriver.run();
        if (successful) {
            System.exit(0);
        }
        System.exit(1);
    }

    @Bean
    public RestTemplate restTemplate()
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        return restTemplate;
    }

    @Bean(name = "defaultTaskExecutor")
    public ThreadPoolTaskExecutor defaultTaskExecutor()
    {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(5);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(Integer.MAX_VALUE);

        return taskExecutor;
    }

    @Bean(name = "queryTaskExecutor")
    public ThreadPoolTaskExecutor queryTaskExecutor()
    {
        // make it parallel
        ThreadPoolTaskExecutor queryExecutor = new ThreadPoolTaskExecutor();
        queryExecutor.setMaxPoolSize(1);
        queryExecutor.setCorePoolSize(1);
        queryExecutor.setWaitForTasksToCompleteOnShutdown(true);
        return queryExecutor;
    }

    @Bean(name = "prewarmStatusReporter")
    public BenchmarkStatusReporter prewarmStatusReporter(
            @Qualifier("defaultTaskExecutor") TaskExecutor taskExecutor)
    {
        return new BenchmarkStatusReporter(taskExecutor, ImmutableList.of());
    }

    @Bean(name = "benchmarkStatusReporter")
    public BenchmarkStatusReporter benchmarkStatusReporter(
            @Qualifier("defaultTaskExecutor") TaskExecutor taskExecutor,
            BenchmarkServiceExecutionListener benchmarkServiceExecutionListener,
            GraphiteEventExecutionListener graphiteEventExecutionListener,
            LoggingBenchmarkExecutionListener loggingBenchmarkExecutionListener)
    {
        return new BenchmarkStatusReporter(taskExecutor,
                ImmutableList.of(benchmarkServiceExecutionListener, graphiteEventExecutionListener, loggingBenchmarkExecutionListener));
    }

    @Bean
    public FreeMarkerConfigurationFactoryBean freemarkerConfiguration()
            throws IOException, TemplateException
    {
        FreeMarkerConfigurationFactoryBean factory = new FreeMarkerConfigurationFactoryBean();
        factory.setDefaultEncoding("UTF-8");
        return factory;
    }
}
