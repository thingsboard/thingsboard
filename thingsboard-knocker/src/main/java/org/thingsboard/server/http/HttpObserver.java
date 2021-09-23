package org.thingsboard.server.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.AbstractTransportObserver;
import org.thingsboard.server.TransportType;
import org.thingsboard.server.WebSocketClientImpl;

import javax.annotation.PostConstruct;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

@Component
@Slf4j
public class HttpObserver extends AbstractTransportObserver {

    private WebSocketClientImpl webSocketClient;
    private CloseableHttpClient httpClient;

    @Value("${http.host}")
    private String host;

    @Value("${http.monitoring_rate}")
    private int monitoringRate;

    @Value("${http.test_device.access_token}")
    private String testDeviceAccessToken;

    @Value("${http.response_timeout}")
    private int responseTimeout;

    @Value("${http.connect_timeout}")
    private int connectTimeout;

    @Value("${http.connection_request_timeout}")
    private int connectionRequestTimeout;

    @Value("${http.socket_timeout}")
    private int socketTimeout;

    @Value("${http.test_device.id}")
    private UUID testDeviceUuid;

    public HttpObserver() {
        super();
    }

    @Override
    public String pingTransport(String payload) throws Exception {
        if (httpClient == null) {
            httpClient = buildHttpClient();
        }
        webSocketClient = validateWebsocketClient(webSocketClient, testDeviceUuid);

        webSocketClient.registerWaitForUpdate();
        sendHttpPostWithTimeout(payload);
        return webSocketClient.waitForUpdate(websocketWaitTime);
    }

    private void sendHttpPostWithTimeout(String payload) throws Exception {
        String uri = host + "/api/v1/" + testDeviceAccessToken + "/telemetry";
        HttpPost httpPost = new HttpPost(uri);
        StringEntity entity = new StringEntity(payload);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                httpPost.abort();
            }
        };
        new Timer(true).schedule(task, responseTimeout);
        httpClient.execute(httpPost);
    }

    @Override
    public int getMonitoringRate() {
        return monitoringRate;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.HTTP;
    }

    private CloseableHttpClient buildHttpClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout )
                .setSocketTimeout(socketTimeout).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }
}
