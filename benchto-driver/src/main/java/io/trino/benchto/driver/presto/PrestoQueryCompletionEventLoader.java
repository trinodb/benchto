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
package io.trino.benchto.driver.presto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.trino.benchto.driver.Measurable;
import io.trino.benchto.driver.execution.QueryExecutionResult;
import io.trino.benchto.driver.listeners.queryinfo.QueryCompletionEventProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.presto", value = "query.completion.event.collection.enabled")
public class PrestoQueryCompletionEventLoader
        implements QueryCompletionEventProvider
{
    @Autowired
    private PrestoClient prestoClient;

    @Override
    public CompletableFuture<Optional<String>> loadQueryCompletionEvent(Measurable measurable)
    {
        if (measurable instanceof QueryExecutionResult executionResult) {
            if (executionResult.getPrestoQueryId().isPresent()) {
                Optional<String> queryCompletionEvent = prestoClient.getQueryCompletionEvent(executionResult.getPrestoQueryId().get());
                queryCompletionEvent = queryCompletionEvent.map(event -> {
                    JsonElement root = JsonParser.parseReader(new JsonReader(new StringReader(event)));
                    JsonObject rootObject = root.getAsJsonObject();
                    JsonObject statistics = rootObject.getAsJsonObject("statistics");
                    // remove huge operatorSummaries entry as it is already part of query_info
                    statistics.remove("operatorSummaries");
                    StringWriter stringWriter = new StringWriter();
                    JsonWriter jsonWriter = new JsonWriter(stringWriter);
                    jsonWriter.setLenient(true);
                    try {
                        Streams.write(root, jsonWriter);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return stringWriter.toString();
                });
                return completedFuture(queryCompletionEvent);
            }
        }
        return completedFuture(Optional.empty());
    }
}
