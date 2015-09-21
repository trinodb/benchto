/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchto.driver.concurrent;

import com.google.common.util.concurrent.ListeningExecutorService;
import org.springframework.stereotype.Component;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newFixedThreadPool;

@Component
public class ExecutorServiceFactory
{
    public ListeningExecutorService create(int concurrency)
    {
        return listeningDecorator(newFixedThreadPool(concurrency));
    }
}
