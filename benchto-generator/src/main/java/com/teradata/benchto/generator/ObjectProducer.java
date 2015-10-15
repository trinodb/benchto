/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.generator;

public interface ObjectProducer<T>
{
    T generateNext();
}
