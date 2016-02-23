/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.jdbc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import java.util.Map;

import static com.teradata.benchto.driver.utils.PropertiesUtils.resolveEnvironmentProperties;
import static java.util.stream.Collectors.toMap;

@Configuration
public class MultipleDataSourcesConfiguration
        implements BeanFactoryPostProcessor, EnvironmentAware
{
    private MultipleDataSourcesProperties multipleDataSourcesProperties;

    @Override
    public void setEnvironment(Environment environment)
    {
        ConfigurableEnvironment configurableEnvironment = ConfigurableEnvironment.class.cast(environment);
        multipleDataSourcesProperties = resolveEnvironmentProperties(configurableEnvironment, MultipleDataSourcesProperties.class);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException
    {
        Map<String, DataSource> dataSources = createDeclaredDataSources();
        register(beanFactory, dataSources);
    }

    private Map<String, DataSource> createDeclaredDataSources()
    {
        return multipleDataSourcesProperties.getDataSources()
                .entrySet().stream()
                .collect(toMap(Map.Entry::getKey, (entry) -> createDataSource(entry.getValue())));
    }

    private DataSource createDataSource(DataSourceProperties properties)
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        return dataSource;
    }

    private void register(ConfigurableListableBeanFactory beanFactory, Map<String, DataSource> dataSources)
    {
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            beanFactory.registerSingleton(entry.getKey(), entry.getValue());
        }
    }
}
