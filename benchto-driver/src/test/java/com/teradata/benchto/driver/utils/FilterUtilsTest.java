/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchto.driver.utils;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import static com.teradata.benchto.driver.utils.FilterUtils.pathContainsAny;
import static org.assertj.core.api.Assertions.assertThat;

public class FilterUtilsTest
{
    @Test
    public void test()
    {
        Predicate<Path> pathPredicate = pathContainsAny(ImmutableList.of("simple", "YACK"));

        assertThat(pathPredicate.test(Paths.get("simple"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/simple"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/simple.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("simple/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/simple/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/YACK/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/YA-CK/file.yaml"))).isFalse();
    }
}
