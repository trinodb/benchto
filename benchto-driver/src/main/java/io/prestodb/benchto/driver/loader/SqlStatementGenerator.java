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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.prestodb.benchto.driver.BenchmarkExecutionException;
import io.prestodb.benchto.driver.Query;
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
