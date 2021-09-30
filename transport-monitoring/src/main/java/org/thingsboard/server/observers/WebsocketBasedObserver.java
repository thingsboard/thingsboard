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
            System.out.println(value);
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
