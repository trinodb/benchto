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

import io.prestosql.benchto.service.category.IntegrationTest;
import io.prestosql.benchto.service.repo.EnvironmentRepo;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;

import static io.prestosql.benchto.service.utils.TimeUtils.currentDateTime;
import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Category(IntegrationTest.class)
public class TagControllerTest
        extends IntegrationTestBase
{
    @Autowired
    private EnvironmentRepo environmentRepo;

    @Test
    public void tagsHappyPath()
            throws Exception
    {
        ZonedDateTime start = currentDateTime();
        // create env
        mvc.perform(post("/v1/environment/env")
                .contentType(APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());

        // create tag1
        mvc.perform(post("/v1/tag/env")
                .contentType(APPLICATION_JSON)
                .content(storeTagRequest("tag1", "description for tag1")))
                .andExpect(status().isOk());

        ZonedDateTime afterTag1 = currentDateTime();

        // create tag2
        mvc.perform(post("/v1/tag/env")
                .contentType(APPLICATION_JSON)
                .content(storeTagRequest("tag2 - no description")))
                .andExpect(status().isOk());

        // create tag3
        mvc.perform(post("/v1/tag/env")
                .contentType(APPLICATION_JSON)
                .content(storeTagRequest("tag3", "description for tag3")))
                .andExpect(status().isOk());

        // get all tags
        mvc.perform(get("/v1/tags/env")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("tag1")))
                .andExpect(jsonPath("$[0].description", is("description for tag1")))
                .andExpect(jsonPath("$[1].name", is("tag2 - no description")))
                .andExpect(jsonPath("$[1].description", isEmptyOrNullString()))
                .andExpect(jsonPath("$[2].name", is("tag3")))
                .andExpect(jsonPath("$[2].description", is("description for tag3")));

        // get tag2, get3
        mvc.perform(get("/v1/tags/env")
                .contentType(APPLICATION_JSON)
                .content("{\"start\": \"" + afterTag1.format(ISO_ZONED_DATE_TIME) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("tag2 - no description")))
                .andExpect(jsonPath("$[0].description", isEmptyOrNullString()))
                .andExpect(jsonPath("$[1].name", is("tag3")))
                .andExpect(jsonPath("$[1].description", is("description for tag3")));

        // get tag1
        mvc.perform(get("/v1/tags/env")
                .contentType(APPLICATION_JSON)
                .content("{\"start\": \"" + start.format(ISO_ZONED_DATE_TIME) + "\", \"end\": \"" + afterTag1.format(ISO_ZONED_DATE_TIME) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("tag1")))
                .andExpect(jsonPath("$[0].description", is("description for tag1")));

        // get latest tag
        mvc.perform(get("/v1/tags/env/latest")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("tag3")))
                .andExpect(jsonPath("$.description", is("description for tag3")));

        // get latest tag but not older than afterTat1 timestamp
        mvc.perform(get("/v1/tags/env/latest?until={until}", afterTag1.toInstant().toEpochMilli())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("tag1")))
                .andExpect(jsonPath("$.description", is("description for tag1")));

        // get latest tag but not older than start timestamp
        mvc.perform(get("/v1/tags/env/latest?until={until}", start.toInstant().toEpochMilli())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(result -> jsonPath("$.name", is("tag1")))
                .andExpect(content().string(""));
    }

    private String storeTagRequest(String tag)
    {
        return "{\"name\": \"" + tag + "\"}";
    }

    private String storeTagRequest(String tag, String description)
    {
        return "{\"name\": \"" + tag + "\", \"description\":\"" + description + "\"}";
    }
}
