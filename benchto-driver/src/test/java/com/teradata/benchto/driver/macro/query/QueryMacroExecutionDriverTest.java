/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.macro.query;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryMacroExecutionDriverTest
{
    @Test
    public void testExtractKeyValue()
            throws Exception
    {

        assertThat(QueryMacroExecutionDriver.extractKeyValue("set session optimize_hash_generation=false"))
                .contains("optimize_hash_generation", "false");
        assertThat(QueryMacroExecutionDriver.extractKeyValue("set session task_default_concurrency=8"))
                .contains("task_default_concurrency", "8");
        assertThat(QueryMacroExecutionDriver.extractKeyValue("set session task_default_concurrency = 8"))
                .contains("task_default_concurrency", "8");
        assertThat(QueryMacroExecutionDriver.extractKeyValue("set session execution_policy='all-at-once'"))
                .contains("execution_policy", "all-at-once");
        assertThat(QueryMacroExecutionDriver.extractKeyValue("set session foo='bar=baz'"))
                .contains("foo", "bar=baz");
    }
}
