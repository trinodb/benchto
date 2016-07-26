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
package com.teradata.benchto.service;

import com.teradata.benchto.service.model.Tag;
import com.teradata.benchto.service.repo.TagRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.teradata.benchto.service.utils.TimeUtils.currentDateTime;

@Service
public class TagService
{
    private static final Logger LOG = LoggerFactory.getLogger(TagService.class);

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private TagRepo repo;

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

    public Optional<Tag> latest(String environmentName, ZonedDateTime until)
    {
        List<Tag> latest = repo.latest(environmentService.findEnvironment(environmentName), until, new PageRequest(0, 1));
        if (latest.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(latest.get(0));
    }
}
