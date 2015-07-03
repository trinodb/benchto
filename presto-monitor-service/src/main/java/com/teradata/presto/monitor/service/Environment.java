/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service;

import static com.google.common.base.Preconditions.checkNotNull;

public class Environment
{
    private String name;
    private String prestoUrl;

    public Environment(String name, String prestoUrl)
    {
        this.name = checkNotNull(name);
        this.prestoUrl = checkNotNull(prestoUrl);
    }

    public String getName()
    {
        return name;
    }

    public String getPrestoUrl()
    {
        return prestoUrl;
    }
}
