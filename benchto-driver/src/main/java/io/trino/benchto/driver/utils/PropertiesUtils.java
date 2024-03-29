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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public final class PropertiesUtils
{
    public static <T> T resolveEnvironmentProperties(ConfigurableEnvironment environment, Class<T> clazz)
    {
        return resolveEnvironmentProperties(environment, clazz, "");
    }

    public static <T> T resolveEnvironmentProperties(ConfigurableEnvironment environment, Class<T> clazz, String prefix)
    {
        T properties = BeanUtils.instantiateClass(clazz);
        Binder binder = Binder.get(environment);
        binder.bind(prefix, Bindable.ofInstance(properties)).orElseThrow(() -> new FatalBeanException("Could not bind " + clazz + " properties"));
        return properties;
    }

    public static List<Path> extractPaths(String paths)
    {
        return splitProperty(paths).map(dirs -> dirs.stream()
                .map(Paths::get)
                .collect(toList()))
                .orElse(emptyList());
    }

    public static Optional<List<String>> splitProperty(String value)
    {
        if (isNullOrEmpty(value)) {
            return Optional.empty();
        }

        Iterable<String> values = Splitter.on(",").trimResults().split(value);
        return Optional.of(ImmutableList.copyOf(values));
    }

    private PropertiesUtils() {}
}
