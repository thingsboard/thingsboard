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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.data.cmd.CmdsWrapper;
import org.thingsboard.monitoring.data.cmd.EntityDataCmd;
import org.thingsboard.monitoring.data.cmd.EntityDataUpdate;
import org.thingsboard.monitoring.data.cmd.LatestValueCmd;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;

import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public class WsClient extends WebSocketClient implements AutoCloseable {

    public final List<JsonNode> lastMsgs = new ArrayList<>();
    private CountDownLatch reply;
    private CountDownLatch update;

    private final Lock updateLock = new ReentrantLock();

    private final long requestTimeoutMs;

    public WsClient(URI serverUri, long requestTimeoutMs) {
        super(serverUri);
        this.requestTimeoutMs = requestTimeoutMs;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    @Override
    public void onMessage(String s) {
        if (s == null) {
            return;
        }
        updateLock.lock();
        try {
            JsonNode msg = JacksonUtil.toJsonNode(s);
            lastMsgs.add(msg);
            log.trace("Received new msg: {}", msg.toPrettyString());
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

    public void registerWaitForUpdates(int count) {
        updateLock.lock();
        try {
            lastMsgs.clear();
            update = new CountDownLatch(count);
        } finally {
            updateLock.unlock();
        }
        log.trace("Registered wait for update");
    }

    @Override
    public void send(String text) throws NotYetConnectedException {
        updateLock.lock();
        try {
            lastMsgs.clear();
            reply = new CountDownLatch(1);
        } finally {
            updateLock.unlock();
        }
        super.send(text);
    }

    public WsClient subscribeForTelemetry(List<UUID> devices, List<String> keys) {
        EntityDataCmd cmd = new EntityDataCmd();
        cmd.setCmdId(RandomUtils.nextInt(0, 1000));

        EntityListFilter devicesFilter = new EntityListFilter();
        devicesFilter.setEntityType(EntityType.DEVICE);
        devicesFilter.setEntityList(devices.stream().map(UUID::toString).collect(Collectors.toList()));
        EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, null);
        EntityDataQuery devicesQuery = new EntityDataQuery(devicesFilter, pageLink, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        cmd.setQuery(devicesQuery);

        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(keys.stream().map(key -> new EntityKey(EntityKeyType.TIME_SERIES, key)).toList());
        cmd.setLatestCmd(latestCmd);

        CmdsWrapper wrapper = new CmdsWrapper();
        wrapper.setEntityDataCmds(List.of(cmd));
        send(JacksonUtil.toString(wrapper));
        return this;
    }

    public List<JsonNode> waitForUpdates(long ms) {
        log.trace("update latch count: {}", update.getCount());
        try {
            if (update.await(ms, TimeUnit.MILLISECONDS)) {
                log.trace("Waited for update");
                return getLastMsgs();
            }
        } catch (InterruptedException e) {
            log.debug("Failed to await reply", e);
        }
        log.trace("No update arrived within {} ms", ms);
        return null;
    }

    public JsonNode waitForReply() {
        try {
            if (reply.await(requestTimeoutMs, TimeUnit.MILLISECONDS)) {
                log.trace("Waited for reply");
                List<JsonNode> lastMsgs = getLastMsgs();
                return lastMsgs.isEmpty() ? null : lastMsgs.get(0);
            }
        } catch (InterruptedException e) {
            log.debug("Failed to await reply", e);
        }
        log.trace("No reply arrived within {} ms", requestTimeoutMs);
        throw new IllegalStateException("No WS reply arrived within " + requestTimeoutMs + " ms");
    }

    private List<JsonNode> getLastMsgs() {
        if (lastMsgs.isEmpty()) {
            return lastMsgs;
        }
        List<JsonNode> errors = lastMsgs.stream()
                .map(msg -> msg.get("errorMsg"))
                .filter(errorMsg -> errorMsg != null && !errorMsg.isNull() && StringUtils.isNotEmpty(errorMsg.asText()))
                .toList();
        if (!errors.isEmpty()) {
            throw new RuntimeException("WS error from server: " + errors.stream()
                    .map(JsonNode::asText)
                    .collect(Collectors.joining(", ")));
        }
        return lastMsgs;
    }

    public Map<String, String> getLatest(UUID deviceId) {
        Map<String, String> updates = new HashMap<>();
        getLastMsgs().forEach(msg -> {
            EntityDataUpdate update = JacksonUtil.treeToValue(msg, EntityDataUpdate.class);
            Map<String, String> latest = update.getLatest(deviceId);
            updates.putAll(latest);
        });
        return updates;
    }

    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm(null);
    }

}