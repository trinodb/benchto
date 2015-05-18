/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.teradata.benchmark")
public class App
{

    public static void main(String[] args)
    {
        ApplicationContext ctx = SpringApplication.run(App.class, args);
        BenchmarkDriver benchmarkDriver = ctx.getBean(BenchmarkDriver.class);

        boolean successful = benchmarkDriver.run();
        if (successful) {
            System.exit(0);
        }
        System.exit(1);
    }
}
