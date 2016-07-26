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
