/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service.utils;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Long.parseLong;

public final class RequestUtils
{
    private static Pattern SNAPSHOT_ID_PATTERN = Pattern.compile(".*snapshotId=(\\d+).*");

    public static Optional<Long> extractSnapshotId(String string)
    {
        if (string == null) {
            return Optional.empty();
        }

        Matcher matcher = SNAPSHOT_ID_PATTERN.matcher(string);

        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(parseLong(matcher.group(1)));
    }

    public static String getReferer(HttpServletRequest request)
    {
        return request.getHeader("referer");
    }

    private RequestUtils()
    {
    }
}
