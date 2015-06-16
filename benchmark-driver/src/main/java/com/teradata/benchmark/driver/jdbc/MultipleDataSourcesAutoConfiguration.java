package com.teradata.benchmark.driver.jdbc;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Configuration
@EnableConfigurationProperties(MultipleDataSourcesProperties.class)
public class MultipleDataSourcesAutoConfiguration {

    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    private MultipleDataSourcesProperties multipleDataSourcesProperties;

    @PostConstruct
    public void postConstruct() {
        Map<String, DataSource> dataSources = createDeclaredDataSources();
        register(dataSources);
    }

    private Map<String, DataSource> createDeclaredDataSources() {
        return multipleDataSourcesProperties.getDataSources()
                .entrySet().stream()
                .collect(toMap(Map.Entry::getKey, (entry) -> createDataSource(entry.getKey(), entry.getValue())));
    }

    private DataSource createDataSource(String name, DataSourceProperties properties) {
        return DataSourceBuilder
                .create()
                .url(properties.getUrl())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .driverClassName(properties.getDriverClassName())
                .build();
    }

    private void register(Map<String, DataSource> dataSources) {
        Assert.state(beanFactory instanceof ConfigurableBeanFactory, "Unsupported BeanFactory.");
        ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;

        for(Map.Entry<String, DataSource> entry : dataSources.entrySet()){
            configurableBeanFactory.registerSingleton(entry.getKey(), entry.getValue());
        }
    }

}
