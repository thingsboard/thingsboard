package org.thingsboard.server.snmp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.AbstractTransportObserver;
import org.thingsboard.server.TransportObserver;
import org.thingsboard.server.TransportType;
import org.thingsboard.server.WebSocketClientImpl;

import java.util.UUID;

@Component
@ConditionalOnProperty(
        value="snmp.enabled",
        havingValue = "true")
public class SnmpObserver extends AbstractTransportObserver {

    private WebSocketClientImpl webSocketClient;

    @Value("${snmp.monitoring_rate}")
    private int monitoringRate;

    @Value("${snmp.test_device.id}")
    private UUID testDeviceUuid;

    @Override
    public String pingTransport(String payload) throws Exception {
        webSocketClient = validateWebsocketClient(webSocketClient, testDeviceUuid);
        webSocketClient.registerWaitForUpdate();

        return webSocketClient.waitForUpdate(1000);
    }

    @Override
    public int getMonitoringRate() {
        return monitoringRate;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.SNMP;
    }
}
