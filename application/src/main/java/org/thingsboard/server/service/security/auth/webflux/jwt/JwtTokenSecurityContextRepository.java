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
package org.thingsboard.server.service.security.auth.webflux.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.thingsboard.server.config.ThingsboardSecurityConfiguration;
import org.thingsboard.server.service.security.auth.JwtAuthenticationToken;
import org.thingsboard.server.service.security.auth.jwt.extractor.TokenExtractor;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtTokenSecurityContextRepository implements ServerSecurityContextRepository {

    public static final String DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME = "SPRING_SECURITY_CONTEXT";

    @Autowired
    private ReactiveAuthenticationManager webfluxAuthenticationManager;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return exchange.getSession()
                .doOnNext(session -> {
                    if (context == null) {
                        session.getAttributes().remove(WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME);
                    } else {
                        session.getAttributes().put(WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME, context);
                    }
                })
                .flatMap(session -> session.changeSessionId());
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String token = extractTokenFromQuery(request);
        if (!StringUtils.isEmpty(token)) {
            RawAccessJwtToken rawToken = new RawAccessJwtToken(token);
            Authentication auth = new JwtAuthenticationToken(rawToken);
            return this.webfluxAuthenticationManager.authenticate(auth).map((authentication) -> {
                return new SecurityContextImpl(authentication);
            });
        } else {
            return Mono.empty();
        }
    }

    private String extractTokenFromQuery(ServerHttpRequest request) {
        String token = null;
        if (request.getQueryParams() != null) {
            List<String> tokenParamValue = request.getQueryParams().get(ThingsboardSecurityConfiguration.JWT_TOKEN_QUERY_PARAM);
            if (tokenParamValue != null && !tokenParamValue.isEmpty()) {
                token = tokenParamValue.get(0);
            }
        }
        return token;
    }

}
