/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver;

import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.execution.ExecutionDriver;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
@EnableRetry
@EnableAutoConfiguration(exclude = {
        FreeMarkerAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
@ComponentScan(basePackages = "com.teradata.benchto")
public class DriverApp
{

    private static final Logger LOG = LoggerFactory.getLogger(DriverApp.class);

    public static void main(String[] args)
    {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(DriverApp.class).web(false).run(args);
        ExecutionDriver executionDriver = ctx.getBean(ExecutionDriver.class);

        Thread.currentThread().setName("main");

        try {
            executionDriver.execute();
            System.exit(0);
        }
        catch (Throwable e) {
            logException(e);
            System.exit(1);
        }
    }

    private static void logException(Throwable e)
    {
        LOG.error("Benchmark execution failed: {}", e.getMessage(), e);
        if (e instanceof FailedBenchmarkExecutionException) {
            FailedBenchmarkExecutionException failedBenchmarkExecutionException = (FailedBenchmarkExecutionException) e;
            for (BenchmarkExecutionResult failedBenchmarkResult : failedBenchmarkExecutionException.getFailedBenchmarkResults()) {
                LOG.error("--------------------------------------------------------------------------");
                LOG.error("Failed benchmark: {}", failedBenchmarkResult.getBenchmark().getUniqueName());
                for (Exception failureCause : failedBenchmarkResult.getFailureCauses()) {
                    LOG.error("Cause: {}", failureCause.getMessage(), failureCause);
                }
            }
            LOG.error("Total benchmarks failed {} out of {}",
                    failedBenchmarkExecutionException.getFailedBenchmarkResults().size(),
                    failedBenchmarkExecutionException.getBenchmarksCount());
        }
    }

    @Bean
    public RestTemplate restTemplate()
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        return restTemplate;
    }

    @Bean
    public ThreadPoolTaskExecutor defaultTaskExecutor()
    {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(5);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(300);

        return taskExecutor;
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
