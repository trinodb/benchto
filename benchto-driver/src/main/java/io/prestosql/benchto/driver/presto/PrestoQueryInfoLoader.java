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
package io.prestosql.benchto.driver.presto;

import io.prestosql.benchto.driver.Measurable;
import io.prestosql.benchto.driver.execution.QueryExecutionResult;
import io.prestosql.benchto.driver.listeners.queryinfo.QueryInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.presto", value = "queryinfo.collection.enabled")
public class PrestoQueryInfoLoader
        implements QueryInfoProvider
{
    @Autowired
    private PrestoClient prestoClient;

    @Override
    public CompletableFuture<Optional<String>> loadQueryInfo(Measurable measurable)
    {
        if (measurable instanceof QueryExecutionResult) {
            QueryExecutionResult executionResult = (QueryExecutionResult) measurable;
            if (executionResult.getPrestoQueryId().isPresent()) {
                return completedFuture(Optional.of(prestoClient.getQueryInfo(executionResult.getPrestoQueryId().get())));
            }
        }
        return completedFuture(Optional.empty());
    }
}
