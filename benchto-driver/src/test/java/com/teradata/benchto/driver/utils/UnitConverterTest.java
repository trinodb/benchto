/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.utils;

import org.junit.Test;

import static com.teradata.benchto.driver.utils.UnitConverter.parseValueAsUnit;
import static javax.measure.unit.NonSI.BYTE;
import static javax.measure.unit.SI.MILLI;
import static javax.measure.unit.SI.SECOND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class UnitConverterTest
{

    @Test
    public void testParseValueAsBytes()
    {
        assertThat(parseValueAsUnit("10B", BYTE)).isEqualTo(10.0);
        assertThat(parseValueAsUnit("1.0kB", BYTE)).isEqualTo(1000.0);
        assertThat(parseValueAsUnit("1.0KB", BYTE)).isEqualTo(1000.0);
        assertThat(parseValueAsUnit("2.0MB", BYTE)).isEqualTo(2000000.0);
        assertThat(parseValueAsUnit("3.0GB", BYTE)).isEqualTo(3000000000.0);
        assertThat(parseValueAsUnit("4.0TB", BYTE)).isEqualTo(4000000000000.0);
    }

    @Test
    public void testParseValueAsMilliseconds()
    {
        assertThat(parseValueAsUnit("50ns", MILLI(SECOND))).isCloseTo(0.00005, offset(0.00000000001));
        assertThat(parseValueAsUnit("30us", MILLI(SECOND))).isCloseTo(0.03, offset(0.00000000001));
        assertThat(parseValueAsUnit("10ms", MILLI(SECOND))).isEqualTo(10.0);
        assertThat(parseValueAsUnit("20s", MILLI(SECOND))).isEqualTo(20000.0);
        assertThat(parseValueAsUnit("30m", MILLI(SECOND))).isEqualTo(1800000.0);
        assertThat(parseValueAsUnit("4h", MILLI(SECOND))).isEqualTo(14400000.0);
        assertThat(parseValueAsUnit("5d", MILLI(SECOND))).isEqualTo(432000000.0);
    }
}
