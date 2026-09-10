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
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.data.cmd.CmdsWrapper;
import org.thingsboard.monitoring.data.cmd.EntityDataCmd;
import org.thingsboard.monitoring.data.cmd.EntityDataUpdate;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class WsClientTest {

    private RecordingWsServer server;
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

    private WsClient newClient() throws Exception {
        WsClient client = spy(new WsClient(new URI("ws://localhost:8080/api/ws"), 1000));
        doNothing().when(client).send(anyString());
        return client;
    }

    @Test
    void authenticate_apiKeyMode_sendsAuthCmdWithApiKeySet() throws Exception {
        WsClient client = newClient();

        client.authenticate(TbClient.AuthMode.API_KEY, "my-api-key");

        String json = captureSentJson(client);
        // must be omitted, not null-valued - some servers reject "apiKey":null outright
        assertThat(json).contains("\"apiKey\"");
        assertThat(json).doesNotContain("\"token\"");

        CmdsWrapper wrapper = JacksonUtil.fromString(json, CmdsWrapper.class);
        assertThat(wrapper.getAuthCmd()).isNotNull();
        assertThat(wrapper.getAuthCmd().getApiKey()).isEqualTo("my-api-key");
        assertThat(wrapper.getAuthCmd().getToken()).isNull();
        assertThat(wrapper.getCmds()).isNull();
    }

    @Test
    void authenticate_loginMode_sendsAuthCmdWithTokenSet() throws Exception {
        WsClient client = newClient();

        client.authenticate(TbClient.AuthMode.LOGIN, "jwt-token");

        String json = captureSentJson(client);
        assertThat(json).contains("\"token\"");
        assertThat(json).doesNotContain("\"apiKey\"");

        CmdsWrapper wrapper = JacksonUtil.fromString(json, CmdsWrapper.class);
        assertThat(wrapper.getAuthCmd()).isNotNull();
        assertThat(wrapper.getAuthCmd().getToken()).isEqualTo("jwt-token");
        assertThat(wrapper.getAuthCmd().getApiKey()).isNull();
        assertThat(wrapper.getCmds()).isNull();
    }

    @Test
    void subscribeForTelemetry_sendsCmdsListWithEntityDataTypeDiscriminator() throws Exception {
        WsClient client = newClient();

        client.subscribeForTelemetry(List.of(UUID.randomUUID()), List.of("temperature"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(client).send(captor.capture());
        String json = captor.getValue();

        assertThat(json).doesNotContain("entityDataCmds");
        assertThat(json).contains("\"type\":\"" + EntityDataCmd.TYPE + "\"");

        CmdsWrapper wrapper = JacksonUtil.fromString(json, CmdsWrapper.class);
        assertThat(wrapper.getAuthCmd()).isNull();
        assertThat(wrapper.getCmds()).hasSize(1);
        assertThat(wrapper.getCmds().get(0).getLatestCmd().getKeys()).hasSize(1);
    }

    private String captureSentJson(WsClient client) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(client).send(captor.capture());
        return captor.getValue();
    }

    // real socket tests below - a mocked send() can't catch a wrong frame ordering or a
    // serialization shape that only breaks against an actual peer

    private WsClient connectRealClient() throws Exception {
        server = new RecordingWsServer();
        server.start();
        server.awaitStart();
        WsClient client = new WsClient(new URI("ws://localhost:" + server.getPort() + "/api/ws"), 2000);
        if (!client.connectBlocking(2000, TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("Test client failed to connect to the embedded WS server");
        }
        this.client = client;
        return client;
    }

    @Test
    void realConnection_authenticate_serverReceivesApiKeyOverTheWire() throws Exception {
        WsClient client = connectRealClient();

        client.authenticate(TbClient.AuthMode.API_KEY, "my-api-key");

        String received = server.awaitMessage();
        CmdsWrapper wrapper = JacksonUtil.fromString(received, CmdsWrapper.class);
        assertThat(wrapper.getAuthCmd().getApiKey()).isEqualTo("my-api-key");
    }

    @Test
    void realConnection_authenticateGoesOutBeforeSubscribe() throws Exception {
        WsClient client = connectRealClient();

        client.authenticate(TbClient.AuthMode.API_KEY, "my-api-key");
        client.subscribeForTelemetry(List.of(UUID.randomUUID()), List.of("temperature"));

        String first = server.awaitMessage();
        String second = server.awaitMessage();

        assertThat(JacksonUtil.fromString(first, CmdsWrapper.class).getAuthCmd())
                .as("first frame must be the auth command")
                .isNotNull();
        assertThat(JacksonUtil.fromString(second, CmdsWrapper.class).getCmds())
                .as("second frame must be the subscribe command")
                .isNotNull();
    }

    @Test
    void realConnection_subscribeForTelemetry_roundTripsEntityDataUpdate() throws Exception {
        WsClient client = connectRealClient();
        UUID deviceId = UUID.randomUUID();
        server.onMessage((conn, message) -> conn.send(entityDataUpdateJson(deviceId, "42")));

        client.registerWaitForUpdates(1);
        client.subscribeForTelemetry(List.of(deviceId), List.of("temperature"));

        String received = server.awaitMessage();
        assertThat(received).contains("\"type\":\"" + EntityDataCmd.TYPE + "\"");

        List<?> updates = client.waitForUpdates(2000);
        assertThat(updates).isNotNull().isNotEmpty();
        assertThat(client.getLatest(deviceId)).containsEntry("temperature", "42");
    }

    @Test
    void realConnection_serverClosesBeforeReply_waitForReplyThrowsWithCloseDiagnostics() throws Exception {
        WsClient client = connectRealClient();
        server.onMessage((conn, message) -> conn.close(1000, "test close"));

        client.subscribeForTelemetry(List.of(UUID.randomUUID()), List.of("temperature"));

        assertThatThrownBy(client::waitForReply)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WebSocket closed")
                .hasMessageContaining("code=1000");
    }

    private static String entityDataUpdateJson(UUID deviceId, String value) {
        EntityData entityData = new EntityData(new DeviceId(deviceId),
                Map.of(EntityKeyType.TIME_SERIES, Map.of("temperature", new TsValue(System.currentTimeMillis(), value))),
                null);
        EntityDataUpdate update = new EntityDataUpdate();
        update.setUpdate(List.of(entityData));
        return JacksonUtil.toString(update);
    }

    private static class RecordingWsServer extends WebSocketServer {

        private final BlockingQueue<String> received = new LinkedBlockingQueue<>();
        private final BlockingQueue<Boolean> started = new LinkedBlockingQueue<>();
        private volatile BiConsumer<WebSocket, String> onMessageHandler = (conn, message) -> {
        };

        RecordingWsServer() {
            super(new InetSocketAddress("localhost", 0));
        }

        void onMessage(BiConsumer<WebSocket, String> handler) {
            this.onMessageHandler = handler;
        }

        void awaitStart() throws InterruptedException {
            if (started.poll(5, TimeUnit.SECONDS) == null) {
                throw new IllegalStateException("Embedded WS server did not start in time");
            }
        }

        String awaitMessage() throws InterruptedException {
            String message = received.poll(5, TimeUnit.SECONDS);
            if (message == null) {
                throw new IllegalStateException("No message received by the embedded WS server in time");
            }
            return message;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            received.add(message);
            onMessageHandler.accept(conn, message);
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
