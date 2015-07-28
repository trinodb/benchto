/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.Query;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.UUID;

@Component
public class SqlStatementGenerator
{
    @Autowired
    private Configuration freemarkerConfiguration;

    public String generateQuerySqlStatement(Query query, Map<String, ?> attributes)
    {
        try {
            // template name must be unique to ensure correct templates caching
            String templateName = UUID.randomUUID().toString();
            Template queryTemplate = new Template(templateName, new StringReader(query.getSqlTemplate()), freemarkerConfiguration);

            return FreeMarkerTemplateUtils.processTemplateIntoString(queryTemplate, attributes);
        }
        catch (IOException | TemplateException e) {
            throw new BenchmarkExecutionException(e);
        }
    }
}
