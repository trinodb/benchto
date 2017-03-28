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
package com.teradata.benchto.driver;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class Query
{
    private final Map<String, String> properties;
    private final String name;
    private final String sqlTemplate;

    public Query(String name, String sqlTemplate, Map<String, String> properties)
    {
        this.name = requireNonNull(name);
        this.sqlTemplate = requireNonNull(sqlTemplate);
        this.properties = requireNonNull(properties);
    }

    public String getName()
    {
        return name;
    }

    public String getSqlTemplate()
    {
        return sqlTemplate;
    }

    public Optional<String> getProperty(String key)
    {
        return Optional.ofNullable(properties.get(key));
    }

    public String getProperty(String key, String defaultValue)
    {
        return properties.getOrDefault(key, defaultValue);
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("name", name)
                .add("sqlTemplate", sqlTemplate)
                .toString();
    }
}
