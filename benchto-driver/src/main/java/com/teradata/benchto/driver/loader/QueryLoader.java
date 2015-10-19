/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.loader;

import com.teradata.benchto.driver.BenchmarkExecutionException;
import com.teradata.benchto.driver.BenchmarkProperties;
import com.teradata.benchto.driver.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.google.common.io.Files.getNameWithoutExtension;
import static com.teradata.benchto.driver.utils.ResourceUtils.asPath;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Component
public class QueryLoader
{
    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private AnnotatedQueryParser annotatedQueryParser;

    /**
     * Loads query from given {@link Path}
     *
     * @param queryName - path to SQL query file
     * @return {@link Query} with the SQL query which can be executed on the destination database
     */
    public Query loadFromFile(String queryName)
    {
        Path queryPath = sqlFilesPath().resolve(queryName);
        try {
            String queryNameWithoutExtension = getNameWithoutExtension(queryPath.toString());
            return annotatedQueryParser.parseFile(queryNameWithoutExtension, queryPath);
        }
        catch (IOException e) {
            throw new BenchmarkExecutionException(format("Error during loading query from path %s", queryPath), e);
        }
    }

    public List<Query> loadFromFiles(List<String> queryNames)
    {
        return queryNames
                .stream()
                .map(this::loadFromFile)
                .collect(toList());
    }

    private Path sqlFilesPath()
    {
        return asPath(properties.getSqlDir());
    }
}
