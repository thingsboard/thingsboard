package org.thingsboard.server.observers;

import org.thingsboard.server.utils.AccessTokenHttpProvider;
import org.thingsboard.server.transport.TransportType;
import org.thingsboard.server.websocket.WebSocketClientImpl;

import java.util.UUID;

public abstract class AbstractTransportObserver extends WebsocketBasedObserver {

    private WebSocketClientImpl webSocketClient;

    public AbstractTransportObserver(AccessTokenHttpProvider tokenHttpProvider) {
        super(tokenHttpProvider);
    }

    public String pingTransport(String payload) throws Exception {
        webSocketClient = validateWebsocketClient(webSocketClient, getTestDeviceUuid());
        webSocketClient.registerWaitForUpdate();
        publishMsg(payload);
        return webSocketClient.waitForUpdate(websocketWaitTime);
    }

    protected abstract void publishMsg(String payload) throws Exception;

    public abstract UUID getTestDeviceUuid();
    public abstract int getMonitoringRate();
    public abstract TransportType getTransportType();
}
