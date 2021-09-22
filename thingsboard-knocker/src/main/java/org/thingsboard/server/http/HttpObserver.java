package org.thingsboard.server.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.AbstractTransportObserver;
import org.thingsboard.server.TransportObserver;
import org.thingsboard.server.TransportType;
import org.thingsboard.server.WebSocketClientImpl;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

@Component
@Slf4j
public class HttpObserver extends AbstractTransportObserver {

    private WebSocketClientImpl webSocketClient;
    private CloseableHttpClient httpClient;


    @Value("${http.monitoring_rate}")
    private int monitoringRate;

    @Value("${http.test_device.access_token}")
    private String testDeviceAccessToken;

    @Value("${http.timeout}")
    private int timeout;

    @Value("${http.test_device.id}")
    private UUID testDeviceUuid;

    public HttpObserver() {
        super();
    }

    @PostConstruct
    private void init() {
        try {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout )
                    .setSocketTimeout(timeout).build();
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
            webSocketClient = buildAndConnectWebSocketClient();
            webSocketClient.send(mapper.writeValueAsString(getTelemetryCmdsWrapper(testDeviceUuid)));
            webSocketClient.waitForReply(websocketWaitTime);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String pingTransport(String payload) throws Exception {
        if (webSocketClient == null || webSocketClient.isClosed()) {
            webSocketClient = buildAndConnectWebSocketClient();
        }

        webSocketClient.registerWaitForUpdate();
        sendHttpPostWithTimeout(payload);
        return webSocketClient.waitForUpdate(websocketWaitTime);
    }

    private void sendHttpPostWithTimeout(String payload) throws Exception {
        String uri = "http://localhost:8080/api/v1/" + testDeviceAccessToken + "/telemetry";
        HttpPost httpPost = new HttpPost(uri);
        StringEntity entity = new StringEntity(payload);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        // TODO: 22.09.21 Different yml configs
        int hardTimeout = timeout;
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                httpPost.abort();
            }
        };
        new Timer(true).schedule(task, hardTimeout);
        httpClient.execute(httpPost);
    }

    @Override
    public int getMonitoringRate() {
        return monitoringRate;
    }

    @Override
    public TransportType getTransportType(String msg) {
        TransportType mqtt = TransportType.HTTP;
        mqtt.setInfo(msg);
        return mqtt;
    }
}
