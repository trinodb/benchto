/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toList;

@Component
@ConfigurationProperties
public class Environments
{
    private Map<String, Object> environments = newHashMap();

    private List<Environment> environmentList = newArrayList();

    @PostConstruct
    public void init()
    {
        environmentList = environments.keySet().stream()
                .map(this::getEnvironment)
                .collect(toList());
    }

    public List<Environment> getEnvironmentList()
    {
        return environmentList;
    }

    public Map<String, Object> getEnvironments()
    {
        return environments;
    }

    public void setEnvironments(Map<String, Object> environments)
    {
        this.environments = environments;
    }

    private Environment getEnvironment(String environmentName)
    {
        String prestoUrl = getValue(getMap(getMap(environments, environmentName), "presto"), "url");
        return new Environment(environmentName, prestoUrl);
    }

    private Map<String, Object> getMap(Map<String, Object> map, String key)
    {
        return (Map<String, Object>) map.get(key);
    }

    private String getValue(Map<String, Object> map, String key)
    {
        return (String) map.get(key);
    }
}
