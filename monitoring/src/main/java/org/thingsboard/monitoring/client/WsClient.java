/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.data.cmd.CmdsWrapper;
import org.thingsboard.monitoring.data.cmd.TimeseriesSubscriptionCmd;
import org.thingsboard.monitoring.data.cmd.TimeseriesUpdate;

import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class WsClient extends WebSocketClient {

    public volatile String lastMsg;
    private CountDownLatch reply;
    private CountDownLatch update;

    private final Lock updateLock = new ReentrantLock();

    public WsClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    @Override
    public void onMessage(String s) {
        log.trace("Received new msg: {}", s);
        if (s == null) {
            return;
        }
        updateLock.lock();
        try {
            lastMsg = s;
            if (update != null) {
                update.countDown();
            }
            if (reply != null) {
                reply.countDown();
            }
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        log.debug("WebSocket client is closed");
    }

    @Override
    public void onError(Exception e) {
        log.error("WebSocket client error:", e);
    }

    public void registerWaitForUpdate() {
        updateLock.lock();
        try {
            lastMsg = null;
            update = new CountDownLatch(1);
        } finally {
            updateLock.unlock();
        }
        log.trace("Registered wait for update");
    }

    @Override
    public void send(String text) throws NotYetConnectedException {
        updateLock.lock();
        try {
            reply = new CountDownLatch(1);
        } finally {
            updateLock.unlock();
        }
        super.send(text);
    }

    public void subscribeForTelemetry(UUID deviceId, String telemetryKey) {
        TimeseriesSubscriptionCmd subCmd = new TimeseriesSubscriptionCmd();
        subCmd.setEntityType("DEVICE");
        subCmd.setEntityId(deviceId.toString());
        subCmd.setScope("LATEST_TELEMETRY");
        subCmd.setKeys(telemetryKey);
        subCmd.setCmdId(RandomUtils.nextInt(0, 100));

        CmdsWrapper wrapper = new CmdsWrapper();
        wrapper.setTsSubCmds(List.of(subCmd));
        send(JacksonUtil.toString(wrapper));
        log.trace("Subscribed for telemetry (key: {})", telemetryKey);
    }

    public JsonNode waitForUpdate(long ms) {
        log.trace("update latch count: {}", update.getCount());
        try {
            if (update.await(ms, TimeUnit.MILLISECONDS)) {
                log.trace("Waited for update");
                return getLastMsg();
            }
        } catch (InterruptedException e) {
            log.debug("Failed to await reply", e);
        }
        log.trace("No update arrived within {} ms", ms);
        return null;
    }

    public JsonNode waitForReply(int ms) {
        try {
            if (reply.await(ms, TimeUnit.MILLISECONDS)) {
                log.trace("Waited for reply");
                return getLastMsg();
            }
        } catch (InterruptedException e) {
            log.debug("Failed to await reply", e);
        }
        log.trace("No reply arrived within {} ms", ms);
        return null;
    }

    public JsonNode getLastMsg() {
        JsonNode msg = JacksonUtil.toJsonNode(lastMsg);
        if (msg != null) {
            JsonNode errorMsg = msg.get("errorMsg");
            if (errorMsg != null && !errorMsg.isNull() && StringUtils.isNotEmpty(errorMsg.asText())) {
                throw new RuntimeException("WS error from server: " + errorMsg.asText());
            } else {
                return msg;
            }
        } else {
            return null;
        }
    }

    public Object getTelemetryKeyUpdate(String key) {
        JsonNode lastMsg = getLastMsg();
        if (lastMsg == null || lastMsg.isNull()) return null;
        TimeseriesUpdate update = JacksonUtil.treeToValue(lastMsg, TimeseriesUpdate.class);
        return update.getLatest(key);
    }

    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm(null);
    }

}