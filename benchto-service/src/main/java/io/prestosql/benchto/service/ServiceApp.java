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

import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import io.prestosql.benchto.service.rest.converters.ZonedDateTimeConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import static com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module.Feature.USE_TRANSIENT_ANNOTATION;

@EnableScheduling
@EnableRetry
@SpringBootApplication
public class ServiceApp
{
    public static void main(String[] args)
    {
        SpringApplication.run(ServiceApp.class, args);
    }

    @Bean
    public Jackson2ObjectMapperBuilder configureObjectMapper()
    {
        Hibernate4Module hibernate4Module = new Hibernate4Module();
        hibernate4Module.disable(USE_TRANSIENT_ANNOTATION);
        return new Jackson2ObjectMapperBuilder()
                .modulesToInstall(hibernate4Module);
    }

    @Bean
    public ZonedDateTimeConverter zonedDateTimeConverter()
    {
        return new ZonedDateTimeConverter();
    }
}
