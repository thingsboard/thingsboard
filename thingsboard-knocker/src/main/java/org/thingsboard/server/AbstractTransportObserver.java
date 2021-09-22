package org.thingsboard.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractTransportObserver implements TransportObserver {

    protected final ObjectMapper mapper = new ObjectMapper();

    private final String WS_URL = "ws://localhost:8080";

    @Value("${websocket.monitoring_tenant_username}")
    private String monitoringTenantUsername;

    @Value("${websocket.monitoring_tenant_password}")
    private String monitoringTenantPassword;

    protected String actualAccessToken;

    @Value("${websocket.wait_time}")
    protected int websocketWaitTime;

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

    protected WebSocketClientImpl buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        URI serverUri = new URI(WS_URL + "/api/ws/plugins/telemetry?token=" + getAccessToken());
        WebSocketClientImpl webSocketClient = new WebSocketClientImpl(serverUri);
        boolean b = webSocketClient.connectBlocking(websocketWaitTime, TimeUnit.MILLISECONDS);
        log.info(String.valueOf(b));
        return webSocketClient;
    }

    protected String getAccessToken() {
        try {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(websocketWaitTime)
                    .setConnectionRequestTimeout(websocketWaitTime )
                    .setSocketTimeout(websocketWaitTime).build();
            CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

            String uri = "http://localhost:8080/api/auth/login";
            HttpPost httpPost = new HttpPost(uri);

            Map<String, String> map = new HashMap<>();
            map.put("username", monitoringTenantUsername);
            map.put("password", monitoringTenantPassword);

            StringEntity entity  = new StringEntity(mapper.writeValueAsString(map));
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
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

}
