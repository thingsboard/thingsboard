// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.monitoring.client;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.thingsboard.rest.client.RestClient;

import java.time.Duration;

@Component
public class TbClient extends RestClient {

    @Value("${monitoring.rest.username}")
    private String username;
    @Value("${monitoring.rest.password}")
    private String password;

    public TbClient(@Value("${monitoring.rest.base_url}") String baseUrl,
                    @Value("${monitoring.rest.request_timeout_ms}") int requestTimeoutMs) {
        super(new RestTemplateBuilder()
                .connectTimeout(Duration.ofMillis(requestTimeoutMs))
                .readTimeout(Duration.ofMillis(requestTimeoutMs))
                .build(), baseUrl);
    }

    @PostConstruct
    private void init() {
        logIn();
    }

    public String logIn() {
        login(username, password);
        return getToken();
    }

}
