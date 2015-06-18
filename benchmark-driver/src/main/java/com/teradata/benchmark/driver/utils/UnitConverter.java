/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.utils;

import javax.measure.unit.Unit;

import static javax.measure.unit.NonSI.BYTE;
import static javax.measure.unit.NonSI.DAY;
import static javax.measure.unit.NonSI.HOUR;
import static javax.measure.unit.NonSI.MINUTE;
import static javax.measure.unit.SI.GIGA;
import static javax.measure.unit.SI.KILO;
import static javax.measure.unit.SI.MEGA;
import static javax.measure.unit.SI.MICRO;
import static javax.measure.unit.SI.MILLI;
import static javax.measure.unit.SI.NANO;
import static javax.measure.unit.SI.SECOND;

/**
 * Helps converting between units.
 */
// TODO: use JSR 363 when available
public final class UnitConverter
{
    public static Unit<?> unitFor(String source)
    {
        switch (source) {
            case "d":
                return DAY;
            case "h":
                return HOUR;
            case "m":
                return MINUTE;
            case "s":
                return SECOND;
            case "ms":
                return MILLI(SECOND);
            case "ns":
                return NANO(SECOND);
            case "us":
                return MICRO(SECOND);
            case "B":
                return BYTE;
            case "kB":
                return KILO(BYTE);
            case "MB":
                return MEGA(BYTE);
            case "GB":
                return GIGA(BYTE);
            default:
                throw new IllegalArgumentException(source + " unit is not supported");
        }
    }

    public static String format(Unit<?> unit)
    {
        if (unit.equals(BYTE)) {
            return "BYTES";
        }
        else if (unit.equals(MILLI(SECOND))) {
            return "MILLISECONDS";
        }
        else {
            throw new IllegalArgumentException(unit + " is not supported");
        }
    }
}
