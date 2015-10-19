/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
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
