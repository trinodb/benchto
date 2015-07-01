/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.BenchmarkProperties;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.io.Files.getNameWithoutExtension;
import static com.teradata.benchmark.driver.utils.ResourceUtils.asPath;
import static java.lang.String.format;
import static java.nio.file.Files.newInputStream;
import static java.util.stream.Collectors.toList;

@Component
public class QueryLoader
{
    @Autowired
    private Configuration freemarkerConfiguration;

    @Autowired
    private BenchmarkProperties properties;

    /**
     * Loads query from given {@link Path}
     *
     * @param queryName - path to SQL query file
     * @param attributes - query attributes (eg. schema, database)
     * @return {@link Query} with the SQL query which can be executed on the destination database
     */
    public Query loadFromFile(String queryName, Map<String, ?> attributes)
    {
        Path queryPath = sqlFilesPath().resolve(queryName);
        try {
            Template queryTemplate = getQueryTemplate(queryPath);
            String queryNameWithoutExtension = getNameWithoutExtension(queryPath.toString());
            return new Query(queryNameWithoutExtension, FreeMarkerTemplateUtils.processTemplateIntoString(queryTemplate, attributes));
        }
        catch (IOException | TemplateException e) {
            throw new BenchmarkExecutionException(format(
                    "Error during loading query from path [%s]. Attributes=[%s].",
                    queryPath, Objects.toString(attributes)
            ), e);
        }
    }

    public List<Query> loadFromFiles(List<String> queryNames, Map<String, ?> attributes)
    {
        return queryNames
                .stream()
                .map(queryName -> loadFromFile(queryName, attributes))
                .collect(toList());
    }

    private Path sqlFilesPath()
    {
        return asPath(properties.getSqlDir());
    }

    private Template getQueryTemplate(Path templatePath)
            throws IOException
    {
        // template name must be unique to ensure correct templates caching
        String templateName = templatePath.toString();
        return new Template(templateName, new InputStreamReader(newInputStream(templatePath)), freemarkerConfiguration);
    }
}
