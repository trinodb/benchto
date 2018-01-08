/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestodb.benchto.driver.loader;

import io.prestodb.benchto.driver.BenchmarkExecutionException;
import io.prestodb.benchto.driver.BenchmarkProperties;
import io.prestodb.benchto.driver.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.io.Files.getNameWithoutExtension;
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
        List<Path> queryPaths = properties.sqlFilesDirs().stream()
                .map(sqlFilesDir -> sqlFilesDir.resolve(queryName))
                .filter(Files::isRegularFile)
                .collect(toList());

        if (queryPaths.isEmpty()) {
            throw new BenchmarkExecutionException(format("Could not find any SQL query file for query name: %s", queryName));
        }

        if (queryPaths.size() > 1) {
            throw new BenchmarkExecutionException(format("Found multiple SQL query files for query name: %s", queryName));
        }

        Path queryPath = getOnlyElement(queryPaths);
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
}
