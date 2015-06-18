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
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import static com.google.common.io.Files.getNameWithoutExtension;
import static java.lang.String.format;
import static java.nio.file.Files.newInputStream;

@Component
public class QueryLoader {
    @Autowired
    private Configuration freemarkerConfiguration;

    /**
     * Loads query from given {@link Path}
     *
     * @param queryPath  - path to SQL query file
     * @param attributes - query attributes (eg. schema, database)
     * @return {@link Query} with the SQL query which can be executed on the destination database
     */
    public Query loadFromFile(Path queryPath, Map<String, ?> attributes) {
        try {
            Template queryTemplate = getQueryTemplate(queryPath);
            String queryName = getNameWithoutExtension(queryPath.toString());
            return new Query(queryName, FreeMarkerTemplateUtils.processTemplateIntoString(queryTemplate, attributes));
        } catch (IOException | TemplateException e) {
            throw new BenchmarkExecutionException(format(
                    "Error during loading query from path [%s]. Attributes=[%s].",
                    queryPath, Objects.toString(attributes)
            ), e);
        }
    }

    private Template getQueryTemplate(Path templatePath)
            throws IOException {
        // template name must be unique to ensure correct templates caching
        String templateName = templatePath.toString();
        return new Template(templateName, new InputStreamReader(newInputStream(templatePath)), freemarkerConfiguration);
    }
}
