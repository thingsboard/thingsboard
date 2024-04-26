/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.msa;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.service.ws.AuthCmd;
import org.thingsboard.server.service.ws.WsCmd;
import org.thingsboard.server.service.ws.WsCommandsWrapper;
import org.thingsboard.server.service.ws.notification.cmd.MarkAllNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsCountSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsCountUpdate;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.AttributesSubscriptionCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdateType;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityHistoryCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.TimeSeriesCmd;

import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
public class WsClient extends WebSocketClient {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    @Getter
    private volatile String lastMsg;
    private volatile CountDownLatch reply;
    private volatile CountDownLatch update;

    private UnreadNotificationsUpdate lastDataUpdate;
    private UnreadNotificationsCountUpdate lastCountUpdate;
    private final long timeoutMultiplier;

    private int limit;
    private int unreadCount;
    private List<Notification> notifications;

    public WsClient(URI serverUri, long timeoutMultiplier) {
        super(serverUri);
        this.timeoutMultiplier = timeoutMultiplier;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    public void authenticate(String token) {
        WsCommandsWrapper cmdsWrapper = new WsCommandsWrapper();
        cmdsWrapper.setAuthCmd(new AuthCmd(1, token));
        send(JacksonUtil.toString(cmdsWrapper));
    }

    public void registerWaitForUpdate() {
        registerWaitForUpdate(1);
    }

    @Override
    public void send(String text) throws NotYetConnectedException {
        log.debug("send [{}]", text);
        reply = new CountDownLatch(1);
        super.send(text);
    }

    @Override
    public synchronized void onClose(int code, String reason, boolean remote) {
        log.error("WS onClose: [{}]", reason);
    }

    @Override
    public synchronized void onError(Exception ex) {
        log.error("WS onError: ", ex);
        ex.printStackTrace();
    }

    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm(null);
    }

    public String waitForUpdate() {
        return waitForUpdate(false);
    }

    public String waitForUpdate(boolean throwExceptionOnTimeout) {
        return waitForUpdate(TIMEOUT, throwExceptionOnTimeout);
    }

    public String waitForUpdate(long ms) {
        return waitForUpdate(ms, false);
    }

    public String waitForUpdate(long ms, boolean throwExceptionOnTimeout) {
        log.debug("waitForUpdate [{}]", ms);
        try {
            if (update.await(ms, TimeUnit.MILLISECONDS)) {
                return lastMsg;
            } else {
                log.warn("Failed to await update (waiting time [{}]ms elapsed)", ms, new RuntimeException("stacktrace"));
            }
        } catch (InterruptedException e) {
            log.warn("Failed to await update", e);
        }
        if (throwExceptionOnTimeout) {
            throw new AssertionError("Waited for update for " + ms + " ms but none arrived");
        } else {
            return null;
        }
    }

    public String waitForReply() {
        return waitForReply(false);
    }

    public String waitForReply(boolean throwExceptionOnTimeout) {
        return waitForReply(TIMEOUT, throwExceptionOnTimeout);
    }

    public String waitForReply(long ms, boolean throwExceptionOnTimeout) {
        log.debug("waitForReply [{}]", ms);
        try {
            if (reply.await(ms, TimeUnit.MILLISECONDS)) {
                return lastMsg;
            } else {
                log.warn("Failed to await reply (waiting time [{}]ms elapsed)", ms, new RuntimeException("stacktrace"));
            }
        } catch (InterruptedException e) {
            log.warn("Failed to await reply", e);
        }
        if (throwExceptionOnTimeout) {
            throw new AssertionError("Waited for reply for " + ms + " ms but none arrived");
        } else {
            return null;
        }
    }

    public EntityDataUpdate parseDataReply(String msg) {
        return JacksonUtil.fromString(msg, EntityDataUpdate.class);
    }

    public EntityCountUpdate parseCountReply(String msg) {
        return JacksonUtil.fromString(msg, EntityCountUpdate.class);
    }

    public AlarmCountUpdate parseAlarmCountReply(String msg) {
        return JacksonUtil.fromString(msg, AlarmCountUpdate.class);
    }

