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
package io.trino.benchto.driver.utils;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Maps.immutableEntry;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Useful methods for dealing with Yaml files.
 */
public final class YamlUtils
{
    public static Map<Object, Object> loadYamlFromPath(Path path)
            throws IOException
    {
        return loadYamlFromString(new String(readAllBytes(path), UTF_8));
    }

    @SuppressWarnings("unchecked")
    public static Map<Object, Object> loadYamlFromString(String string)
    {
        Yaml yaml = new Yaml();
        return Optional.ofNullable((Map) yaml.load(string)).orElse(ImmutableMap.of());
    }

    public static Map<String, List<String>> stringifyMultimap(Map<Object, Object> variableMap)
    {
        return variableMap.entrySet()
                .stream()
                .map(entry -> immutableEntry(entry.getKey().toString(),
                        asStringList(requireNonNull(entry.getValue(), "Null value for key: " + entry.getKey()))))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static List<String> asStringList(Object object)
    {
        if (!(object instanceof Iterable<?> iterable)) {
            return ImmutableList.copyOf(Splitter.on(",").trimResults().omitEmptyStrings().split(object.toString()));
        }
        else {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(Object::toString)
                    .collect(toList());
        }
    }

    private YamlUtils()
    {
    }
}
