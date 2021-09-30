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
