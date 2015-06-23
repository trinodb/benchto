/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners.measurements;

import com.teradata.benchmark.driver.Measurable;
import com.teradata.benchmark.driver.service.Measurement;

import java.util.List;

public interface PostExecutionMeasurementProvider
{
    List<Measurement> loadMeasurements(Measurable measurable);
}
