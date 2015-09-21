/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;

public class Query
{
    private final String name;
    private final String sqlTemplate;

    public Query(String name, String sqlTemplate)
    {
        this.name = checkNotNull(name);
        this.sqlTemplate = checkNotNull(sqlTemplate);
    }

    public String getName()
    {
        return name;
    }

    public String getSqlTemplate()
    {
        return sqlTemplate;
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
