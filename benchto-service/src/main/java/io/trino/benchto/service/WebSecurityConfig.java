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
package io.trino.benchto.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig
{
    private static final String API_WRITE_ROLE = "API_WRITE_ROLE";

    @Value("${benchto.security.api.protected:false}")
    private boolean isApiProtected;

    @Value("${benchto.security.api.login:}")
    private String userLogin;

    @Value("${benchto.security.api.password:}")
    private String userPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception
    {
        if (isApiProtected) {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeRequests()
                    .requestMatchers("/**")
                    .permitAll()
                    .and()
                    .authorizeRequests()
                    .requestMatchers((httpServletRequest) -> httpServletRequest.getMethod().equalsIgnoreCase("POST"))
                    .hasAnyRole(API_WRITE_ROLE)
                    .and()
                    .httpBasic(withDefaults());
        }
        else {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeRequests()
                    .anyRequest()
                    .permitAll();
        }

        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth)
            throws Exception
    {
        if (isApiProtected) {
            auth
                    .inMemoryAuthentication()
                    .withUser(userLogin).password(userPassword).roles(API_WRITE_ROLE);
        }
    }
}
