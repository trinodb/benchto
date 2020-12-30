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
package io.trino.benchto.service.rest;

import io.trino.benchto.service.TagService;
import io.trino.benchto.service.model.Tag;
import io.trino.benchto.service.rest.requests.GetTagsRequest;
import io.trino.benchto.service.rest.requests.StoreTagRequest;
import io.trino.benchto.service.utils.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class TagController
{
    @Autowired
    private TagService service;

    @RequestMapping(value = "/v1/tag/{environmentName}", method = POST)
    public void store(@PathVariable("environmentName") String environmentName, @RequestBody StoreTagRequest request)
    {
        service.store(environmentName, request.getName(), request.getDescription());
    }

    @RequestMapping(value = "/v1/tags/{environmentName}", method = GET)
    public List<Tag> find(
            @PathVariable("environmentName") String environmentName,
            @RequestBody(required = false) GetTagsRequest request)
    {
        if (request != null && request.getStart().isPresent()) {
            return service.find(
                    environmentName,
                    request.getStart().get(),
                    request.getEnd().orElse(TimeUtils.currentDateTime()));
        }
        return service.find(environmentName);
    }

    @RequestMapping(value = "/v1/tags/{environmentName}/latest", method = GET)
    public Tag find(
            @PathVariable("environmentName") String environmentName,
            @RequestParam(required = false) ZonedDateTime until)
    {
        if (until == null) {
            until = TimeUtils.currentDateTime();
        }
        return service.latest(environmentName, until).orElse(null);
    }
}
