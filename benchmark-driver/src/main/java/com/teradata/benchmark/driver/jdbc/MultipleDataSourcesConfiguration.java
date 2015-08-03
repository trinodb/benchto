/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

import java.util.Map;

import static com.teradata.benchmark.driver.utils.PropertiesUtils.resolveEnvironmentProperties;
import static com.teradata.benchmark.driver.utils.Types.checkType;
import static java.util.stream.Collectors.toMap;

@Configuration
public class MultipleDataSourcesConfiguration
        implements BeanFactoryPostProcessor, EnvironmentAware
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleDataSourcesConfiguration.class);

    static final int INITIAL_POOL_SIZE = 1;
    static final int MAX_POOL_SIZE = 500;

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
        DataSource dataSource = DataSourceBuilder
                .create()
                .url(properties.getUrl())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .driverClassName(properties.getDriverClassName())
                .build();
        setConnectionPoolProperties(dataSource);
        return dataSource;
    }

    private void register(ConfigurableListableBeanFactory beanFactory, Map<String, DataSource> dataSources)
    {

        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            beanFactory.registerSingleton(entry.getKey(), entry.getValue());
        }
    }

    private void setConnectionPoolProperties(DataSource dataSource)
    {
        org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = checkType(
                dataSource, org.apache.tomcat.jdbc.pool.DataSource.class, "dataSource"
        );

        tomcatDataSource.setInitialSize(INITIAL_POOL_SIZE);
        tomcatDataSource.setMaxActive(MAX_POOL_SIZE);

        // do not wait for connection
        // when all the connections are used, just throw an exception that connection pool is exhausted
        // we must use here 1ms instead 0ms, because 0 means wait forever.
        tomcatDataSource.setMaxWait(1);

        LOGGER.debug("DataSource connection pool properties have been successfully set.");
    }
}
