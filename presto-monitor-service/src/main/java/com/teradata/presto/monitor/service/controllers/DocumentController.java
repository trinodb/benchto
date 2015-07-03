/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service.controllers;

import com.teradata.presto.monitor.service.controllers.response.DocumentDescriptor;
import com.teradata.presto.monitor.service.model.Document;
import com.teradata.presto.monitor.service.model.Snapshot;
import com.teradata.presto.monitor.service.repo.DocumentRepo;
import com.teradata.presto.monitor.service.repo.SnapshotRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

import java.time.ZonedDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static com.teradata.presto.monitor.service.utils.RequestUtils.extractSnapshotId;
import static com.teradata.presto.monitor.service.utils.RequestUtils.getReferer;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
public class DocumentController
{
    private static final Logger LOG = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SnapshotRepo snapshotRepo;

    @Autowired
    private DocumentRepo documentRepo;

    @RequestMapping(value = "/v1/**", method = GET)
    @ResponseBody
    public String documentRequest(
            HttpServletRequest request)
    {
        LOG.info("New Json request, referer: {}, query: {}", getReferer(request), request.getQueryString());

        Optional<Long> querySnapshotId = extractSnapshotId(request.getQueryString());
        Optional<Long> refererSnapshotId = extractSnapshotId(getReferer(request));
        checkState(querySnapshotId.isPresent() || refererSnapshotId.isPresent());
        long snapshotId = querySnapshotId.isPresent() ? querySnapshotId.get() : refererSnapshotId.get();

        Snapshot snapshot = snapshotRepo.findOne(snapshotId);
        if (snapshot == null) {
            throw new ResourceNotFoundException();
        }

        String path = extractWildcardParameter(request);
        Optional<Document> document = snapshot.getDocuments().stream()
                .filter(d -> d.getName().equals("/v1/" + path))
                .findAny();

        if (!document.isPresent()) {
            throw new ResourceNotFoundException();
        }

        return document.get().getContent();
    }

    @RequestMapping(value = "/version/{environment}/{timestamp}/v1/**", method = GET)
    @ResponseBody
    public DocumentDescriptor documentVersionRequest(
            @PathVariable("environment") String environment,
            @PathVariable("timestamp") @DateTimeFormat(iso = DATE_TIME) ZonedDateTime timestamp,
            HttpServletRequest request)
    {
        String path = extractWildcardParameter(request);

        Document document = documentRepo.findLatestByName(environment, "/v1/" + path, timestamp);
        if (document == null) {
            throw new ResourceNotFoundException();
        }

        return new DocumentDescriptor(document);
    }

    @RequestMapping(value = "/timestamp/snapshot/{snapshotId}", method = GET)
    @ResponseBody
    public String getSnapshotTimestamp(@PathVariable("snapshotId") long snapshotId)
    {
        Snapshot snapshot = snapshotRepo.findOne(snapshotId);

        if (snapshot == null) {
            throw new ResourceNotFoundException();
        }

        return snapshot.getTimestamp().format(ISO_DATE_TIME);
    }

    private String extractWildcardParameter(HttpServletRequest request)
    {
        String path = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();
        return apm.extractPathWithinPattern(bestMatchPattern, path);
    }
}
