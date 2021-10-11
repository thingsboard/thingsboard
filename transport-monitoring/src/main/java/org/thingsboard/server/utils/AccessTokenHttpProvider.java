/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessTokenHttpProvider {

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${websocket.token.host}")
    private String tokenHost;

    @Value("${websocket.token.request_timeout}")
    private int timeout;

    @Value("${websocket.monitoring_tenant_username}")
    private String monitoringTenantUsername;

    @Value("${websocket.monitoring_tenant_password}")
    private String monitoringTenantPassword;

    private CloseableHttpClient httpClient;

    @PostConstruct
    private void init() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout).build();
        httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    public String getAccessToken() {
        try {
            String uri = tokenHost + "/api/auth/login";
            HttpPost httpPost = new HttpPost(uri);

            Map<String, String> map = new HashMap<>();
            map.put("username", monitoringTenantUsername);
            map.put("password", monitoringTenantPassword);

            StringEntity entity = new StringEntity(mapper.writeValueAsString(map));
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
            JsonNode jsonNode = mapper.readValue(responseString, JsonNode.class);
            if (jsonNode.has("token")) {
                String token = jsonNode.get("token").asText();
                log.info("Token received: {}", token);
                return token;
            }
        } catch (IOException e) {
            log.error(e.toString());
        }
        return null;
    }
}
