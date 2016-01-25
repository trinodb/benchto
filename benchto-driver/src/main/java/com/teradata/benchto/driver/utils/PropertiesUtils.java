/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.utils;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.validation.BindException;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

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
            throw new FatalBeanException("Could not bind " + clazz + " properties", ex);
        }
    }

    public static Optional<List<String>> splitProperty(String value)
    {
        if (isNullOrEmpty(value)) {
            return Optional.empty();
        }

        Iterable<String> values = Splitter.on(",").trimResults().split(value);
        return Optional.of(ImmutableList.copyOf(values));
    }

    private PropertiesUtils() {}
}
