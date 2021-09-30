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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.utils.AccessTokenHttpProvider;
import org.thingsboard.server.utils.WebsocketUtils;
import org.thingsboard.server.websocket.WebSocketClientImpl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public abstract class WebsocketBasedObserver {

    private final AccessTokenHttpProvider tokenHttpProvider;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${websocket.update_wait_time}")
    protected int websocketWaitTime;

    @Value("${websocket.host}")
    private String WS_URL;

    protected WebSocketClientImpl validateWebsocketClient(WebSocketClientImpl webSocketClient, UUID deviceToOpenSession)
            throws URISyntaxException, IOException, InterruptedException {
        if (webSocketClient == null || webSocketClient.isClosed()) {
            webSocketClient = buildAndConnectWebSocketClient();
            webSocketClient.send(mapper.writeValueAsString(WebsocketUtils.getTelemetryCmdsWrapper(deviceToOpenSession)));
            String value = webSocketClient.waitForReply(websocketWaitTime);
            Optional.ofNullable(value)
                    .orElseThrow(() -> new IllegalStateException( "Unable to open websocket session"));
            return webSocketClient;
        }
        return webSocketClient;
    }

    private WebSocketClientImpl buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException, IOException {
        String accessToken = Optional.ofNullable(tokenHttpProvider.getAccessToken())
                .orElseThrow(() -> new IOException("Unable to receive access token to establish websocket subscription"));
        URI serverUri = new URI(WS_URL + "/api/ws/plugins/telemetry?token=" + accessToken);
        WebSocketClientImpl webSocketClient = new WebSocketClientImpl(serverUri);
        webSocketClient.connectBlocking(websocketWaitTime, TimeUnit.MILLISECONDS);
        return webSocketClient;
    }
}
