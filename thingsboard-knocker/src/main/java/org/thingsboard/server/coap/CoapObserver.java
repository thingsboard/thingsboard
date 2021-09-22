package org.thingsboard.server.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.AbstractTransportObserver;
import org.thingsboard.server.TransportObserver;
import org.thingsboard.server.TransportType;
import org.eclipse.californium.core.CoapClient;
import org.thingsboard.server.WebSocketClientImpl;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.mapper;

@Component
@Slf4j
public class CoapObserver extends AbstractTransportObserver {

    private CoapClient coapClient;
    private WebSocketClientImpl webSocketClient;

    @Value("${coap.url}")
    private String coapBaseUrl;

    @Value("${coap.monitoring_rate}")
    private int monitoringRate;

    @Value("${coap.test_device.id}")
    private UUID testDeviceUuid;

    @Value("${coap.test_device.access_token}")
    private String testDeviceAccessToken;

    @Value("${coap.timeout}")
    private long timeout;

    @PostConstruct
    private void init() {
        try {
            coapClient = new CoapClient(coapBaseUrl + "/api/v1/" + testDeviceAccessToken + "/" + "telemetry");

            webSocketClient = buildAndConnectWebSocketClient();
            webSocketClient.send(mapper.writeValueAsString(getTelemetryCmdsWrapper(testDeviceUuid)));
            String s = webSocketClient.waitForReply(websocketWaitTime);
            System.out.println(s);
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    @Override
    public String pingTransport(String payload) throws Exception {
        webSocketClient.registerWaitForUpdate();
        postTelemetry(coapClient, payload.getBytes());
        return webSocketClient.waitForUpdate(timeout);
    }

    @Override
    public int getMonitoringRate() {
        return monitoringRate;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.COAP;
    }

    private void postTelemetry(CoapClient client, byte[] payload) throws IOException, ConnectorException {
        CoapResponse response = client.setTimeout(timeout).post(payload, MediaTypeRegistry.APPLICATION_JSON);
    }

}
