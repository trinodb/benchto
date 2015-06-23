/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchmark.driver.utils;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import static com.teradata.benchmark.driver.utils.FileUtils.pathMatchesTo;
import static org.assertj.core.api.Assertions.assertThat;

public class FileUtilsTest
{
    @Test
    public void test()
    {
        Predicate<Path> pathPredicate = pathMatchesTo(ImmutableList.of("simple", "YACK"));

        assertThat(pathPredicate.test(Paths.get("simple"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/simple"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/simple.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("simple/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/simple/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/YACK/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/YA-CK/file.yaml"))).isFalse();
    }
}