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
package org.thingsboard.server.service.edge.rpc;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.ConnectResponseCode;
import org.thingsboard.server.gen.edge.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceProfileDevicesRequestMsg;
import org.thingsboard.server.gen.edge.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EdgeUpdateMsg;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RequestMsg;
import org.thingsboard.server.gen.edge.RequestMsgType;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.AssetsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerUsersEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DashboardsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DeviceProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.GeneralEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.RuleChainsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetsBundlesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantAdminUsersEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetsBundlesEdgeEventFetcher;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Data
public final class EdgeGrpcSession implements Closeable {

    private static final ReentrantLock downlinkMsgLock = new ReentrantLock();

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";

    private final UUID sessionId;
    private final BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener;
    private final Consumer<EdgeId> sessionCloseListener;
    private final ObjectMapper mapper;

    private EdgeContextComponent ctx;
    private Edge edge;
    private StreamObserver<RequestMsg> inputStream;
    private StreamObserver<ResponseMsg> outputStream;
    private boolean connected;
    private boolean syncCompleted;

    private ExecutorService syncExecutorService;

    private CountDownLatch latch;

    EdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream, BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener,
                    Consumer<EdgeId> sessionCloseListener, ObjectMapper mapper, ExecutorService syncExecutorService) {
        this.sessionId = UUID.randomUUID();
        this.ctx = ctx;
        this.outputStream = outputStream;
        this.sessionOpenListener = sessionOpenListener;
        this.sessionCloseListener = sessionCloseListener;
        this.mapper = mapper;
        this.syncExecutorService = syncExecutorService;
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
                if (connected && requestMsg.getMsgType().equals(RequestMsgType.SYNC_REQUEST_RPC_MESSAGE)) {
                    if (requestMsg.getSyncRequestMsg().getSyncRequired()) {
                        startSyncProcess(edge.getTenantId(), edge.getId());
                    }
                    syncCompleted = true;
                }
                if (connected) {
                    if (requestMsg.getMsgType().equals(RequestMsgType.UPLINK_RPC_MESSAGE) && requestMsg.hasUplinkMsg()) {
                        onUplinkMsg(requestMsg.getUplinkMsg());
                    }
                    if (requestMsg.getMsgType().equals(RequestMsgType.UPLINK_RPC_MESSAGE) && requestMsg.hasDownlinkResponseMsg()) {
                        onDownlinkResponse(requestMsg.getDownlinkResponseMsg());
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Failed to deliver message from client!", t);
                closeSession();
            }

            @Override
            public void onCompleted() {
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

    public void startSyncProcess(TenantId tenantId, EdgeId edgeId) {
        log.trace("[{}][{}] Staring edge sync process", tenantId, edgeId);
        syncExecutorService.submit(() -> {
            try {
                startProcessingEdgeEvents(new SystemWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
                startProcessingEdgeEvents(new TenantWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
                startProcessingEdgeEvents(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
                startProcessingEdgeEvents(new RuleChainsEdgeEventFetcher(ctx.getRuleChainService()));

                startProcessingEdgeEvents(new TenantAdminUsersEdgeEventFetcher(ctx.getUserService()));
                if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
                    EdgeEvent customerEdgeEvent = EdgeEventUtils.constructEdgeEvent(edge.getTenantId(), edge.getId(),
                            EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, edge.getCustomerId(), null);
                    DownlinkMsg customerDownlinkMsg = convertToDownlinkMsg(customerEdgeEvent);
                    sendDownlinkMsgsPack(Collections.singletonList(customerDownlinkMsg));

                    startProcessingEdgeEvents(new CustomerUsersEdgeEventFetcher(ctx.getUserService(), edge.getCustomerId()));
                }

                // TODO: voba - implement this functionality
                // syncAdminSettings(edge);

                startProcessingEdgeEvents(new AssetsEdgeEventFetcher(ctx.getAssetService()));
                startProcessingEdgeEvents(new DashboardsEdgeEventFetcher(ctx.getDashboardService()));

                DownlinkMsg syncCompleteDownlinkMsg = DownlinkMsg.newBuilder()
                        .setSyncCompletedMsg(SyncCompletedMsg.newBuilder().build())
                        .build();
                sendDownlinkMsgsPack(Collections.singletonList(syncCompleteDownlinkMsg));
            } catch (Exception e) {
                log.error("[{}][{}] Exception during sync process", edge.getTenantId(), edge.getId(), e);
            }
        });
    }

    private void onUplinkMsg(UplinkMsg uplinkMsg) {
        ListenableFuture<List<Void>> future = processUplinkMsg(uplinkMsg);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                UplinkResponseMsg uplinkResponseMsg = UplinkResponseMsg.newBuilder().setSuccess(true).build();
                sendDownlinkMsg(ResponseMsg.newBuilder()
                        .setUplinkResponseMsg(uplinkResponseMsg)
                        .build());
            }

            @Override
            public void onFailure(Throwable t) {
                UplinkResponseMsg uplinkResponseMsg = UplinkResponseMsg.newBuilder().setSuccess(false).setErrorMsg(t.getMessage()).build();
                sendDownlinkMsg(ResponseMsg.newBuilder()
                        .setUplinkResponseMsg(uplinkResponseMsg)
                        .build());
            }
        }, MoreExecutors.directExecutor());
    }

    private void onDownlinkResponse(DownlinkResponseMsg msg) {
        try {
            if (msg.getSuccess()) {
                log.debug("[{}] Msg has been processed successfully! {}", edge.getRoutingKey(), msg);
            } else {
                log.error("[{}] Msg processing failed! Error msg: {}", edge.getRoutingKey(), msg.getErrorMsg());
            }
            latch.countDown();
        } catch (Exception e) {
            log.error("[{}] Can't process downlink response message [{}]", this.sessionId, msg, e);
        }
    }

    private void sendDownlinkMsg(ResponseMsg downlinkMsg) {
        log.trace("[{}] Sending downlink msg [{}]", this.sessionId, downlinkMsg);
        if (isConnected()) {
            try {
                downlinkMsgLock.lock();
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
                .setConfiguration(constructEdgeConfigProto(edge)).build();
        ResponseMsg edgeConfigMsg = ResponseMsg.newBuilder()
                .setEdgeUpdateMsg(edgeConfig)
                .build();
        sendDownlinkMsg(edgeConfigMsg);
    }

    void processEdgeEvents() throws ExecutionException, InterruptedException {
        log.trace("[{}] processHandleMessages started", this.sessionId);
        if (isConnected() && isSyncCompleted()) {
            Long queueStartTs = getQueueStartTs().get();
            GeneralEdgeEventFetcher fetcher = new GeneralEdgeEventFetcher(
                    queueStartTs,
                    ctx.getEdgeEventService());
            UUID ifOffset = startProcessingEdgeEvents(fetcher);
            if (ifOffset != null) {
                Long newStartTs = Uuids.unixTimestamp(ifOffset);
                updateQueueStartTs(newStartTs);
            }
        }
        log.trace("[{}] processHandleMessages finished", this.sessionId);
    }

    private UUID startProcessingEdgeEvents(EdgeEventFetcher fetcher) throws InterruptedException {
        PageLink pageLink = fetcher.getPageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount());
        PageData<EdgeEvent> pageData;
        UUID ifOffset = null;
        boolean success;
        do {
            pageData = fetcher.fetchEdgeEvents(edge.getTenantId(), edge.getId(), pageLink);
            if (isConnected() && !pageData.getData().isEmpty()) {
                log.trace("[{}] [{}] event(s) are going to be processed.", this.sessionId, pageData.getData().size());
                List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(pageData.getData());
                success = sendDownlinkMsgsPack(downlinkMsgsPack);
                ifOffset = pageData.getData().get(pageData.getData().size() - 1).getUuidId();
                if (success) {
                    pageLink = pageLink.nextPageLink();
                }
            } else {
                log.trace("[{}] no event(s) found. Stop processing edge events", this.sessionId);
            }
        } while (isConnected() && pageData.hasNext());
        return ifOffset;
    }

    private boolean sendDownlinkMsgsPack(List<DownlinkMsg> downlinkMsgsPack) throws InterruptedException {
        boolean success;
        do {
            log.trace("[{}] [{}] downlink msg(s) are going to be send.", this.sessionId, downlinkMsgsPack.size());
            latch = new CountDownLatch(downlinkMsgsPack.size());
            for (DownlinkMsg downlinkMsg : downlinkMsgsPack) {
                sendDownlinkMsg(ResponseMsg.newBuilder()
                        .setDownlinkMsg(downlinkMsg)
                        .build());
            }
            success = latch.await(10, TimeUnit.SECONDS);
            if (!success) {
                log.warn("[{}] Failed to deliver the batch: {}", this.sessionId, downlinkMsgsPack);
            }
            if (isConnected() && !success) {
                try {
                    Thread.sleep(ctx.getEdgeEventStorageSettings().getSleepIntervalBetweenBatches());
                } catch (InterruptedException e) {
                    log.error("[{}] Error during sleep between next send of single downlink msg", this.sessionId, e);
                }
            }
        } while (isConnected() && !success);
        return success;
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
                    downlinkMsg = processEntityMessage(edgeEvent, edgeEvent.getAction());
                    log.trace("[{}][{}] entity message processed [{}]", edgeEvent.getTenantId(), this.sessionId, downlinkMsg);
                    break;
                case ATTRIBUTES_UPDATED:
                case POST_ATTRIBUTES:
                case ATTRIBUTES_DELETED:
                case TIMESERIES_UPDATED:
                    downlinkMsg = ctx.getTelemetryProcessor().processTelemetryMessageToEdge(edgeEvent);
                    break;
                case CREDENTIALS_REQUEST:
                    downlinkMsg = ctx.getEntityProcessor().processCredentialsRequestMessageToEdge(edgeEvent);
                    break;
                case ENTITY_MERGE_REQUEST:
                    downlinkMsg = ctx.getEntityProcessor().processEntityMergeRequestMessageToEdge(edge, edgeEvent);
                    break;
                case RPC_CALL:
                    downlinkMsg = ctx.getDeviceProcessor().processRpcCallMsgToEdge(edgeEvent);
                    break;
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
        }, ctx.getDbCallbackExecutor());
    }

    private void updateQueueStartTs(Long newStartTs) {
        log.trace("[{}] updating QueueStartTs [{}][{}]", this.sessionId, edge.getId(), newStartTs);
        newStartTs = ++newStartTs; // increments ts by 1 - next edge event search starts from current offset + 1
        List<AttributeKvEntry> attributes = Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs), System.currentTimeMillis()));
        ctx.getAttributesService().save(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, attributes);
    }

    private DownlinkMsg processEntityMessage(EdgeEvent edgeEvent, EdgeEventActionType action) {
        UpdateMsgType msgType = getResponseMsgType(edgeEvent.getAction());
        log.trace("Executing processEntityMessage, edgeEvent [{}], action [{}], msgType [{}]", edgeEvent, action, msgType);
        switch (edgeEvent.getType()) {
            case DEVICE:
                return ctx.getDeviceProcessor().processDeviceToEdge(edge, edgeEvent, msgType, action);
            case DEVICE_PROFILE:
                return ctx.getDeviceProfileProcessor().processDeviceProfileToEdge(edgeEvent, msgType, action);
            case ASSET:
                return ctx.getAssetProcessor().processAssetToEdge(edge, edgeEvent, msgType, action);
            case ENTITY_VIEW:
                return ctx.getEntityViewProcessor().processEntityViewToEdge(edge, edgeEvent, msgType, action);
            case DASHBOARD:
                return ctx.getDashboardProcessor().processDashboardToEdge(edge, edgeEvent, msgType, action);
            case CUSTOMER:
                return ctx.getCustomerProcessor().processCustomerToEdge(edgeEvent, msgType, action);
            case RULE_CHAIN:
                return ctx.getRuleChainProcessor().processRuleChainToEdge(edge, edgeEvent, msgType, action);
            case RULE_CHAIN_METADATA:
                return ctx.getRuleChainProcessor().processRuleChainMetadataToEdge(edgeEvent, msgType);
            case ALARM:
                return ctx.getAlarmProcessor().processAlarmToEdge(edge, edgeEvent, msgType);
            case USER:
                return ctx.getUserProcessor().processUserToEdge(edge, edgeEvent, msgType, action);
            case RELATION:
                return ctx.getRelationProcessor().processRelationToEdge(edgeEvent, msgType);
            case WIDGETS_BUNDLE:
                return ctx.getWidgetBundleProcessor().processWidgetsBundleToEdge(edgeEvent, msgType, action);
            case WIDGET_TYPE:
                return ctx.getWidgetTypeProcessor().processWidgetTypeToEdge(edgeEvent, msgType, action);
            case ADMIN_SETTINGS:
                return ctx.getAdminSettingsProcessor().processAdminSettingsToEdge(edgeEvent);
            default:
                log.warn("Unsupported edge event type [{}]", edgeEvent);
                return null;
        }
    }

    private UpdateMsgType getResponseMsgType(EdgeEventActionType actionType) {
        switch (actionType) {
            case UPDATED:
            case CREDENTIALS_UPDATED:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED:
            case ASSIGNED_TO_EDGE:
            case RELATION_ADD_OR_UPDATE:
                return UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
            case RELATION_DELETED:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        }
    }


    private ListenableFuture<List<Void>> processUplinkMsg(UplinkMsg uplinkMsg) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            if (uplinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : uplinkMsg.getEntityDataList()) {
                    result.addAll(ctx.getTelemetryProcessor().processTelemetryFromEdge(edge.getTenantId(), edge.getCustomerId(), entityData));
                }
            }
            if (uplinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : uplinkMsg.getDeviceUpdateMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceFromEdge(edge.getTenantId(), edge, deviceUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : uplinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceCredentialsFromEdge(edge.getTenantId(), deviceCredentialsUpdateMsg));
                }
            }
            if (uplinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : uplinkMsg.getAlarmUpdateMsgList()) {
                    result.add(ctx.getAlarmProcessor().processAlarmFromEdge(edge.getTenantId(), alarmUpdateMsg));
                }
            }
            if (uplinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : uplinkMsg.getRelationUpdateMsgList()) {
                    result.add(ctx.getRelationProcessor().processRelationFromEdge(edge.getTenantId(), relationUpdateMsg));
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
                    result.add(ctx.getDeviceProcessor().processDeviceRpcCallResponseFromEdge(edge.getTenantId(), deviceRpcCallMsg));
                }
            }
            if (uplinkMsg.getDeviceProfileDevicesRequestMsgCount() > 0) {
                for (DeviceProfileDevicesRequestMsg deviceProfileDevicesRequestMsg : uplinkMsg.getDeviceProfileDevicesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processDeviceProfileDevicesRequestMsg(edge.getTenantId(), edge, deviceProfileDevicesRequestMsg));
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
                    return ConnectResponseMsg.newBuilder()
                            .setResponseCode(ConnectResponseCode.ACCEPTED)
                            .setErrorMsg("")
                            .setConfiguration(constructEdgeConfigProto(edge)).build();
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

    private EdgeConfiguration constructEdgeConfigProto(Edge edge) {
        EdgeConfiguration.Builder builder = EdgeConfiguration.newBuilder()
                .setEdgeIdMSB(edge.getId().getId().getMostSignificantBits())
                .setEdgeIdLSB(edge.getId().getId().getLeastSignificantBits())
                .setTenantIdMSB(edge.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(edge.getTenantId().getId().getLeastSignificantBits())
                .setName(edge.getName())
                .setType(edge.getType())
                .setRoutingKey(edge.getRoutingKey())
                .setSecret(edge.getSecret())
                .setEdgeLicenseKey(edge.getEdgeLicenseKey())
                .setCloudEndpoint(edge.getCloudEndpoint())
                .setAdditionalInfo(JacksonUtil.toString(edge.getAdditionalInfo()))
                .setCloudType("CE");
        if (edge.getCustomerId() != null) {
            builder.setCustomerIdMSB(edge.getCustomerId().getId().getMostSignificantBits())
                    .setCustomerIdLSB(edge.getCustomerId().getId().getLeastSignificantBits());
        }
        return builder
                .build();
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
}
