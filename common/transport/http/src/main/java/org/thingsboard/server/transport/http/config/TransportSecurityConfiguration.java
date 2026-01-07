/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.http.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class TransportSecurityConfiguration {
    public static final String DEVICE_API_ENTRY_POINT = "/api/v1/**";

    @Value("${transport.http.max_payload_size:/api/v1/*/rpc/**=65536;/api/v1/**=52428800}")
    private String maxPayloadSizeConfig;

    @Bean
    protected PayloadSizeFilter transportPayloadSizeFilter() {
        return new PayloadSizeFilter(maxPayloadSizeConfig);
    }

    @Bean
    @Order(1)
    SecurityFilterChain httpTransportFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(AntPathRequestMatcher.antMatcher(DEVICE_API_ENTRY_POINT))
                .cors(cors -> {
                })
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(config -> config
                        .requestMatchers(DEVICE_API_ENTRY_POINT).permitAll())
                .addFilterBefore(transportPayloadSizeFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
