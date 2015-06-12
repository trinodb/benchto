/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.teradata.benchmark")
public class App
{

    public static void main(String[] args)
    {
        ConfigurableApplicationContext ctx = SpringApplication.run(App.class, args);
        BenchmarkDriver benchmarkDriver = ctx.getBean(BenchmarkDriver.class);

        boolean successful = benchmarkDriver.run();
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
}
