/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class Query
{
    private final Map<String, String> properties;
    private final String name;
    private final String sqlTemplate;

    public Query(String name, String sqlTemplate, Map<String, String> properties)
    {
        this.name = checkNotNull(name);
        this.sqlTemplate = checkNotNull(sqlTemplate);
        this.properties = checkNotNull(properties);
    }

    public String getName()
    {
        return name;
    }

    public String getSqlTemplate()
    {
        return sqlTemplate;
    }

    public Optional<String> getProperty(String key)
    {
        return Optional.ofNullable(properties.get(key));
    }

    public String getProperty(String key, String defaultValue)
    {
        return properties.getOrDefault(key, defaultValue);
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("sqlTemplate", sqlTemplate)
                .toString();
    }
}
