/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchmark.driver.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.validation.BindException;

public final class PropertiesUtils
{
    public static <T> T resolveEnvironmentProperties(ConfigurableEnvironment environment, Class<T> clazz)
    {
        return resolveEnvironmentProperties(environment, clazz, "");
    }

    public static <T> T resolveEnvironmentProperties(ConfigurableEnvironment environment, Class<T> clazz, String prefix)
    {
        try {
            T properties = BeanUtils.instantiate(clazz);
            PropertiesConfigurationFactory<T> factory = new PropertiesConfigurationFactory<T>(properties);
            factory.setTargetName(prefix);
            factory.setPropertySources(environment.getPropertySources());
            factory.setConversionService(environment.getConversionService());
            factory.bindPropertiesToTarget();
            return properties;
        }
        catch (BindException ex) {
            throw new FatalBeanException("Could not bind DataSourceSettings properties", ex);
        }
    }

    private PropertiesUtils() {}
}
