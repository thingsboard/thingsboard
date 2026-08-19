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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.User;

import java.time.Duration;
import java.util.Optional;

@Component
public class TbClient extends RestClient {

    public enum AuthMode {LOGIN, API_KEY}

    private final AuthMode authMode;
    private final String apiKey;

    @Value("${monitoring.rest.username:}")
    private String username;
    @Value("${monitoring.rest.password:}")
    private String password;

    public TbClient(@Value("${monitoring.rest.base_url}") String baseUrl,
                    @Value("${monitoring.rest.request_timeout_ms}") int requestTimeoutMs,
                    @Value("${monitoring.rest.auth_mode:LOGIN}") AuthMode authMode,
                    @Value("${monitoring.rest.api_key:}") String apiKey) {
        super(new RestTemplateBuilder()
                .connectTimeout(Duration.ofMillis(requestTimeoutMs))
                .readTimeout(Duration.ofMillis(requestTimeoutMs))
                .build(), baseUrl,
                authMode == AuthMode.API_KEY ? AuthType.API_KEY : AuthType.JWT,
                authMode == AuthMode.API_KEY ? apiKey : null);
        this.authMode = authMode;
        this.apiKey = apiKey;
    }

    @PostConstruct
    private void init() {
        logIn();
    }

    public AuthMode getAuthMode() {
        return authMode;
    }

    // for LOGIN, performs the real username/password login and returns the resulting JWT.
    // for API_KEY, there is no login step - proves the key still works via a lightweight
    // authenticated call instead, and returns the static key itself (nothing else to return).
    public String logIn() {
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
