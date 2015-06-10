/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;

public class Query
{
    private final String name;
    private final String sql;

    public Query(String name, String sql)
    {
        this.name = checkNotNull(name);
        this.sql = checkNotNull(sql);
    }

    public String getName()
    {
        return name;
    }

    public String getSql()
    {
        return sql;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("sql", sql)
                .toString();
    }
}
