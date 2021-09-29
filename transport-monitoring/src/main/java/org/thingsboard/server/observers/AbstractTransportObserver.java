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
package org.thingsboard.server.observers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.TransportsMonitoringScheduler;
import org.thingsboard.server.transport.TransportInfo;
import org.thingsboard.server.transport.TransportObserver;
import org.thingsboard.server.websocket.WebSocketClientImpl;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.telemetry.cmd.LatestValueCmd;
import org.thingsboard.server.common.data.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.common.data.telemetry.wrapper.TelemetryPluginCmdsWrapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractTransportObserver implements TransportObserver {

    protected final ObjectMapper mapper = new ObjectMapper();
    protected String actualAccessToken;

    @Value("${websocket.update_wait_time}")
    protected int websocketWaitTime;

    @Value("${websocket.host}")
    private String WS_URL;

    @Value("${websocket.monitoring_tenant_username}")
    private String monitoringTenantUsername;

    @Value("${websocket.monitoring_tenant_password}")
    private String monitoringTenantPassword;

    @Value("${websocket.token_host}")
    private String tokenHost;

    private final String UNABLE_RECEIVE_ACCESS_TOKEN_MSG = "Unable to receive access token to perform websocket subscription";

    protected WebSocketClientImpl validateWebsocketClient(WebSocketClientImpl webSocketClient, UUID deviceToOpenSession)
            throws URISyntaxException, IOException, InterruptedException {
        if (webSocketClient == null || webSocketClient.isClosed()) {
            webSocketClient = buildAndConnectWebSocketClient();
            webSocketClient.send(mapper.writeValueAsString(getTelemetryCmdsWrapper(deviceToOpenSession)));
            Optional.ofNullable(webSocketClient.waitForReply(websocketWaitTime))
                    .orElseThrow(() -> new IllegalStateException(String.valueOf(new TransportInfo(getTransportType(), "Unable to open websocket session"))));
            return webSocketClient;
        }
        return webSocketClient;
    }

    protected TelemetryPluginCmdsWrapper getTelemetryCmdsWrapper(UUID uuid) {
        DeviceId deviceId = new DeviceId(uuid);
        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(deviceId);
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, TransportsMonitoringScheduler.PAYLOAD_KEY_STR)));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));
        return wrapper;
    }

    protected WebSocketClientImpl buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException, IOException {
        String accessToken = getAccessToken().orElseThrow(() -> new IOException(UNABLE_RECEIVE_ACCESS_TOKEN_MSG));
        URI serverUri = new URI(WS_URL + "/api/ws/plugins/telemetry?token=" + accessToken);
        WebSocketClientImpl webSocketClient = new WebSocketClientImpl(serverUri);
        webSocketClient.connectBlocking(websocketWaitTime, TimeUnit.MILLISECONDS);
        return webSocketClient;
    }

    protected Optional<String> getAccessToken() {
        try {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(websocketWaitTime)
                    .setConnectionRequestTimeout(websocketWaitTime)
                    .setSocketTimeout(websocketWaitTime).build();
            CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

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
                actualAccessToken = token;
                return Optional.of(token);
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error(e.toString());
        }
        return Optional.empty();
    }

}
