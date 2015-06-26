/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PrestoMonitorApp
{
    public static void main(String[] args)
    {
        SpringApplication.run(PrestoMonitorApp.class, args);
    }
}
