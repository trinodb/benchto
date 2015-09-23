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
    private final List<String> sqlTemplates;

    public Query(String name, List<String> sqlTemplates, Map<String, String> properties)
    {
        this.name = checkNotNull(name);
        this.sqlTemplates = checkNotNull(sqlTemplates);
        this.properties = checkNotNull(properties);
    }

    public String getName()
    {
        return name;
    }

    public List<String> getSqlTemplates()
    {
        return sqlTemplates;
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
                .add("sqlTemplate", Joiner.on("; ").join(sqlTemplates))
                .toString();
    }
}
