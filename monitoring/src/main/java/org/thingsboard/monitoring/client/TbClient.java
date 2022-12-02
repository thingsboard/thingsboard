/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.rest.client.RestClient;

import java.time.Duration;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TbClient extends RestClient {

    @Value("${monitoring.auth.username}")
    private String username;
    @Value("${monitoring.auth.password}")
    private String password;

    public TbClient(@Value("${monitoring.auth.base_url}") String baseUrl,
                    @Value("${monitoring.rest_request_timeout_ms}") int requestTimeoutMs) {
        super(new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(requestTimeoutMs))
                .setReadTimeout(Duration.ofMillis(requestTimeoutMs))
                .build(), baseUrl);
    }

    public String logIn() {
        login(username, password);
        return getToken();
    }

}
