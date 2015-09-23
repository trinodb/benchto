/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.loader;

import com.teradata.benchto.driver.BenchmarkExecutionException;
import com.teradata.benchto.driver.Query;
import com.google.common.collect.ImmutableList;
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

import static java.util.stream.Collectors.toList;

@Component
public class SqlStatementGenerator
{
    @Autowired
    private Configuration freemarkerConfiguration;

    public List<String> generateQuerySqlStatement(Query query, Map<String, ?> attributes)
    {
        ImmutableList.Builder<String> sqlQueries = ImmutableList.<String>builder();
        for (String sqlTemplate : query.getSqlTemplates()) {
            sqlQueries.add(generateQuerySqlStatement(sqlTemplate, attributes));
        }
        return sqlQueries.build();
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
