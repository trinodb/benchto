/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service;

import com.teradata.presto.monitor.service.filter.PrestoSnapshotIdAppender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import javax.servlet.Filter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableScheduling
public class PrestoMonitorApp
{
    @Bean
    public RestTemplate restTemplate()
    {
        return new RestTemplate();
    }

    @Bean(name = "downloadExecutorService")
    public ExecutorService downloadExecutorService(@Value("${download.threads:10}") int downloadThreads)
    {
        return Executors.newFixedThreadPool(downloadThreads);
    }

    @Bean
    public Filter snapshotIdAppender() {
        return new PrestoSnapshotIdAppender();
    }

    public static void main(String[] args)
    {
        SpringApplication.run(PrestoMonitorApp.class, args);
    }
}
