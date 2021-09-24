package org.thingsboard.server.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.AbstractTransportObserver;
import org.thingsboard.server.TransportType;
import org.thingsboard.server.WebSocketClientImpl;

import java.io.IOException;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        value="coap.enabled",
        havingValue = "true")
@Slf4j
public class CoapObserver extends AbstractTransportObserver {

    private CoapClient coapClient;
    private WebSocketClientImpl webSocketClient;

    @Value("${coap.host}")
    private String coapBaseUrl;

    @Value("${coap.monitoring_rate}")
    private int monitoringRate;

    @Value("${coap.test_device.id}")
    private UUID testDeviceUuid;

    @Value("${coap.test_device.access_token}")
    private String testDeviceAccessToken;

    @Value("${coap.timeout}")
    private long timeout;

    @Override
    public String pingTransport(String payload) throws Exception {
        if (coapClient == null) {
            coapClient = new CoapClient(coapBaseUrl + "/api/v1/" + testDeviceAccessToken + "/" + "telemetry");
        }
        webSocketClient = validateWebsocketClient(webSocketClient, testDeviceUuid);

        webSocketClient.registerWaitForUpdate();
        postCoapTelemetry(coapClient, payload.getBytes());
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

    private void postCoapTelemetry(CoapClient client, byte[] payload) throws IOException, ConnectorException {
        CoapResponse response = client.setTimeout(timeout).post(payload, MediaTypeRegistry.APPLICATION_JSON);
    }

}
