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
package com.teradata.benchto.driver.loader;

import com.google.common.base.Splitter;
import com.google.common.collect.MapDifference.ValueDifference;
import com.teradata.benchto.driver.Query;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.difference;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * Parses sql queries from files where first line can be single line header.
 * The line must start with --! marker, and define semicolon separated map of params.
 * <p>
 * Example contents:
 * --! key1: value1; key2: value2a,value2b
 * --! key3: value3
 * FIRST_SQL_QUERY;
 * SECOND
 * SQL
 * QUERY;
 */
@Component
public class AnnotatedQueryParser
{
    private static final String COMMENT_LINE_PREFIX = "--";
    private static final String PROPERTIES_LINE_PREFIX = "--!";

    private static final Splitter.MapSplitter PROPERTIES_SPLITTER = Splitter.on(';')
            .omitEmptyStrings()
            .trimResults()
            .withKeyValueSeparator(Splitter.on(":").trimResults());

    public Query parseFile(String queryName, Path inputFile)
            throws IOException
    {
        return parseLines(queryName, Files.lines(inputFile, UTF_8).collect(toList()));
    }

    public Query parseLines(String queryName, List<String> lines)
    {
        lines = lines.stream()
                .map(line -> line.trim())
                .collect(toList());

        Map<String, String> properties = new HashMap<>();
        for (String line : lines) {
            if (isPropertiesLine(line)) {
                Map<String, String> lineProperties = parseLineProperties(line);
                Map<String, ValueDifference<String>> difference = difference(properties, lineProperties).entriesDiffering();
                checkState(difference.isEmpty(), "Different properties: ", difference);
                properties.putAll(lineProperties);
            }
        }

        String contentFiltered = lines.stream()
                .filter(this::isNotCommentLine)
                .collect(Collectors.joining("\n"));
        return new Query(queryName, contentFiltered, properties);
    }

    private Map<String, String> parseLineProperties(String line)
    {
        checkArgument(isPropertiesLine(line));

        return PROPERTIES_SPLITTER.split(line.substring(line.indexOf("!") + 1));
    }

    private boolean isPropertiesLine(String line)
    {
        return line.startsWith(PROPERTIES_LINE_PREFIX);
    }

    private boolean isNotCommentLine(String s)
    {
        return !s.startsWith(COMMENT_LINE_PREFIX);
    }
}
