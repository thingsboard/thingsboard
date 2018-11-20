/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
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

package org.thingsboard.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;

//@EnableWebFluxSecurity
//@EnableReactiveMethodSecurity
public class ThingsboardWebFluxSecurityConfig {

    private static final String WS_TOKEN_BASED_AUTH_ENTRY_POINT = "/api/ws/**";

    @Autowired
    private ReactiveAuthenticationManager webfluxAuthenticationManager;

    @Autowired
    private ServerSecurityContextRepository jwtTokenSecurityContextRepository;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors()
                .and()
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .exceptionHandling()
                .and()
                .authenticationManager(webfluxAuthenticationManager)
                .securityContextRepository(jwtTokenSecurityContextRepository)
                .authorizeExchange()
                .pathMatchers(WS_TOKEN_BASED_AUTH_ENTRY_POINT)
                .authenticated()
                .and()
                .build();
    }

}
