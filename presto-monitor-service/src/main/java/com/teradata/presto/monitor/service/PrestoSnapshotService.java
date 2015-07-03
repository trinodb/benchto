/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.teradata.presto.monitor.service.model.Document;
import com.teradata.presto.monitor.service.model.Snapshot;
import com.teradata.presto.monitor.service.repo.SnapshotRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.teradata.presto.monitor.service.utils.PrestoJsonUtils.queryIdsFromQueryList;
import static java.time.Clock.systemUTC;
import static java.util.stream.Collectors.toList;

@Service
public class PrestoSnapshotService
{
    private static final Logger LOG = LoggerFactory.getLogger(PrestoSnapshotService.class);

    private static final String SERVICE_PATH = "/v1/service";
    private static final String QUERY_PATH = "/v1/query";
    private static final String QUERY_EXECUTION_PATH = "/v1/query-execution";

    @Autowired
    private Environments environments;

    @Autowired
    @Qualifier("downloadExecutorService")
    private ExecutorService downloadExecutorService;

    @Autowired
    private SnapshotRepo snapshotRepo;

    @Autowired
    private RestTemplate restTemplate;

    @Scheduled(fixedDelayString = "${snapshot.interval}", initialDelayString = "${snapshot.interval}")
    @Transactional
    public void makeSnapshot()
    {
        environments.getEnvironmentList().stream().forEach(this::makeSnapshot);
    }

    private void makeSnapshot(Environment environment)
    {
        try {
            Snapshot snapshot = createSnapshot();
            downloadServiceDocument(snapshot, environment);
            downloadQueryDocuments(snapshot, environment);
            snapshotRepo.save(snapshot);
            LOG.info("Created new snapshot with ID: {}", snapshot.getId());
        }
        catch (HttpServerErrorException | InterruptedException e) {
            LOG.error("Could not download snapshot for environment: {}", environment.getName());
        }
    }

    private void downloadServiceDocument(Snapshot snapshot, Environment environment)
    {
        addDocument(snapshot, environment, SERVICE_PATH, downloadPrestoFile(environment, SERVICE_PATH));
    }

    private void downloadQueryDocuments(Snapshot snapshot, Environment environment)
            throws InterruptedException
    {
        ReadContext queryList = downloadQueryListDocument(snapshot, environment);
        List<Callable<Void>> queryDownloadCallables = queryIdsFromQueryList(queryList).stream()
                .map(queryId -> (Callable<Void>) () -> {
                    try {
                        downloadQueryDocument(snapshot, environment, queryId);
                        downloadQueryExecutionDocument(snapshot, environment, queryId);
                    }
                    catch (HttpServerErrorException e) {
                        LOG.info("Could not download JSONs for query {} in environment {}", queryId, environment.getName(), e);
                    }
                    return null;
                }).collect(toList());
        downloadExecutorService.invokeAll(queryDownloadCallables);
    }

    private ReadContext downloadQueryListDocument(Snapshot snapshot, Environment environment)
    {
        String queryListJson = downloadPrestoFile(environment, QUERY_PATH);
        addDocument(snapshot, environment, QUERY_PATH, queryListJson);
        return JsonPath.parse(queryListJson);
    }

    private void downloadQueryDocument(Snapshot snapshot, Environment environment, String queryId)
    {
        String name = QUERY_PATH + "/" + queryId;
        addDocument(snapshot, environment, name, downloadPrestoFile(environment, name));
    }

    private void downloadQueryExecutionDocument(Snapshot snapshot, Environment environment, String queryId)
    {
        String name = QUERY_EXECUTION_PATH + "/" + queryId;
        addDocument(snapshot, environment, name, downloadPrestoFile(environment, name));
    }

    private String downloadPrestoFile(Environment environment, String path)
    {
        String url = environment.getPrestoUrl() + path;
        LOG.info("Downloading {},", url);
        return restTemplate.getForObject(environment.getPrestoUrl() + path, String.class);
    }

    private Snapshot createSnapshot()
    {
        Snapshot snapshot = new Snapshot();
        snapshot.setTimestamp(ZonedDateTime.now(systemUTC()));
        return snapshot;
    }

    private void addDocument(Snapshot snapshot, Environment environment, String name, String content)
    {
        Document document = new Document();
        document.setEnvironment(environment.getName());
        document.setName(name);
        document.setTimestamp(ZonedDateTime.now(systemUTC()));
        document.setContent(content);
        document.setSnapshot(snapshot);
    }
}
