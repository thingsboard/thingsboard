/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth.webflux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.thingsboard.server.service.security.auth.JwtAuthenticationToken;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;
import reactor.core.publisher.Mono;

@Component
public class WebfluxAuthenticationManager implements ReactiveAuthenticationManager {

    @Autowired
    private JwtTokenFactory tokenFactory;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        try {
            if (authentication.getCredentials() != null && authentication.getCredentials() instanceof RawAccessJwtToken) {
                RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
                SecurityUser securityUser = tokenFactory.parseAccessJwtToken(rawAccessToken);
                JwtAuthenticationToken auth = new JwtAuthenticationToken(securityUser);
                return Mono.just(auth);
            }
            return Mono.empty();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
