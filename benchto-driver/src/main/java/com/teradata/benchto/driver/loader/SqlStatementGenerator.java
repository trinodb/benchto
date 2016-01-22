/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.loader;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.teradata.benchto.driver.BenchmarkExecutionException;
import com.teradata.benchto.driver.Query;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SqlStatementGenerator
{
    private static final Splitter SQL_STATEMENT_SPLITTER = Splitter.on(";").trimResults().omitEmptyStrings();

    @Autowired
    private Configuration freemarkerConfiguration;

    public List<String> generateQuerySqlStatement(Query query, Map<String, ?> attributes)
    {
        ImmutableList.Builder<String> sqlQueries = ImmutableList.<String>builder();
        String sqlTemplate = generateQuerySqlStatement(query.getSqlTemplate(), attributes);
        for (String sqlQuery : toSqlQueries(sqlTemplate)) {
            sqlQueries.add(sqlQuery);
        }
        return sqlQueries.build();
    }

    private static ImmutableList<String> toSqlQueries(String sqlTemplate)
    {
        return ImmutableList.copyOf(SQL_STATEMENT_SPLITTER.split(sqlTemplate));
    }

    private String generateQuerySqlStatement(String sqlTemplate, Map<String, ?> attributes)
    {
        try {
            // template name must be unique to ensure correct templates caching
            String templateName = UUID.randomUUID().toString();
            Template queryTemplate = new Template(templateName, new StringReader(sqlTemplate), freemarkerConfiguration);

            return FreeMarkerTemplateUtils.processTemplateIntoString(queryTemplate, attributes);
        }
        catch (IOException | TemplateException e) {
            throw new BenchmarkExecutionException(e);
        }
    }
}
