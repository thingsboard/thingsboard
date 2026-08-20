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
package org.thingsboard.monitoring.client;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.User;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class TbClient extends RestClient {

    // separate from RestClient.AuthType: this also drives the WS auth path (see WsClient#authenticate), which RestClient doesn't know about
    public enum AuthMode {LOGIN, API_KEY}

    private final AuthMode authMode;
    private final String apiKey;
    private final String username;
    private final String password;

    public TbClient(@Value("${monitoring.rest.base_url}") String baseUrl,
                    @Value("${monitoring.rest.request_timeout_ms}") int requestTimeoutMs,
                    @Value("${monitoring.rest.auth_mode:LOGIN}") AuthMode authMode,
                    @Value("${monitoring.rest.api_key:}") String apiKey,
                    @Value("${monitoring.rest.username:}") String username,
                    @Value("${monitoring.rest.password:}") String password) {
        super(new RestTemplateBuilder()
                .connectTimeout(Duration.ofMillis(requestTimeoutMs))
                .readTimeout(Duration.ofMillis(requestTimeoutMs))
                .build(), baseUrl,
                authMode == AuthMode.API_KEY ? AuthType.API_KEY : AuthType.JWT,
                authMode == AuthMode.API_KEY ? apiKey : null);
        this.authMode = authMode;
        this.apiKey = apiKey;
        this.username = username;
        this.password = password;
        if (authMode == AuthMode.API_KEY && StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("monitoring.rest.api_key must be set when monitoring.rest.auth_mode is API_KEY");
        }
        if (authMode == AuthMode.LOGIN && (StringUtils.isBlank(username) || StringUtils.isBlank(password))) {
            throw new IllegalStateException("monitoring.rest.username and monitoring.rest.password must be set when monitoring.rest.auth_mode is LOGIN");
        }
        log.info("Starting TbClient with auth mode: {}", authMode);
    }

    @PostConstruct
    private void init() {
        getWsCredential();
    }

    public AuthMode getAuthMode() {
        return authMode;
    }

    // returns whatever WsClient#authenticate needs to hand the server next: a fresh JWT in LOGIN
    // mode, or the api key itself in API_KEY mode - which has no login step, so this just proves
    // the key works via getUser() and returns it as-is
    public String getWsCredential() {
        if (authMode == AuthMode.API_KEY) {
            Optional<User> user = getUser();
            if (user.isEmpty()) {
                throw new IllegalStateException("API key authentication failed - no user returned");
            }
            return apiKey;
        }
        login(username, password);
        return getToken();
    }

}
