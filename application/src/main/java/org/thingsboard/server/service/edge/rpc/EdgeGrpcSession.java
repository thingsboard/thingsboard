/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectResponseCode;
import org.thingsboard.server.gen.edge.v1.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EdgeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsgType;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.GeneralEdgeEventFetcher;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Data
public final class EdgeGrpcSession implements Closeable {

    private static final ReentrantLock downlinkMsgLock = new ReentrantLock();

    private static final int MAX_DOWNLINK_ATTEMPTS = 10; // max number of attemps to send downlink message if edge connected

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";

    private final UUID sessionId;
    private final BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener;
    private final Consumer<EdgeId> sessionCloseListener;

    private final EdgeSessionState sessionState = new EdgeSessionState();

    private EdgeContextComponent ctx;
    private Edge edge;
    private StreamObserver<RequestMsg> inputStream;
    private StreamObserver<ResponseMsg> outputStream;
    private boolean connected;
    private boolean syncCompleted;

    private EdgeVersion edgeVersion;

    private ScheduledExecutorService sendDownlinkExecutorService;

    EdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream, BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener,
                    Consumer<EdgeId> sessionCloseListener, ScheduledExecutorService sendDownlinkExecutorService) {
        this.sessionId = UUID.randomUUID();
        this.ctx = ctx;
        this.outputStream = outputStream;
        this.sessionOpenListener = sessionOpenListener;
        this.sessionCloseListener = sessionCloseListener;
        this.sendDownlinkExecutorService = sendDownlinkExecutorService;
        initInputStream();
    }

    private void initInputStream() {
        this.inputStream = new StreamObserver<>() {
            @Override
            public void onNext(RequestMsg requestMsg) {
                if (!connected && requestMsg.getMsgType().equals(RequestMsgType.CONNECT_RPC_MESSAGE)) {
                    ConnectResponseMsg responseMsg = processConnect(requestMsg.getConnectRequestMsg());
                    outputStream.onNext(ResponseMsg.newBuilder()
                            .setConnectResponseMsg(responseMsg)
                            .build());
                    if (ConnectResponseCode.ACCEPTED != responseMsg.getResponseCode()) {
                        outputStream.onError(new RuntimeException(responseMsg.getErrorMsg()));
                    } else {
                        connected = true;
                    }
                }
                if (connected) {
                    if (requestMsg.getMsgType().equals(RequestMsgType.SYNC_REQUEST_RPC_MESSAGE)) {
                        if (requestMsg.hasSyncRequestMsg() && requestMsg.getSyncRequestMsg().getSyncRequired()) {
                            boolean fullSync = true;
                            if (requestMsg.getSyncRequestMsg().hasFullSync()) {
                                fullSync = requestMsg.getSyncRequestMsg().getFullSync();
                            }
                            startSyncProcess(edge.getTenantId(), edge.getId(), fullSync);
                        } else {
                            syncCompleted = true;
                        }
                    }
                    if (requestMsg.getMsgType().equals(RequestMsgType.UPLINK_RPC_MESSAGE)) {
                        if (requestMsg.hasUplinkMsg()) {
                            onUplinkMsg(requestMsg.getUplinkMsg());
                        }
                        if (requestMsg.hasDownlinkResponseMsg()) {
                            onDownlinkResponse(requestMsg.getDownlinkResponseMsg());
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("[{}] Stream was terminated due to error:", sessionId, t);
                closeSession();
            }

            @Override
            public void onCompleted() {
                log.info("[{}] Stream was closed and completed successfully!", sessionId);
                closeSession();
            }

            private void closeSession() {
                connected = false;
                if (edge != null) {
                    try {
                        sessionCloseListener.accept(edge.getId());
                    } catch (Exception ignored) {
                    }
                }
                try {
                    outputStream.onCompleted();
                } catch (Exception ignored) {
                }
            }
        };
    }

    public void startSyncProcess(TenantId tenantId, EdgeId edgeId, boolean fullSync) {
        log.trace("[{}][{}] Staring edge sync process", tenantId, edgeId);
        syncCompleted = false;
        interruptGeneralProcessingOnSync(tenantId, edgeId);
        doSync(new EdgeSyncCursor(ctx, edge, fullSync));
    }

    private void doSync(EdgeSyncCursor cursor) {
        if (cursor.hasNext()) {
            EdgeEventFetcher next = cursor.getNext();
            log.info("[{}][{}] starting sync process, cursor current idx = {}, class = {}",
                    edge.getTenantId(), edge.getId(), cursor.getCurrentIdx(), next.getClass().getSimpleName());
            ListenableFuture<UUID> uuidListenableFuture = startProcessingEdgeEvents(next);
            Futures.addCallback(uuidListenableFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable UUID result) {
                    doSync(cursor);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Exception during sync process", edge.getTenantId(), edge.getId(), t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        } else {
            DownlinkMsg syncCompleteDownlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .setSyncCompletedMsg(SyncCompletedMsg.newBuilder().build())
                    .build();
            Futures.addCallback(sendDownlinkMsgsPack(Collections.singletonList(syncCompleteDownlinkMsg)), new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    syncCompleted = true;
                    ctx.getClusterService().onEdgeEventUpdate(edge.getTenantId(), edge.getId());
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Exception during sending sync complete", edge.getTenantId(), edge.getId(), t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        }
    }

    private void onUplinkMsg(UplinkMsg uplinkMsg) {
        ListenableFuture<List<Void>> future = processUplinkMsg(uplinkMsg);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                UplinkResponseMsg uplinkResponseMsg = UplinkResponseMsg.newBuilder()
                        .setUplinkMsgId(uplinkMsg.getUplinkMsgId())
                        .setSuccess(true).build();
                sendDownlinkMsg(ResponseMsg.newBuilder()
                        .setUplinkResponseMsg(uplinkResponseMsg)
                        .build());
            }

            @Override
            public void onFailure(Throwable t) {
                String errorMsg = EdgeUtils.createErrorMsgFromRootCauseAndStackTrace(t);
                UplinkResponseMsg uplinkResponseMsg = UplinkResponseMsg.newBuilder()
                        .setUplinkMsgId(uplinkMsg.getUplinkMsgId())
                        .setSuccess(false).setErrorMsg(errorMsg).build();
                sendDownlinkMsg(ResponseMsg.newBuilder()
                        .setUplinkResponseMsg(uplinkResponseMsg)
                        .build());
            }
        }, ctx.getGrpcCallbackExecutorService());
    }

    private void onDownlinkResponse(DownlinkResponseMsg msg) {
        try {
            if (msg.getSuccess()) {
                sessionState.getPendingMsgsMap().remove(msg.getDownlinkMsgId());
                log.debug("[{}] Msg has been processed successfully!Msd Id: [{}], Msg: {}", edge.getRoutingKey(), msg.getDownlinkMsgId(), msg);
            } else {
                log.error("[{}] Msg processing failed! Msd Id: [{}], Error msg: {}", edge.getRoutingKey(), msg.getDownlinkMsgId(), msg.getErrorMsg());
            }
            if (sessionState.getPendingMsgsMap().isEmpty()) {
                log.debug("[{}] Pending msgs map is empty. Stopping current iteration", edge.getRoutingKey());
                stopCurrentSendDownlinkMsgsTask(null);
            }
        } catch (Exception e) {
            log.error("[{}] Can't process downlink response message [{}]", this.sessionId, msg, e);
        }
    }

    private void sendDownlinkMsg(ResponseMsg downlinkMsg) {
        log.trace("[{}] Sending downlink msg [{}]", this.sessionId, downlinkMsg);
        if (isConnected()) {
            downlinkMsgLock.lock();
            try {
                outputStream.onNext(downlinkMsg);
            } catch (Exception e) {
                log.error("[{}] Failed to send downlink message [{}]", this.sessionId, downlinkMsg, e);
                connected = false;
                sessionCloseListener.accept(edge.getId());
            } finally {
                downlinkMsgLock.unlock();
            }
            log.trace("[{}] Response msg successfully sent [{}]", this.sessionId, downlinkMsg);
        }
    }

    void onConfigurationUpdate(Edge edge) {
        log.debug("[{}] onConfigurationUpdate [{}]", this.sessionId, edge);
        this.edge = edge;
        EdgeUpdateMsg edgeConfig = EdgeUpdateMsg.newBuilder()
                .setConfiguration(ctx.getEdgeMsgConstructor().constructEdgeConfiguration(edge)).build();
        ResponseMsg edgeConfigMsg = ResponseMsg.newBuilder()
                .setEdgeUpdateMsg(edgeConfig)
                .build();
        sendDownlinkMsg(edgeConfigMsg);
    }

    ListenableFuture<Void> processEdgeEvents() throws Exception {
        SettableFuture<Void> result = SettableFuture.create();
        log.trace("[{}] starting processing edge events", this.sessionId);
        if (isConnected() && isSyncCompleted()) {
            Long queueStartTs = getQueueStartTs().get();
            GeneralEdgeEventFetcher fetcher = new GeneralEdgeEventFetcher(
                    queueStartTs,
                    ctx.getEdgeEventService());
            ListenableFuture<UUID> ifOffsetFuture = startProcessingEdgeEvents(fetcher);
            Futures.addCallback(ifOffsetFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable UUID ifOffset) {
                    if (ifOffset != null) {
                        Long newStartTs = Uuids.unixTimestamp(ifOffset);
                        ListenableFuture<List<String>> updateFuture = updateQueueStartTs(newStartTs);
                        Futures.addCallback(updateFuture, new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable List<String> list) {
                                log.debug("[{}] queue offset was updated [{}][{}]", sessionId, ifOffset, newStartTs);
                                result.set(null);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.error("[{}] Failed to update queue offset [{}]", sessionId, ifOffset, t);
                                result.setException(t);
                            }
                        }, ctx.getGrpcCallbackExecutorService());
                    } else {
                        log.trace("[{}] ifOffset is null. Skipping iteration without db update", sessionId);
                        result.set(null);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}] Failed to process events", sessionId, t);
                    result.setException(t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        } else {
            log.trace("[{}] edge is not connected or sync is not completed. Skipping iteration", sessionId);
            result.set(null);
        }
        return result;
    }

    private ListenableFuture<UUID> startProcessingEdgeEvents(EdgeEventFetcher fetcher) {
        SettableFuture<UUID> result = SettableFuture.create();
        PageLink pageLink = fetcher.getPageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount());
        processEdgeEvents(fetcher, pageLink, result);
        return result;
    }

    private void processEdgeEvents(EdgeEventFetcher fetcher, PageLink pageLink, SettableFuture<UUID> result) {
        try {
            PageData<EdgeEvent> pageData = fetcher.fetchEdgeEvents(edge.getTenantId(), edge, pageLink);
            if (isConnected() && !pageData.getData().isEmpty()) {
                log.trace("[{}] [{}] event(s) are going to be processed.", this.sessionId, pageData.getData().size());
                List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(pageData.getData());
                Futures.addCallback(sendDownlinkMsgsPack(downlinkMsgsPack), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        if (isConnected() && pageData.hasNext()) {
                            processEdgeEvents(fetcher, pageLink.nextPageLink(), result);
                        } else {
                            UUID ifOffset = pageData.getData().get(pageData.getData().size() - 1).getUuidId();
                            result.set(ifOffset);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to send downlink msgs pack", sessionId, t);
                        result.setException(t);
                    }
                }, ctx.getGrpcCallbackExecutorService());
            } else {
                log.trace("[{}] no event(s) found. Stop processing edge events", this.sessionId);
                result.set(null);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to fetch edge events", this.sessionId, e);
            result.setException(e);
        }
    }

    private ListenableFuture<Void> sendDownlinkMsgsPack(List<DownlinkMsg> downlinkMsgsPack) {
        interruptPreviousSendDownlinkMsgsTask();

        sessionState.setSendDownlinkMsgsFuture(SettableFuture.create());
        sessionState.getPendingMsgsMap().clear();

        downlinkMsgsPack.forEach(msg -> sessionState.getPendingMsgsMap().put(msg.getDownlinkMsgId(), msg));
        scheduleDownlinkMsgsPackSend(1);

        return sessionState.getSendDownlinkMsgsFuture();
    }

    private void scheduleDownlinkMsgsPackSend(int attempt) {
        Runnable sendDownlinkMsgsTask = () -> {
            try {
                if (isConnected() && sessionState.getPendingMsgsMap().values().size() > 0) {
                    List<DownlinkMsg> copy = new ArrayList<>(sessionState.getPendingMsgsMap().values());
                    if (attempt > 1) {
                        log.warn("[{}] Failed to deliver the batch: {}, attempt: {}", this.sessionId, copy, attempt);
                    }
                    log.trace("[{}] [{}] downlink msg(s) are going to be send.", this.sessionId, copy.size());
                    for (DownlinkMsg downlinkMsg : copy) {
                        sendDownlinkMsg(ResponseMsg.newBuilder()
                                .setDownlinkMsg(downlinkMsg)
                                .build());
                    }
                    if (attempt < MAX_DOWNLINK_ATTEMPTS) {
                        scheduleDownlinkMsgsPackSend(attempt + 1);
                    } else {
                        log.warn("[{}] Failed to deliver the batch after {} attempts. Next messages are going to be discarded {}",
                                this.sessionId, MAX_DOWNLINK_ATTEMPTS, copy);
                        stopCurrentSendDownlinkMsgsTask(null);
                    }
                } else {
                    stopCurrentSendDownlinkMsgsTask(null);
                }
            } catch (Exception e) {
                stopCurrentSendDownlinkMsgsTask(e);
            }
        };

        if (attempt == 1) {
            sendDownlinkExecutorService.submit(sendDownlinkMsgsTask);
        } else {
            sessionState.setScheduledSendDownlinkTask(
                    sendDownlinkExecutorService.schedule(
                            sendDownlinkMsgsTask,
                            ctx.getEdgeEventStorageSettings().getSleepIntervalBetweenBatches(),
                            TimeUnit.MILLISECONDS)
            );
        }
    }

    private DownlinkMsg convertToDownlinkMsg(EdgeEvent edgeEvent) {
        log.trace("[{}][{}] converting edge event to downlink msg [{}]", edge.getTenantId(), this.sessionId, edgeEvent);
        DownlinkMsg downlinkMsg = null;
        try {
            switch (edgeEvent.getAction()) {
                case UPDATED:
                case ADDED:
                case DELETED:
                case ASSIGNED_TO_EDGE:
                case UNASSIGNED_FROM_EDGE:
                case ALARM_ACK:
                case ALARM_CLEAR:
                case CREDENTIALS_UPDATED:
                case RELATION_ADD_OR_UPDATE:
                case RELATION_DELETED:
                case ASSIGNED_TO_CUSTOMER:
                case UNASSIGNED_FROM_CUSTOMER:
                case CREDENTIALS_REQUEST:
                case RPC_CALL:
                    downlinkMsg = convertEntityEventToDownlink(edgeEvent);
                    log.trace("[{}][{}] entity message processed [{}]", edgeEvent.getTenantId(), this.sessionId, downlinkMsg);
                    break;
                case ATTRIBUTES_UPDATED:
                case POST_ATTRIBUTES:
                case ATTRIBUTES_DELETED:
                case TIMESERIES_UPDATED:
                    downlinkMsg = ctx.getTelemetryProcessor().convertTelemetryEventToDownlink(edgeEvent);
                    break;
                default:
                    log.warn("[{}][{}] Unsupported action type [{}]", edge.getTenantId(), this.sessionId, edgeEvent.getAction());
            }
        } catch (Exception e) {
            log.error("[{}][{}] Exception during converting edge event to downlink msg", edge.getTenantId(), this.sessionId, e);
        }
        return downlinkMsg;
    }

    private List<DownlinkMsg> convertToDownlinkMsgsPack(List<EdgeEvent> edgeEvents) {
        return edgeEvents
                .stream()
                .map(this::convertToDownlinkMsg)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ListenableFuture<Long> getQueueStartTs() {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                ctx.getAttributesService().find(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, QUEUE_START_TS_ATTR_KEY);
        return Futures.transform(future, attributeKvEntryOpt -> {
            if (attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent()) {
                AttributeKvEntry attributeKvEntry = attributeKvEntryOpt.get();
                return attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
            } else {
                return 0L;
            }
        }, ctx.getGrpcCallbackExecutorService());
    }

    private ListenableFuture<List<String>> updateQueueStartTs(Long newStartTs) {
        log.trace("[{}] updating QueueStartTs [{}][{}]", this.sessionId, edge.getId(), newStartTs);
        List<AttributeKvEntry> attributes = Collections.singletonList(
                new BaseAttributeKvEntry(
                        new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs), System.currentTimeMillis()));
        return ctx.getAttributesService().save(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, attributes);
    }

    private DownlinkMsg convertEntityEventToDownlink(EdgeEvent edgeEvent) {
        log.trace("Executing convertEntityEventToDownlink, edgeEvent [{}], action [{}]", edgeEvent, edgeEvent.getAction());
        switch (edgeEvent.getType()) {
            case EDGE:
                return ctx.getEdgeProcessor().convertEdgeEventToDownlink(edgeEvent);
            case DEVICE:
                return ctx.getDeviceProcessor().convertDeviceEventToDownlink(edgeEvent);
            case DEVICE_PROFILE:
                return ctx.getDeviceProfileProcessor().convertDeviceProfileEventToDownlink(edgeEvent);
            case ASSET_PROFILE:
                return ctx.getAssetProfileProcessor().convertAssetProfileEventToDownlink(edgeEvent);
            case ASSET:
                return ctx.getAssetProcessor().convertAssetEventToDownlink(edgeEvent);
            case ENTITY_VIEW:
                return ctx.getEntityViewProcessor().convertEntityViewEventToDownlink(edgeEvent);
            case DASHBOARD:
                return ctx.getDashboardProcessor().convertDashboardEventToDownlink(edgeEvent);
            case CUSTOMER:
                return ctx.getCustomerProcessor().convertCustomerEventToDownlink(edgeEvent);
            case RULE_CHAIN:
                return ctx.getRuleChainProcessor().convertRuleChainEventToDownlink(edgeEvent);
            case RULE_CHAIN_METADATA:
                return ctx.getRuleChainProcessor().convertRuleChainMetadataEventToDownlink(edgeEvent, this.edgeVersion);
            case ALARM:
                return ctx.getAlarmProcessor().convertAlarmEventToDownlink(edgeEvent);
            case USER:
                return ctx.getUserProcessor().convertUserEventToDownlink(edgeEvent);
            case RELATION:
                return ctx.getRelationProcessor().convertRelationEventToDownlink(edgeEvent);
            case WIDGETS_BUNDLE:
                return ctx.getWidgetBundleProcessor().convertWidgetsBundleEventToDownlink(edgeEvent);
            case WIDGET_TYPE:
                return ctx.getWidgetTypeProcessor().convertWidgetTypeEventToDownlink(edgeEvent);
            case ADMIN_SETTINGS:
                return ctx.getAdminSettingsProcessor().convertAdminSettingsEventToDownlink(edgeEvent);
            case OTA_PACKAGE:
                return ctx.getOtaPackageEdgeProcessor().convertOtaPackageEventToDownlink(edgeEvent);
            case QUEUE:
                return ctx.getQueueEdgeProcessor().convertQueueEventToDownlink(edgeEvent);
            default:
                log.warn("Unsupported edge event type [{}]", edgeEvent);
                return null;
        }
    }

    private ListenableFuture<List<Void>> processUplinkMsg(UplinkMsg uplinkMsg) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            if (uplinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : uplinkMsg.getEntityDataList()) {
                    result.addAll(ctx.getTelemetryProcessor().processTelemetryMsg(edge.getTenantId(), entityData));
                }
            }
            if (uplinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : uplinkMsg.getDeviceUpdateMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceMsgFromEdge(edge.getTenantId(), edge, deviceUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : uplinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceCredentialsMsg(edge.getTenantId(), deviceCredentialsUpdateMsg));
                }
            }
            if (uplinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : uplinkMsg.getAlarmUpdateMsgList()) {
                    result.add(ctx.getAlarmProcessor().processAlarmMsg(edge.getTenantId(), alarmUpdateMsg));
                }
            }
            if (uplinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : uplinkMsg.getRelationUpdateMsgList()) {
                    result.add(ctx.getRelationProcessor().processRelationMsg(edge.getTenantId(), relationUpdateMsg));
                }
            }
            if (uplinkMsg.getRuleChainMetadataRequestMsgCount() > 0) {
                for (RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg : uplinkMsg.getRuleChainMetadataRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processRuleChainMetadataRequestMsg(edge.getTenantId(), edge, ruleChainMetadataRequestMsg));
                }
            }
            if (uplinkMsg.getAttributesRequestMsgCount() > 0) {
                for (AttributesRequestMsg attributesRequestMsg : uplinkMsg.getAttributesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processAttributesRequestMsg(edge.getTenantId(), edge, attributesRequestMsg));
                }
            }
            if (uplinkMsg.getRelationRequestMsgCount() > 0) {
                for (RelationRequestMsg relationRequestMsg : uplinkMsg.getRelationRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processRelationRequestMsg(edge.getTenantId(), edge, relationRequestMsg));
                }
            }
            if (uplinkMsg.getUserCredentialsRequestMsgCount() > 0) {
                for (UserCredentialsRequestMsg userCredentialsRequestMsg : uplinkMsg.getUserCredentialsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processUserCredentialsRequestMsg(edge.getTenantId(), edge, userCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : uplinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processDeviceCredentialsRequestMsg(edge.getTenantId(), edge, deviceCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceRpcCallMsgCount() > 0) {
                for (DeviceRpcCallMsg deviceRpcCallMsg : uplinkMsg.getDeviceRpcCallMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceRpcCallFromEdge(edge.getTenantId(), edge, deviceRpcCallMsg));
                }
            }
            if (uplinkMsg.getWidgetBundleTypesRequestMsgCount() > 0) {
                for (WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg : uplinkMsg.getWidgetBundleTypesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processWidgetBundleTypesRequestMsg(edge.getTenantId(), edge, widgetBundleTypesRequestMsg));
                }
            }
            if (uplinkMsg.getEntityViewsRequestMsgCount() > 0) {
                for (EntityViewsRequestMsg entityViewRequestMsg : uplinkMsg.getEntityViewsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processEntityViewsRequestMsg(edge.getTenantId(), edge, entityViewRequestMsg));
                }
            }
        } catch (Exception e) {
            log.error("[{}] Can't process uplink msg [{}]", this.sessionId, uplinkMsg, e);
            return Futures.immediateFailedFuture(e);
        }
        return Futures.allAsList(result);
    }

    private ConnectResponseMsg processConnect(ConnectRequestMsg request) {
        log.trace("[{}] processConnect [{}]", this.sessionId, request);
        Optional<Edge> optional = ctx.getEdgeService().findEdgeByRoutingKey(TenantId.SYS_TENANT_ID, request.getEdgeRoutingKey());
        if (optional.isPresent()) {
            edge = optional.get();
            try {
                if (edge.getSecret().equals(request.getEdgeSecret())) {
                    sessionOpenListener.accept(edge.getId(), this);
                    this.edgeVersion = request.getEdgeVersion();
                    return ConnectResponseMsg.newBuilder()
                            .setResponseCode(ConnectResponseCode.ACCEPTED)
                            .setErrorMsg("")
                            .setConfiguration(ctx.getEdgeMsgConstructor().constructEdgeConfiguration(edge)).build();
                }
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                        .setErrorMsg("Failed to validate the edge!")
                        .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
            } catch (Exception e) {
                log.error("[{}] Failed to process edge connection!", request.getEdgeRoutingKey(), e);
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.SERVER_UNAVAILABLE)
                        .setErrorMsg("Failed to process edge connection!")
                        .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
            }
        }
        return ConnectResponseMsg.newBuilder()
                .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                .setErrorMsg("Failed to find the edge! Routing key: " + request.getEdgeRoutingKey())
                .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
    }

    @Override
    public void close() {
        log.debug("[{}] Closing session", sessionId);
        connected = false;
        try {
            outputStream.onCompleted();
        } catch (Exception e) {
            log.debug("[{}] Failed to close output stream: {}", sessionId, e.getMessage());
        }
    }

    private void interruptPreviousSendDownlinkMsgsTask() {
        String msg = String.format("[%s] Previous send downlink future was not properly completed, stopping it now!", this.sessionId);
        stopCurrentSendDownlinkMsgsTask(new RuntimeException(msg));
    }

    private void interruptGeneralProcessingOnSync(TenantId tenantId, EdgeId edgeId) {
        String msg = String.format("[%s][%s] Sync process started. General processing interrupted!", tenantId, edgeId);
        stopCurrentSendDownlinkMsgsTask(new RuntimeException(msg));
    }

    public void stopCurrentSendDownlinkMsgsTask(Exception e) {
        if (sessionState.getSendDownlinkMsgsFuture() != null && !sessionState.getSendDownlinkMsgsFuture().isDone()) {
            if (e != null) {
                log.warn(e.getMessage(), e);
                sessionState.getSendDownlinkMsgsFuture().setException(e);
            } else {
                sessionState.getSendDownlinkMsgsFuture().set(null);
            }
        }
        if (sessionState.getScheduledSendDownlinkTask() != null) {
            sessionState.getScheduledSendDownlinkTask().cancel(true);
        }
    }

}
