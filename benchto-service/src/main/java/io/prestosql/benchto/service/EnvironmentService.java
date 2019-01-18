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
package io.prestosql.benchto.service;

import io.prestosql.benchto.service.model.Environment;
import io.prestosql.benchto.service.repo.EnvironmentRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.prestosql.benchto.service.utils.TimeUtils.currentDateTime;
import static java.util.Optional.ofNullable;

@Service
public class EnvironmentService
{
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentService.class);

    @Autowired
    private EnvironmentRepo environmentRepo;

    @Retryable(value = {TransientDataAccessException.class, DataIntegrityViolationException.class})
    @Transactional
    public void storeEnvironment(String name, Map<String, String> attributes)
    {
        Optional<Environment> environmentOptional = tryFindEnvironment(name);
        if (!environmentOptional.isPresent()) {
            Environment environment = new Environment();
            environment.setName(name);
            environment.setAttributes(attributes);
            environment.setStarted(currentDateTime());
            environmentOptional = Optional.of(environmentRepo.save(environment));
        }
        else {
            environmentOptional.get().setAttributes(attributes);
        }
        LOG.debug("Starting environment - {}", environmentOptional.get());
    }

    @Transactional(readOnly = true)
    public Environment findEnvironment(String name)
    {
        Optional<Environment> environment = tryFindEnvironment(name);
        if (!environment.isPresent()) {
            throw new IllegalArgumentException("Could not find environment " + name);
        }
        return environment.get();
    }

    @Transactional(readOnly = true)
    public Optional<Environment> tryFindEnvironment(String name)
    {
        return ofNullable(environmentRepo.findByName(name));
    }

    public List<Environment> findEnvironments()
    {
        return environmentRepo.findAll();
    }
}
