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
package com.teradata.benchto.driver.utils;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.teradata.benchto.driver.BenchmarkExecutionException;
import com.teradata.benchto.driver.loader.BenchmarkLoader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ResourceUtils
{
    public static Path asPath(String resourcePath)
    {
        URL resourceUrl = BenchmarkLoader.class.getClassLoader().getResource(resourcePath);
        if (resourceUrl != null) {
            try {
                return getPath(resourceUrl.toURI());
            }
            catch (URISyntaxException e) {
                throw new BenchmarkExecutionException("Cant resolve URL", e);
            }
        }
        return FileSystems.getDefault().getPath(resourcePath);
    }

    private static Path getPath(URI uri)
    {
        try {
            return Paths.get(uri);
        }
        catch (FileSystemNotFoundException e) {
            try {
                FileSystems.newFileSystem(uri, ImmutableMap.of());
                return Paths.get(uri);
            }
            catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }
    }

    private ResourceUtils() {}
}
