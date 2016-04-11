/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.service;

import com.teradata.benchto.service.model.Tag;
import com.teradata.benchto.service.repo.TagRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

import static com.teradata.benchto.service.utils.TimeUtils.currentDateTime;

@Service
public class TagService
{
    private static final Logger LOG = LoggerFactory.getLogger(TagService.class);

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private TagRepo repo;

    @Retryable(value = {TransientDataAccessException.class, DataIntegrityViolationException.class}, maxAttempts = 1)
    @Transactional
    public void store(String environmentName, String tag, String description)
    {
        Tag entity = new Tag();
        entity.setEnvironment(environmentService.findEnvironment(environmentName));
        entity.setName(tag);
        entity.setDescription(description);
        entity.setCreated(currentDateTime());
        entity = repo.save(entity);
        LOG.info("Storing new tag - {}", entity);
    }

    @Transactional(readOnly = true)
    public List<Tag> find(String environmentName)
    {
        return repo.find(environmentService.findEnvironment(environmentName));
    }

    @Transactional(readOnly = true)
    public List<Tag> find(String environmentName, ZonedDateTime start, ZonedDateTime end)
    {
        return repo.find(environmentService.findEnvironment(environmentName), start, end);
    }
}
