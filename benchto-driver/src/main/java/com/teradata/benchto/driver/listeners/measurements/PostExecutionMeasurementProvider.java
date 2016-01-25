/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.listeners.measurements;

import com.teradata.benchto.driver.Measurable;
import com.teradata.benchto.driver.service.Measurement;

import java.util.List;

public interface PostExecutionMeasurementProvider
{
    List<Measurement> loadMeasurements(Measurable measurable);
}
