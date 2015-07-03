/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service.controllers;

import com.teradata.presto.monitor.service.controllers.response.DocumentDescriptor;
import com.teradata.presto.monitor.service.model.Document;
import com.teradata.presto.monitor.service.repo.DocumentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
public class PrestoPageController
{
    private static final String FIRST_DOCUMENT_KEY = "firstDocument";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String PAGE_URL_KEY = "pageUrl";
    private static final String DOCUMENT_NAME_KEY = "documentName";
    private static final String LAST_DOCUMENT_KEY = "lastDocument";

    @Autowired
    private DocumentRepo documentRepo;

    @RequestMapping(value = "/history/{environment}/query/{queryId}", method = GET)
    @ResponseBody
    public ModelAndView queryRequest(
            @PathVariable("environment") String environment,
            @PathVariable("queryId") String queryId)
    {
        Map<String, Object> model = newHashMap();

        addPageInformation(environment, "/presto/query.html?" + queryId, model);
        addDocumentInformation(environment, "/v1/query/" + queryId, model);

        return new ModelAndView("history", model);
    }

    @RequestMapping(value = "/history/{environment}", method = GET)
    @ResponseBody
    public ModelAndView queryListRequest(@PathVariable("environment") String environment)
    {
        Map<String, Object> model = newHashMap();

        addPageInformation(environment, "/presto/index.html", model);
        addDocumentInformation(environment, "/v1/query", model);

        return new ModelAndView("history", model);
    }

    private void addPageInformation(String environment, String pageUrl, Map<String, Object> model)
    {
        model.put(ENVIRONMENT_KEY, environment);
        model.put(PAGE_URL_KEY, pageUrl);
    }

    private void addDocumentInformation(String environment, String documentName, Map<String, Object> model)
    {
        Document firstDocument = documentRepo.findFirstDocument(environment, documentName);
        Document lastDocument = documentRepo.findLastDocument(environment, documentName);
        if (firstDocument == null || lastDocument == null) {
            throw new ResourceNotFoundException();
        }

        model.put(DOCUMENT_NAME_KEY, documentName);
        model.put(FIRST_DOCUMENT_KEY, new DocumentDescriptor(firstDocument));
        model.put(LAST_DOCUMENT_KEY, new DocumentDescriptor(lastDocument));
    }
}
