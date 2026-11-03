// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Order(SecurityProperties.BASIC_AUTH_ORDER)
@ConditionalOnExpression("'${service.type:null}'=='tb-rule-engine'")
public class TbRuleEngineSecurityConfiguration {

    @Autowired
    private HttpSecurityHeadersCustomizer httpSecurityHeadersCustomizer;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> {
                    headers.defaultsDisabled();
                    headers.cacheControl(config -> {});
                    httpSecurityHeadersCustomizer.customize(headers);
                })
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(config -> config
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
