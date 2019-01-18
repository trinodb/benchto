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
package io.prestosql.benchto.service.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

public final class CollectionUtils
{
    private CollectionUtils()
    {
    }

    public static <K, V> Map<K, V> failSafeEmpty(Map<K, V> map)
    {
        if (map == null) {
            return ImmutableMap.of();
        }
        return map;
    }

    public static <T> List<T> failSafeEmpty(List<T> list)
    {
        if (list == null) {
            return ImmutableList.of();
        }
        return list;
    }
}
