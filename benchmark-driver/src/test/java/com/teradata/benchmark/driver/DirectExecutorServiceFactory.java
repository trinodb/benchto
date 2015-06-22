/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.teradata.benchmark.driver.concurrent.ExecutorServiceFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static com.facebook.presto.jdbc.internal.guava.util.concurrent.MoreExecutors.newDirectExecutorService;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;

@Component
@Primary
public class DirectExecutorServiceFactory
        extends ExecutorServiceFactory
{
    @Override
    public ListeningExecutorService create(int concurrency)
    {
        // no concurrency in tests
        return listeningDecorator(newDirectExecutorService());
    }
}
