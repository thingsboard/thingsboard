// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