    public EntityDataUpdate subscribeLatestUpdate(List<EntityKey> keys, EntityFilter entityFilter) {
        EntityDataQuery edq = new EntityDataQuery(entityFilter, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        return subscribeLatestUpdate(keys, edq);
    }

    public EntityDataUpdate subscribeLatestUpdate(List<EntityKey> keys) {
        return subscribeLatestUpdate(keys, (EntityDataQuery) null);
    }

    public EntityDataUpdate subscribeLatestUpdate(List<EntityKey> keys, EntityDataQuery edq) {
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(keys);
        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        send(cmd);
        return parseDataReply(waitForReply());
    }

    public EntityDataUpdate subscribeTsUpdate(List<String> keys, long startTs, long timeWindow) {
        return subscribeTsUpdate(keys, startTs, timeWindow, (EntityDataQuery) null);
    }

    public EntityDataUpdate subscribeTsUpdate(List<String> keys, long startTs, long timeWindow, EntityFilter entityFilter) {
        EntityDataQuery edq = new EntityDataQuery(entityFilter, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        return subscribeTsUpdate(keys, startTs, timeWindow, edq);
    }

    public EntityDataUpdate subscribeTsUpdate(List<String> keys, long startTs, long timeWindow, EntityDataQuery edq) {
        TimeSeriesCmd tsCmd = new TimeSeriesCmd();
        tsCmd.setKeys(keys);
        tsCmd.setAgg(Aggregation.NONE);
        tsCmd.setLimit(1000);
        tsCmd.setStartTs(startTs - timeWindow);
        tsCmd.setTimeWindow(timeWindow);

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, null, tsCmd);

        send(cmd);
        return parseDataReply(waitForReply());
    }

    public void subscribeForTsUpdates(EntityId entityId, List<String> keys) {
        subscribeForTsUpdates(entityId, keys, System.currentTimeMillis());
    }

    public void subscribeForTsUpdates(EntityId entityId, List<String> keys, long startTs) {
        subscribeForTsUpdates(entityId, keys, startTs, TimeUnit.HOURS.toMillis(1));
    }

    public void subscribeForTsUpdates(EntityId entityId, List<String> keys, long startTs, long timeWindow) {
        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(entityId);
        EntityDataUpdate entityDataUpdate = this.subscribeTsUpdate(keys, startTs, timeWindow, filter);
        assertThat(entityDataUpdate.getData().getData().size()).isEqualTo(1);
        Map<String, TsValue[]> timeseries = entityDataUpdate.getData().getData().get(0).getTimeseries();
        assertThat(timeseries.keySet()).containsOnlyOnceElementsOf(keys);
    }

    public Map<String, TsValue[]> getLatestTsValuesFromSubscription() {
        String updateString = this.waitForUpdate(3000, true);
        EntityDataUpdate update = JacksonUtil.fromString(updateString, EntityDataUpdate.class);
        assertThat(update).isNotNull();
        assertThat(update.getUpdate()).isNotNull();
        assertThat(update.getUpdate().size()).isEqualTo(1);
        assertThat(update.getUpdate().get(0).getTimeseries()).isNotNull();
        return update.getUpdate().get(0).getTimeseries();
    }

    public JsonNode subscribeForAttributes(EntityId entityId, String scope, List<String> keys) {
        AttributesSubscriptionCmd cmd = new AttributesSubscriptionCmd();
        cmd.setCmdId(1);
        cmd.setEntityType(entityId.getEntityType().toString());
        cmd.setEntityId(entityId.getId().toString());
        cmd.setScope(scope);
        cmd.setKeys(String.join(",", keys));
        send(cmd);
        return JacksonUtil.toJsonNode(waitForReply());
    }

    public EntityDataUpdate sendHistoryCmd(List<String> keys, long startTs, long timeWindow) {
        return sendHistoryCmd(keys, startTs, timeWindow, (EntityDataQuery) null);
    }

    public EntityDataUpdate sendHistoryCmd(List<String> keys, long startTs, long timeWindow, EntityFilter entityFilter) {
        EntityDataQuery edq = new EntityDataQuery(entityFilter,
                new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        return sendHistoryCmd(keys, startTs, timeWindow, edq);
    }

    public EntityDataUpdate sendHistoryCmd(List<String> keys, long startTs, long timeWindow, EntityDataQuery edq) {
        EntityHistoryCmd historyCmd = new EntityHistoryCmd();
        historyCmd.setKeys(keys);
        historyCmd.setAgg(Aggregation.NONE);
        historyCmd.setLimit(1000);
        historyCmd.setStartTs(startTs - timeWindow);
        historyCmd.setEndTs(startTs);

        EntityDataCmd cmd = new EntityDataCmd(1, edq, historyCmd, null, null);

        send(cmd);
        return parseDataReply(this.waitForReply());
    }

    public EntityDataUpdate sendEntityDataQuery(EntityDataQuery edq) {
        log.warn("sendEntityDataQuery {}", edq);
        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, null, null);
        send(cmd);
        String msg = this.waitForReply();
        return parseDataReply(msg);
    }

    public EntityDataUpdate sendEntityDataQuery(EntityFilter entityFilter) {
        log.warn("sendEntityDataQuery {}", entityFilter);
        EntityDataQuery edq = new EntityDataQuery(entityFilter, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        return sendEntityDataQuery(edq);
    }

    public void send(WsCmd... cmds) {
        WsCommandsWrapper cmdsWrapper = new WsCommandsWrapper();
        cmdsWrapper.setCmds(List.of(cmds));
        send(JacksonUtil.toString(cmdsWrapper));
    }

    public WsClient subscribeForUnreadNotifications(int limit) throws InterruptedException {
        send(new NotificationsSubCmd(1, limit));
        this.limit = limit;
        return this;
    }

    public WsClient subscribeForUnreadNotificationsCount() {
        send(new NotificationsCountSubCmd(2));
        return this;
    }

    public void markNotificationAsRead(UUID... notifications) {
        send(new MarkNotificationsAsReadCmd(newCmdId(), Arrays.asList(notifications)));
    }

    public void markAllNotificationsAsRead() {
        send(new MarkAllNotificationsAsReadCmd(newCmdId()));
    }

    public void registerWaitForUpdate(int count) {
        lastDataUpdate = null;
        lastCountUpdate = null;
        log.debug("registerWaitForUpdate [{}]", count);
        lastMsg = null;
        update = new CountDownLatch(count);
    }

    @Override
    public void onMessage(String s) {
        JsonNode updateNode = JacksonUtil.toJsonNode(s);
        if (updateNode.has("cmdUpdateType")) {
            CmdUpdateType updateType = CmdUpdateType.valueOf(updateNode.get("cmdUpdateType").asText());
            if (updateType == CmdUpdateType.NOTIFICATIONS) {
                lastDataUpdate = JacksonUtil.treeToValue(updateNode, UnreadNotificationsUpdate.class);
                unreadCount = lastDataUpdate.getTotalUnreadCount();
                if (lastDataUpdate.getNotifications() != null) {
                    notifications = new ArrayList<>(lastDataUpdate.getNotifications());
                } else {
                    Notification notificationUpdate = lastDataUpdate.getUpdate();
                    boolean updated = false;
                    for (int i = 0; i < notifications.size(); i++) {
                        Notification existing = notifications.get(i);
                        if (existing.getId().equals(notificationUpdate.getId())) {
                            notifications.set(i, notificationUpdate);
                            updated = true;
                            break;
                        }
                    }
                    if (!updated) {
                        notifications.add(0, notificationUpdate);
                        if (notifications.size() > limit) {
                            notifications = notifications.subList(0, limit);
                        }
                    }
                }
            } else if (updateType == CmdUpdateType.NOTIFICATIONS_COUNT) {
                lastCountUpdate = JacksonUtil.treeToValue(updateNode, UnreadNotificationsCountUpdate.class);
                unreadCount = lastCountUpdate.getTotalUnreadCount();
            }
        }
        log.info("RECEIVED: {}", s);
        lastMsg = s;
        if (update != null) {
            update.countDown();
        }
        if (reply != null) {
            reply.countDown();
        }
    }

    private static int newCmdId() {
        return RandomUtils.nextInt(1, 1000);
    }

}
