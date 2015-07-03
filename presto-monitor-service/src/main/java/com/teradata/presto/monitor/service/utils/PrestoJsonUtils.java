/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service.utils;

import com.jayway.jsonpath.ReadContext;

import java.util.List;

public final class PrestoJsonUtils
{
    public static List<String> queryIdsFromQueryList(ReadContext queryList)
    {
        return queryList.read("$.[*].queryId");
    }

    private PrestoJsonUtils()
    {
    }
}
