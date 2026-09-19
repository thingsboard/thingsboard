/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.monitoring.client;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

// a wrong base_url -> path combination fails quietly server-side (exact string match on /api/ws),
// so this connects to a real embedded server rather than asserting on a URI field
class WsClientFactoryTest {

    private PathRecordingWsServer server;
    private WsClient client;

    @AfterEach
    void tearDown() throws Exception {
        if (client != null && client.isOpen()) {
            client.closeBlocking();
        }
        if (server != null) {
            server.stop(1000);
        }
    }

    @Test
    void createClient_connectsToApiWsPathBuiltFromBaseUrl() throws Exception {
        server = new PathRecordingWsServer();
        server.start();
        server.awaitStart();

        TbClient tbClient = mock(TbClient.class);
        doReturn(TbClient.AuthMode.API_KEY).when(tbClient).getAuthMode();

        WsClientFactory factory = new WsClientFactory(tbClient, mock(MonitoringReporter.class), new TbStopWatch());
        ReflectionTestUtils.setField(factory, "baseUrl", "ws://localhost:" + server.getPort());
        ReflectionTestUtils.setField(factory, "requestTimeoutMs", 2000);

        client = factory.createClient("my-api-key");

        assertThat(server.awaitPath()).isEqualTo("/api/ws");
    }

    private static class PathRecordingWsServer extends WebSocketServer {

        private final BlockingQueue<String> paths = new LinkedBlockingQueue<>();
        private final BlockingQueue<Boolean> started = new LinkedBlockingQueue<>();

        PathRecordingWsServer() {
            super(new InetSocketAddress("localhost", 0));
        }

        void awaitStart() throws InterruptedException {
            if (started.poll(5, TimeUnit.SECONDS) == null) {
                throw new IllegalStateException("Embedded WS server did not start in time");
            }
        }

        String awaitPath() throws InterruptedException {
            String path = paths.poll(5, TimeUnit.SECONDS);
            if (path == null) {
                throw new IllegalStateException("No connection received by the embedded WS server in time");
            }
            return path;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            paths.add(handshake.getResourceDescriptor());
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
        }

        @Override
        public void onStart() {
            started.add(true);
        }

    }

}
