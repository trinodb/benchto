/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.queries;

import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.BenchmarkProperties;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Map;

import static java.lang.String.format;

@Component
public class QueryLoader {

    @Autowired
    private BenchmarkProperties benchmarkProperties;

    @Autowired
    private Configuration freemarkerConfiguration;

    /**
     * Loads query by the relative  path
     *
     * @param relativePath - relative to the sql dir path
     * @param attributes   - query attributes (eg. schema, database)
     * @return String with the SQL query which can be executed on the destination database
     */
    public String loadQuery(String relativePath, Map<String, ?> attributes) {
        String templatePath = getQueryTemplatePath(relativePath);
        try {
            Template queryTemplate = getQueryTemplate(templatePath);
            return FreeMarkerTemplateUtils.processTemplateIntoString(queryTemplate, attributes);
        } catch (IOException | TemplateException e) {
            throw new BenchmarkExecutionException(format(
                    "Error during loading query from path [%s]. Attributes=[%s].",
                    templatePath, JSONObject.valueToString(attributes)
            ), e);
        }
    }

    private Template getQueryTemplate(String templatePath) throws IOException {
        return freemarkerConfiguration.getTemplate(templatePath, "UTF-8");
    }

    private String getQueryTemplatePath(String relativePath) {
        return format("%s/%s", benchmarkProperties.getSqlDir(), relativePath);
    }

}
