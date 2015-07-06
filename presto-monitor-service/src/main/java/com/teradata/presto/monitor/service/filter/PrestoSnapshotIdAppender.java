/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

import static com.teradata.presto.monitor.service.utils.RequestUtils.extractSnapshotId;
import static com.teradata.presto.monitor.service.utils.RequestUtils.getReferer;

public class PrestoSnapshotIdAppender
        implements Filter
{
    @Override
    public void init(FilterConfig filterConfig)
            throws ServletException
    {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String query = httpRequest.getQueryString();
        Optional<Long> querySnapshotId = extractSnapshotId(query);
        Optional<Long> refererSnapshotId = extractSnapshotId(getReferer(httpRequest));

        if (!querySnapshotId.isPresent() && refererSnapshotId.isPresent()) {
            String redirectRequest = httpRequest.getRequestURI();

            if (query != null) {
                redirectRequest += "?" + query;
            }
            redirectRequest += "?snapshotId=" + refererSnapshotId.get();

            httpResponse.sendRedirect(redirectRequest);
        }
        else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy()
    {
    }
}
