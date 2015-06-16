package com.teradata.benchmark.driver.jdbc;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties
public class MultipleDataSourcesProperties {

    private Map<String, DataSourceProperties> dataSources = new HashMap<>();

    public Map<String, DataSourceProperties> getDataSources() {
        return dataSources;
    }
}
