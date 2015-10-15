/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.generator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Random;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class RegexMatchingStringProducerTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final long SEED = 1234L;
    private final int NUMBER_OF_STRINGS = 2;

    @Test
    public void testGeneratesMatchingString()
    {
        assertThatGeneratedExpressionMatches("H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?H?Holmes", 15, 100);
        assertThatGeneratedExpressionMatches("(a*)*", 1000, 1000);
        assertThatGeneratedExpressionMatches("\\w{10,12}", 10, 14);
        assertThatGeneratedExpressionMatches("[A-B]{90,120}", 100, 101);
        assertThatGeneratedExpressionMatches("[0-3]([a-c]|[e-g]+)", 1000000, 1000010);
        assertThatGeneratedExpressionMatches("e*(cat|dog)", 1000000, 1000003);
    }

    @Test
    public void testShouldFailNoMatchingString()
    {
        thrown.expect(IllegalStateException.class);
        RegexMatchingStringProducer regexMatchingStringProducer = new RegexMatchingStringProducer("a{100}", 101, 101, new Random(SEED));
        regexMatchingStringProducer.generateNext();
    }

    public void assertThatGeneratedExpressionMatches(String regex, int minLength, int maxLength)
    {
        RegexMatchingStringProducer regexMatchingStringProducer = new RegexMatchingStringProducer(regex, minLength, maxLength, new Random(SEED));
        Pattern pattern = Pattern.compile(regex);
        for (int i = 0; i < NUMBER_OF_STRINGS; ++i) {
            String matchingString = regexMatchingStringProducer.generateNext();
            assertThat(matchingString.length()).isBetween(minLength, maxLength);
            assertThat(pattern.matcher(matchingString).matches()).isTrue();
        }
    }
}
