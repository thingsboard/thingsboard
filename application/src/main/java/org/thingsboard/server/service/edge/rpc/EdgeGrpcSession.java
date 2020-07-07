/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.ConnectResponseCode;
import org.thingsboard.server.gen.edge.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RequestMsg;
import org.thingsboard.server.gen.edge.RequestMsgType;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.thingsboard.server.gen.edge.UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;

@Slf4j
@Data
public final class EdgeGrpcSession implements Closeable {

    private static final ReentrantLock deviceCreationLock = new ReentrantLock();

    private final Gson gson = new Gson();

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

    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> ruleEngineMsgProducer;

    EdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream, BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener,
                    Consumer<EdgeId> sessionCloseListener, ObjectMapper mapper) {
        this.sessionId = UUID.randomUUID();
        this.ctx = ctx;
        this.outputStream = outputStream;
        this.sessionOpenListener = sessionOpenListener;
        this.sessionCloseListener = sessionCloseListener;
        this.mapper = mapper;
        this.ruleEngineMsgProducer = ctx.getProducerProvider().getRuleEngineMsgProducer();
        initInputStream();
    }

    private void initInputStream() {
        this.inputStream = new StreamObserver<RequestMsg>() {
            @Override
            public void onNext(RequestMsg requestMsg) {
                if (!connected && requestMsg.getMsgType().equals(RequestMsgType.CONNECT_RPC_MESSAGE)) {
                    ConnectResponseMsg responseMsg = processConnect(requestMsg.getConnectRequestMsg());
                    outputStream.onNext(ResponseMsg.newBuilder()
                            .setConnectResponseMsg(responseMsg)
                            .build());
                    if (ConnectResponseCode.ACCEPTED != responseMsg.getResponseCode()) {
                        outputStream.onError(new RuntimeException(responseMsg.getErrorMsg()));
                    }
                    if (ConnectResponseCode.ACCEPTED == responseMsg.getResponseCode()) {
                        ctx.getSyncEdgeService().sync(edge);
                    }
                }
                if (connected) {
                    if (requestMsg.getMsgType().equals(RequestMsgType.UPLINK_RPC_MESSAGE) && requestMsg.hasUplinkMsg()) {
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setUplinkResponseMsg(processUplinkMsg(requestMsg.getUplinkMsg()))
                                .build());
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Failed to deliver message from client!", t);
            }

            @Override
            public void onCompleted() {
                sessionCloseListener.accept(edge.getId());
                outputStream.onCompleted();
            }
        };
    }

    void processHandleMessages() throws ExecutionException, InterruptedException {
        Long queueStartTs = getQueueStartTs().get();
        TimePageLink pageLink = new TimePageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount(), queueStartTs, null, true);
        TimePageData<EdgeEvent> pageData;
        UUID ifOffset = null;
        do {
            pageData = ctx.getEdgeNotificationService().findEdgeEvents(edge.getTenantId(), edge.getId(), pageLink);
            if (isConnected() && !pageData.getData().isEmpty()) {
                log.trace("[{}] [{}] event(s) are going to be processed.", this.sessionId, pageData.getData().size());
                for (EdgeEvent edgeEvent : pageData.getData()) {
                    log.trace("[{}] Processing edge event [{}]", this.sessionId, edgeEvent);
                    try {
                        ActionType edgeEventAction = ActionType.valueOf(edgeEvent.getEdgeEventAction());
                        switch (edgeEventAction) {
                            case UPDATED:
                            case ADDED:
                            case ASSIGNED_TO_EDGE:
                            case DELETED:
                            case UNASSIGNED_FROM_EDGE:
                            case ALARM_ACK:
                            case ALARM_CLEAR:
                            case CREDENTIALS_UPDATED:
                                processEntityMessage(edgeEvent, edgeEventAction);
                                break;
                            case ATTRIBUTES_UPDATED:
                            case ATTRIBUTES_DELETED:
                            case TIMESERIES_UPDATED:
                                processTelemetryMessage(edgeEvent);
                                break;
                        }
                    } catch (Exception e) {
                        log.error("Exception during processing records from queue", e);
                    }
                    ifOffset = edgeEvent.getUuidId();
                }
            }
            if (isConnected() && pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
                try {
                    Thread.sleep(ctx.getEdgeEventStorageSettings().getSleepIntervalBetweenBatches());
                } catch (InterruptedException e) {
                    log.error("Error during sleep between batches", e);
                }
            }
        } while (isConnected() && pageData.hasNext());

        if (ifOffset != null) {
            Long newStartTs = UUIDs.unixTimestamp(ifOffset);
            updateQueueStartTs(newStartTs);
        }
        try {
            Thread.sleep(ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval());
        } catch (InterruptedException e) {
            log.error("Error during sleep", e);
        }
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
        newStartTs = ++newStartTs; // increments ts by 1 - next edge event search starts from current offset + 1
        List<AttributeKvEntry> attributes = Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs), System.currentTimeMillis()));
        ctx.getAttributesService().save(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, attributes);
    }

    private void processTelemetryMessage(EdgeEvent edgeEvent) throws IOException {
        log.trace("Executing processTelemetryMessage, edgeEvent [{}]", edgeEvent);
        EntityId entityId = null;
        switch (edgeEvent.getEdgeEventType()) {
            case DEVICE:
                entityId = new DeviceId(edgeEvent.getEntityId());
                break;
            case ASSET:
                entityId = new AssetId(edgeEvent.getEntityId());
                break;
            case ENTITY_VIEW:
                entityId = new EntityViewId(edgeEvent.getEntityId());
                break;
            case DASHBOARD:
                entityId = new DashboardId(edgeEvent.getEntityId());
                break;
        }
        if (entityId != null) {
            log.debug("Sending telemetry data msg, entityId [{}], body [{}]", edgeEvent.getEntityId(), edgeEvent.getEntityBody());
            DownlinkMsg downlinkMsg;
            try {
                ActionType actionType = ActionType.valueOf(edgeEvent.getEdgeEventAction());
                downlinkMsg = constructEntityDataProtoMsg(entityId, actionType, JsonUtils.parse(mapper.writeValueAsString(edgeEvent.getEntityBody())));
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setDownlinkMsg(downlinkMsg)
                        .build());
            } catch (Exception e) {
                log.warn("Can't send telemetry data msg, entityId [{}], body [{}]", edgeEvent.getEntityId(), edgeEvent.getEntityBody(), e);
            }

        }
    }

    private void processEntityMessage(EdgeEvent edgeEvent, ActionType edgeEventAction) {
        UpdateMsgType msgType = getResponseMsgType(ActionType.valueOf(edgeEvent.getEdgeEventAction()));
        log.trace("Executing processEntityMessage, edgeEvent [{}], edgeEventAction [{}], msgType [{}]", edgeEvent, edgeEventAction, msgType);
        switch (edgeEvent.getEdgeEventType()) {
            case EDGE:
                // TODO: voba - add edge update logic
                break;
            case DEVICE:
                processDevice(edgeEvent, msgType, edgeEventAction);
                break;
            case ASSET:
                processAsset(edgeEvent, msgType, edgeEventAction);
                break;
            case ENTITY_VIEW:
                processEntityView(edgeEvent, msgType, edgeEventAction);
                break;
            case DASHBOARD:
                processDashboard(edgeEvent, msgType, edgeEventAction);
                break;
            case RULE_CHAIN:
                processRuleChain(edgeEvent, msgType, edgeEventAction);
                break;
            case RULE_CHAIN_METADATA:
                processRuleChainMetadata(edgeEvent, msgType);
                break;
            case ALARM:
                processAlarm(edgeEvent, msgType);
                break;
            case USER:
                processUser(edgeEvent, msgType, edgeEventAction);
                break;
            case RELATION:
                processRelation(edgeEvent, msgType);
                break;
        }
    }

    private void processDevice(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeActionType) {
        DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        EntityUpdateMsg entityUpdateMsg = null;
        switch (edgeActionType) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                Device device = ctx.getDeviceService().findDeviceById(edgeEvent.getTenantId(), deviceId);
                if (device != null) {
                    DeviceUpdateMsg deviceUpdateMsg =
                            ctx.getDeviceUpdateMsgConstructor().constructDeviceUpdatedMsg(msgType, device);
                    entityUpdateMsg = EntityUpdateMsg.newBuilder()
                            .setDeviceUpdateMsg(deviceUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                DeviceUpdateMsg deviceUpdateMsg =
                        ctx.getDeviceUpdateMsgConstructor().constructDeviceDeleteMsg(deviceId);
                entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setDeviceUpdateMsg(deviceUpdateMsg)
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                DeviceCredentials deviceCredentials = ctx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(edge.getTenantId(), deviceId);
                if (deviceCredentials != null) {
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg =
                            ctx.getDeviceUpdateMsgConstructor().constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                    entityUpdateMsg = EntityUpdateMsg.newBuilder()
                            .setDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsg)
                            .build();
                }
                break;
        }
        if (entityUpdateMsg != null) {
            outputStream.onNext(ResponseMsg.newBuilder()
                    .setEntityUpdateMsg(entityUpdateMsg)
                    .build());
        }
    }

    private void processAsset(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeEventAction) {
        AssetId assetId = new AssetId(edgeEvent.getEntityId());
        EntityUpdateMsg entityUpdateMsg = null;
        switch (edgeEventAction) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                Asset asset = ctx.getAssetService().findAssetById(edgeEvent.getTenantId(), assetId);
                if (asset != null) {
                    AssetUpdateMsg assetUpdateMsg =
                            ctx.getAssetUpdateMsgConstructor().constructAssetUpdatedMsg(msgType, asset);
                    entityUpdateMsg = EntityUpdateMsg.newBuilder()
                            .setAssetUpdateMsg(assetUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                AssetUpdateMsg assetUpdateMsg =
                        ctx.getAssetUpdateMsgConstructor().constructAssetDeleteMsg(assetId);
                entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setAssetUpdateMsg(assetUpdateMsg)
                        .build();
                break;
        }
        if (entityUpdateMsg != null) {
            outputStream.onNext(ResponseMsg.newBuilder()
                    .setEntityUpdateMsg(entityUpdateMsg)
                    .build());
        }
    }

    private void processEntityView(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeEventAction) {
        EntityViewId entityViewId = new EntityViewId(edgeEvent.getEntityId());
        EntityUpdateMsg entityUpdateMsg = null;
        switch (edgeEventAction) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                EntityView entityView = ctx.getEntityViewService().findEntityViewById(edgeEvent.getTenantId(), entityViewId);
                if (entityView != null) {
                    EntityViewUpdateMsg entityViewUpdateMsg =
                            ctx.getEntityViewUpdateMsgConstructor().constructEntityViewUpdatedMsg(msgType, entityView);
                    entityUpdateMsg = EntityUpdateMsg.newBuilder()
                            .setEntityViewUpdateMsg(entityViewUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                EntityViewUpdateMsg entityViewUpdateMsg =
                        ctx.getEntityViewUpdateMsgConstructor().constructEntityViewDeleteMsg(entityViewId);
                entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setEntityViewUpdateMsg(entityViewUpdateMsg)
                        .build();
                break;
        }
        if (entityUpdateMsg != null) {
            outputStream.onNext(ResponseMsg.newBuilder()
                    .setEntityUpdateMsg(entityUpdateMsg)
                    .build());
        }
    }

    private void processDashboard(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeEventAction) {
        DashboardId dashboardId = new DashboardId(edgeEvent.getEntityId());
        EntityUpdateMsg entityUpdateMsg = null;
        switch (edgeEventAction) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                Dashboard dashboard = ctx.getDashboardService().findDashboardById(edgeEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    DashboardUpdateMsg dashboardUpdateMsg =
                            ctx.getDashboardUpdateMsgConstructor().constructDashboardUpdatedMsg(msgType, dashboard);
                    entityUpdateMsg = EntityUpdateMsg.newBuilder()
                            .setDashboardUpdateMsg(dashboardUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                DashboardUpdateMsg dashboardUpdateMsg =
                        ctx.getDashboardUpdateMsgConstructor().constructDashboardDeleteMsg(dashboardId);
                entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setDashboardUpdateMsg(dashboardUpdateMsg)
                        .build();
                break;
        }
        if (entityUpdateMsg != null) {
            outputStream.onNext(ResponseMsg.newBuilder()
                    .setEntityUpdateMsg(entityUpdateMsg)
                    .build());
        }
    }

    private void processRuleChain(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeEventAction) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        EntityUpdateMsg entityUpdateMsg = null;
        switch (edgeEventAction) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                RuleChain ruleChain = ctx.getRuleChainService().findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
                if (ruleChain != null) {
                    RuleChainUpdateMsg ruleChainUpdateMsg =
                            ctx.getRuleChainUpdateMsgConstructor().constructRuleChainUpdatedMsg(edge.getRootRuleChainId(), msgType, ruleChain);
                    entityUpdateMsg = EntityUpdateMsg.newBuilder()
                            .setRuleChainUpdateMsg(ruleChainUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setRuleChainUpdateMsg(ctx.getRuleChainUpdateMsgConstructor().constructRuleChainDeleteMsg(ruleChainId))
                        .build();
                break;
        }
        if (entityUpdateMsg != null) {
            outputStream.onNext(ResponseMsg.newBuilder()
                    .setEntityUpdateMsg(entityUpdateMsg)
                    .build());
        }
    }

    private void processRuleChainMetadata(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        RuleChain ruleChain = ctx.getRuleChainService().findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
        if (ruleChain != null) {
            RuleChainMetaData ruleChainMetaData = ctx.getRuleChainService().loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                    ctx.getRuleChainUpdateMsgConstructor().constructRuleChainMetadataUpdatedMsg(msgType, ruleChainMetaData);
            if (ruleChainMetadataUpdateMsg != null) {
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg)
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        }
    }

    private void processUser(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeActionType) {
        UserId userId = new UserId(edgeEvent.getEntityId());
        EntityUpdateMsg entityUpdateMsg = null;
        switch (edgeActionType) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                User user = ctx.getUserService().findUserById(edgeEvent.getTenantId(), userId);
                if (user != null) {
                    entityUpdateMsg = EntityUpdateMsg.newBuilder()
                            .setUserUpdateMsg(ctx.getUserUpdateMsgConstructor().constructUserUpdatedMsg(msgType, user))
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setUserUpdateMsg(ctx.getUserUpdateMsgConstructor().constructUserDeleteMsg(userId))
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                UserCredentials userCredentialsByUserId = ctx.getUserService().findUserCredentialsByUserId(edge.getTenantId(), userId);
                if (userCredentialsByUserId != null) {
                    UserCredentialsUpdateMsg userCredentialsUpdateMsg =
                            ctx.getUserUpdateMsgConstructor().constructUserCredentialsUpdatedMsg(userCredentialsByUserId);
                    entityUpdateMsg = EntityUpdateMsg.newBuilder()
                            .setUserCredentialsUpdateMsg(userCredentialsUpdateMsg)
                            .build();
                }
        }
        if (entityUpdateMsg != null) {
            outputStream.onNext(ResponseMsg.newBuilder()
                    .setEntityUpdateMsg(entityUpdateMsg)
                    .build());
        }
    }

    private void processRelation(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        EntityRelation entityRelation = mapper.convertValue(edgeEvent.getEntityBody(), EntityRelation.class);
        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                .setRelationUpdateMsg(ctx.getRelationUpdateMsgConstructor().constructRelationUpdatedMsg(msgType, entityRelation))
                .build();
        outputStream.onNext(ResponseMsg.newBuilder()
                .setEntityUpdateMsg(entityUpdateMsg)
                .build());
    }

    private void processAlarm(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        try {
            AlarmId alarmId = new AlarmId(edgeEvent.getEntityId());
            Alarm alarm = ctx.getAlarmService().findAlarmByIdAsync(edgeEvent.getTenantId(), alarmId).get();
            if (alarm != null) {
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setAlarmUpdateMsg(ctx.getAlarmUpdateMsgConstructor().constructAlarmUpdatedMsg(edge.getTenantId(), msgType, alarm))
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        } catch (Exception e) {
            log.error("Can't process alarm msg [{}] [{}]", edgeEvent, msgType, e);
        }
    }

    private UpdateMsgType getResponseMsgType(ActionType actionType) {
        switch (actionType) {
            case UPDATED:
            case CREDENTIALS_UPDATED:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED:
            case ASSIGNED_TO_EDGE:
                return ENTITY_CREATED_RPC_MESSAGE;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        }
    }

    private DownlinkMsg constructEntityDataProtoMsg(EntityId entityId, ActionType actionType, JsonElement entityData) {
        EntityDataProto entityDataProto = ctx.getEntityDataMsgConstructor().constructEntityDataMsg(entityId, actionType, entityData);
        DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                .addAllEntityData(Collections.singletonList(entityDataProto));
        return builder.build();
    }

    private UplinkResponseMsg processUplinkMsg(UplinkMsg uplinkMsg) {
        try {
            if (uplinkMsg.getEntityDataList() != null && !uplinkMsg.getEntityDataList().isEmpty()) {
                for (EntityDataProto entityData : uplinkMsg.getEntityDataList()) {
                    EntityId entityId = constructEntityId(entityData);
                    if ((entityData.hasPostAttributesMsg() || entityData.hasPostTelemetryMsg()) && entityId != null) {
                        ListenableFuture<TbMsgMetaData> metaDataFuture = constructBaseMsgMetadata(entityId);
                        Futures.transform(metaDataFuture, metaData -> {
                            if (metaData != null) {
                                metaData.putValue(DataConstants.MSG_SOURCE_KEY, DataConstants.EDGE_MSG_SOURCE);
                                if (entityData.hasPostAttributesMsg()) {
                                    processPostAttributes(entityId, entityData.getPostAttributesMsg(), metaData);
                                }
                                if (entityData.hasPostTelemetryMsg()) {
                                    processPostTelemetry(entityId, entityData.getPostTelemetryMsg(), metaData);
                                }
                            }
                            return null;
                        }, ctx.getDbCallbackExecutor());
                    }
                }
            }

            if (uplinkMsg.getDeviceUpdateMsgList() != null && !uplinkMsg.getDeviceUpdateMsgList().isEmpty()) {
                for (DeviceUpdateMsg deviceUpdateMsg : uplinkMsg.getDeviceUpdateMsgList()) {
                    onDeviceUpdate(deviceUpdateMsg);
                }
            }
            if (uplinkMsg.getDeviceCredentialsUpdateMsgList() != null && !uplinkMsg.getDeviceCredentialsUpdateMsgList().isEmpty()) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : uplinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    onDeviceCredentialsUpdate(deviceCredentialsUpdateMsg);
                }
            }
            if (uplinkMsg.getAlarmUpdateMsgList() != null && !uplinkMsg.getAlarmUpdateMsgList().isEmpty()) {
                for (AlarmUpdateMsg alarmUpdateMsg : uplinkMsg.getAlarmUpdateMsgList()) {
                    onAlarmUpdate(alarmUpdateMsg);
                }
            }
            if (uplinkMsg.getRuleChainMetadataRequestMsgList() != null && !uplinkMsg.getRuleChainMetadataRequestMsgList().isEmpty()) {
                for (RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg : uplinkMsg.getRuleChainMetadataRequestMsgList()) {
                    ctx.getSyncEdgeService().processRuleChainMetadataRequestMsg(edge, ruleChainMetadataRequestMsg);
                }
            }
            if (uplinkMsg.getAttributesRequestMsgList() != null && !uplinkMsg.getAttributesRequestMsgList().isEmpty()) {
                for (AttributesRequestMsg attributesRequestMsg : uplinkMsg.getAttributesRequestMsgList()) {
                    ctx.getSyncEdgeService().processAttributesRequestMsg(edge, attributesRequestMsg);
                }
            }
            if (uplinkMsg.getRelationRequestMsgList() != null && !uplinkMsg.getRelationRequestMsgList().isEmpty()) {
                for (RelationRequestMsg relationRequestMsg : uplinkMsg.getRelationRequestMsgList()) {
                    ctx.getSyncEdgeService().processRelationRequestMsg(edge, relationRequestMsg);
                }
            }
            if (uplinkMsg.getUserCredentialsRequestMsgList() != null && !uplinkMsg.getUserCredentialsRequestMsgList().isEmpty()) {
                for (UserCredentialsRequestMsg userCredentialsRequestMsg : uplinkMsg.getUserCredentialsRequestMsgList()) {
                    ctx.getSyncEdgeService().processUserCredentialsRequestMsg(edge, userCredentialsRequestMsg);
                }
            }
            if (uplinkMsg.getDeviceCredentialsRequestMsgList() != null && !uplinkMsg.getDeviceCredentialsRequestMsgList().isEmpty()) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : uplinkMsg.getDeviceCredentialsRequestMsgList()) {
                    ctx.getSyncEdgeService().processDeviceCredentialsRequestMsg(edge, deviceCredentialsRequestMsg);
                }
            }
        } catch (Exception e) {
            return UplinkResponseMsg.newBuilder().setSuccess(false).setErrorMsg(e.getMessage()).build();
        }

        return UplinkResponseMsg.newBuilder().setSuccess(true).build();
    }

    private ListenableFuture<TbMsgMetaData> constructBaseMsgMetadata(EntityId entityId) {
        switch (entityId.getEntityType()) {
            case DEVICE:
                ListenableFuture<Device> deviceFuture = ctx.getDeviceService().findDeviceByIdAsync(edge.getTenantId(), new DeviceId(entityId.getId()));
                return Futures.transform(deviceFuture, device -> {
                    TbMsgMetaData metaData = new TbMsgMetaData();
                    if (device != null) {
                        metaData.putValue("deviceName", device.getName());
                        metaData.putValue("deviceType", device.getType());
                    }
                    return metaData;
                }, ctx.getDbCallbackExecutor());
            case ASSET:
                ListenableFuture<Asset> assetFuture = ctx.getAssetService().findAssetByIdAsync(edge.getTenantId(), new AssetId(entityId.getId()));
                return Futures.transform(assetFuture, asset -> {
                    TbMsgMetaData metaData = new TbMsgMetaData();
                    if (asset != null) {
                        metaData.putValue("assetName", asset.getName());
                        metaData.putValue("assetType", asset.getType());
                    }
                    return metaData;
                }, ctx.getDbCallbackExecutor());
            case ENTITY_VIEW:
                ListenableFuture<EntityView> entityViewFuture = ctx.getEntityViewService().findEntityViewByIdAsync(edge.getTenantId(), new EntityViewId(entityId.getId()));
                return Futures.transform(entityViewFuture, entityView -> {
                    TbMsgMetaData metaData = new TbMsgMetaData();
                    if (entityView != null) {
                        metaData.putValue("entityViewName", entityView.getName());
                        metaData.putValue("entityViewType", entityView.getType());
                    }
                    return metaData;
                }, ctx.getDbCallbackExecutor());
            default:
                log.debug("Constructing empty metadata for entityId [{}]", entityId);
                return Futures.immediateFuture(new TbMsgMetaData());
        }
    }

    private EntityId constructEntityId(EntityDataProto entityData) {
        EntityType entityType = EntityType.valueOf(entityData.getEntityType());
        switch (entityType) {
            case DEVICE:
                return new DeviceId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ASSET:
                return new AssetId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ENTITY_VIEW:
                return new EntityViewId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case DASHBOARD:
                return new DashboardId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            default:
                log.warn("Unsupported entity type [{}] during construct of entity id. EntityDataProto [{}]", entityData.getEntityType(), entityData);
                return null;
        }
    }

    private void processPostTelemetry(EntityId entityId, TransportProtos.PostTelemetryMsg msg, TbMsgMetaData metaData) {
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
            metaData.putValue("ts", tsKv.getTs() + "");
            TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), entityId, metaData, gson.toJson(json));
            // TODO: voba - verify that null callback is OK
            ctx.getTbClusterService().pushMsgToRuleEngine(edge.getTenantId(), tbMsg.getOriginator(), tbMsg, null);
        }
    }

    private void processPostAttributes(EntityId entityId, TransportProtos.PostAttributeMsg msg, TbMsgMetaData metaData) {
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_ATTRIBUTES_REQUEST.name(), entityId, metaData, gson.toJson(json));
        // TODO: voba - verify that null callback is OK
        ctx.getTbClusterService().pushMsgToRuleEngine(edge.getTenantId(), tbMsg.getOriginator(), tbMsg, null);
    }

    private void onDeviceUpdate(DeviceUpdateMsg deviceUpdateMsg) {
        log.info("onDeviceUpdate {}", deviceUpdateMsg);
        DeviceId edgeDeviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
                String deviceName = deviceUpdateMsg.getName();
                Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(edge.getTenantId(), deviceName);
                if (device != null) {
                    // device with this name already exists on the cloud - update ID on the edge
                    if (!device.getId().equals(edgeDeviceId)) {
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setDeviceUpdateMsg(ctx.getDeviceUpdateMsgConstructor().constructDeviceUpdatedMsg(UpdateMsgType.DEVICE_CONFLICT_RPC_MESSAGE, device))
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                    }
                } else {
                    Device deviceById = ctx.getDeviceService().findDeviceById(edge.getTenantId(), edgeDeviceId);
                    if (deviceById != null) {
                        // this ID already used by other device - create new device and update ID on the edge
                        device = createDevice(deviceUpdateMsg);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setDeviceUpdateMsg(ctx.getDeviceUpdateMsgConstructor().constructDeviceUpdatedMsg(UpdateMsgType.DEVICE_CONFLICT_RPC_MESSAGE, device))
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                    } else {
                        device = createDevice(deviceUpdateMsg);
                    }
                }
                // TODO: voba - assign device only in case device is not assigned yet. Missing functionality to check this relation prior assignment
                ctx.getDeviceService().assignDeviceToEdge(edge.getTenantId(), device.getId(), edge.getId());
                break;
            case ENTITY_UPDATED_RPC_MESSAGE:
                updateDevice(deviceUpdateMsg);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                Device deviceToDelete = ctx.getDeviceService().findDeviceById(edge.getTenantId(), edgeDeviceId);
                if (deviceToDelete != null) {
                    ctx.getDeviceService().unassignDeviceFromEdge(edge.getTenantId(), edgeDeviceId, edge.getId());
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }
    }

    private void updateDevice(DeviceUpdateMsg deviceUpdateMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        Device device = ctx.getDeviceService().findDeviceById(edge.getTenantId(), deviceId);
        device.setName(deviceUpdateMsg.getName());
        device.setType(deviceUpdateMsg.getType());
        device.setLabel(deviceUpdateMsg.getLabel());
        device = ctx.getDeviceService().saveDevice(device);

        requestDeviceCredentialsFromEdge(device);
    }

    private void onDeviceCredentialsUpdate(DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        log.debug("Executing onDeviceCredentialsUpdate, deviceCredentialsUpdateMsg [{}]", deviceCredentialsUpdateMsg);
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsUpdateMsg.getDeviceIdMSB(), deviceCredentialsUpdateMsg.getDeviceIdLSB()));
        ListenableFuture<Device> deviceFuture = ctx.getDeviceService().findDeviceByIdAsync(edge.getTenantId(), deviceId);

        Futures.addCallback(deviceFuture, new FutureCallback<Device>() {
            @Override
            public void onSuccess(@Nullable Device device) {
                if (device != null) {
                    log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                            device.getName(), deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentialsUpdateMsg.getCredentialsValue());
                    try {
                        DeviceCredentials deviceCredentials = ctx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(edge.getTenantId(), device.getId());
                        deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(deviceCredentialsUpdateMsg.getCredentialsType()));
                        deviceCredentials.setCredentialsId(deviceCredentialsUpdateMsg.getCredentialsId());
                        deviceCredentials.setCredentialsValue(deviceCredentialsUpdateMsg.getCredentialsValue());
                        ctx.getDeviceCredentialsService().updateDeviceCredentials(edge.getTenantId(), deviceCredentials);
                    } catch (Exception e) {
                        log.error("Can't update device credentials for device [{}], deviceCredentialsUpdateMsg [{}]", device.getName(), deviceCredentialsUpdateMsg, e);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Can't update device credentials for deviceCredentialsUpdateMsg [{}]", deviceCredentialsUpdateMsg, t);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private void requestDeviceCredentialsFromEdge(Device device) {
        log.debug("Executing requestDeviceCredentialsFromEdge device [{}]", device);

        DownlinkMsg downlinkMsg = constructDeviceCredentialsRequestMsg(device.getId());
        outputStream.onNext(ResponseMsg.newBuilder()
                .setDownlinkMsg(downlinkMsg)
                .build());
    }

    private DownlinkMsg constructDeviceCredentialsRequestMsg(DeviceId deviceId) {
        DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .build();
        DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                .addAllDeviceCredentialsRequestMsg(Collections.singletonList(deviceCredentialsRequestMsg));
        return builder.build();
    }

    private Device createDevice(DeviceUpdateMsg deviceUpdateMsg) {
        Device device;
        try {
            deviceCreationLock.lock();
            DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
            device = new Device();
            device.setTenantId(edge.getTenantId());
            device.setCustomerId(edge.getCustomerId());
            device.setId(deviceId);
            device.setName(deviceUpdateMsg.getName());
            device.setType(deviceUpdateMsg.getType());
            device.setLabel(deviceUpdateMsg.getLabel());
            device = ctx.getDeviceService().saveDevice(device);
            createDeviceCredentials(device);
            createRelationFromEdge(device.getId());
            ctx.getDeviceStateService().onDeviceAdded(device);
            pushDeviceCreatedEventToRuleEngine(device);
            requestDeviceCredentialsFromEdge(device);
        } finally {
            deviceCreationLock.unlock();
        }
        return device;
    }

    private void createDeviceCredentials(Device device) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(device.getId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(RandomStringUtils.randomAlphanumeric(20));
        ctx.getDeviceCredentialsService().createDeviceCredentials(device.getTenantId(), deviceCredentials);
    }

    private void pushDeviceCreatedEventToRuleEngine(Device device) {
        try {
            ObjectNode entityNode = mapper.valueToTree(device);
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, device.getId(), getActionTbMsgMetaData(device.getCustomerId()), mapper.writeValueAsString(entityNode));
            sendToRuleEngine(edge.getTenantId(), tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    // TODO: voba - handle success
                }

                @Override
                public void onFailure(Throwable t) {
                    // TODO: voba - handle failure
                }
            });
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    protected void sendToRuleEngine(TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = ctx.getPartitionService().resolve(ServiceType.TB_RULE_ENGINE, tenantId, tbMsg.getOriginator());
        TransportProtos.ToRuleEngineMsg msg = TransportProtos.ToRuleEngineMsg.newBuilder().setTbMsg(TbMsg.toByteString(tbMsg))
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).build();
        ruleEngineMsgProducer.send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
    }

    private TbMsgMetaData getActionTbMsgMetaData(CustomerId customerId) {
        TbMsgMetaData metaData = getTbMsgMetaData(edge);
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    private TbMsgMetaData getTbMsgMetaData(Edge edge) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("edgeId", edge.getId().toString());
        metaData.putValue("edgeName", edge.getName());
        return metaData;
    }

    private EntityId getAlarmOriginator(String entityName, org.thingsboard.server.common.data.EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return ctx.getDeviceService().findDeviceByTenantIdAndName(edge.getTenantId(), entityName).getId();
            case ASSET:
                return ctx.getAssetService().findAssetByTenantIdAndName(edge.getTenantId(), entityName).getId();
            case ENTITY_VIEW:
                return ctx.getEntityViewService().findEntityViewByTenantIdAndName(edge.getTenantId(), entityName).getId();
            default:
                return null;
        }
    }

    private void onAlarmUpdate(AlarmUpdateMsg alarmUpdateMsg) {
        EntityId originatorId = getAlarmOriginator(alarmUpdateMsg.getOriginatorName(), org.thingsboard.server.common.data.EntityType.valueOf(alarmUpdateMsg.getOriginatorType()));
        if (originatorId != null) {
            try {
                Alarm existentAlarm = ctx.getAlarmService().findLatestByOriginatorAndType(edge.getTenantId(), originatorId, alarmUpdateMsg.getType()).get();
                switch (alarmUpdateMsg.getMsgType()) {
                    case ENTITY_CREATED_RPC_MESSAGE:
                    case ENTITY_UPDATED_RPC_MESSAGE:
                        if (existentAlarm == null) {
                            existentAlarm = new Alarm();
                            existentAlarm.setTenantId(edge.getTenantId());
                            existentAlarm.setType(alarmUpdateMsg.getName());
                            existentAlarm.setOriginator(originatorId);
                            existentAlarm.setSeverity(AlarmSeverity.valueOf(alarmUpdateMsg.getSeverity()));
                            existentAlarm.setStatus(AlarmStatus.valueOf(alarmUpdateMsg.getStatus()));
                            existentAlarm.setStartTs(alarmUpdateMsg.getStartTs());
                            existentAlarm.setAckTs(alarmUpdateMsg.getAckTs());
                            existentAlarm.setClearTs(alarmUpdateMsg.getClearTs());
                            existentAlarm.setPropagate(alarmUpdateMsg.getPropagate());
                        }
                        existentAlarm.setEndTs(alarmUpdateMsg.getEndTs());
                        existentAlarm.setDetails(mapper.readTree(alarmUpdateMsg.getDetails()));
                        ctx.getAlarmService().createOrUpdateAlarm(existentAlarm);
                        break;
                    case ALARM_ACK_RPC_MESSAGE:
                        if (existentAlarm != null) {
                            ctx.getAlarmService().ackAlarm(edge.getTenantId(), existentAlarm.getId(), alarmUpdateMsg.getAckTs());
                        }
                        break;
                    case ALARM_CLEAR_RPC_MESSAGE:
                        if (existentAlarm != null) {
                            ctx.getAlarmService().clearAlarm(edge.getTenantId(), existentAlarm.getId(), mapper.readTree(alarmUpdateMsg.getDetails()), alarmUpdateMsg.getAckTs());
                        }
                        break;
                    case ENTITY_DELETED_RPC_MESSAGE:
                        if (existentAlarm != null) {
                            ctx.getAlarmService().deleteAlarm(edge.getTenantId(), existentAlarm.getId());
                        }
                        break;
                }
            } catch (Exception e) {
                log.error("Error during finding existent alarm", e);
            }
        }
    }

    private ConnectResponseMsg processConnect(ConnectRequestMsg request) {
        Optional<Edge> optional = ctx.getEdgeService().findEdgeByRoutingKey(TenantId.SYS_TENANT_ID, request.getEdgeRoutingKey());
        if (optional.isPresent()) {
            edge = optional.get();
            try {
                if (edge.getSecret().equals(request.getEdgeSecret())) {
                    connected = true;
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

    private void createRelationFromEdge(EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(edge.getId());
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.EDGE_TYPE);
        ctx.getRelationService().saveRelation(edge.getTenantId(), relation);
    }

    private EdgeConfiguration constructEdgeConfigProto(Edge edge) throws JsonProcessingException {
        return EdgeConfiguration.newBuilder()
                .setEdgeIdMSB(edge.getId().getId().getMostSignificantBits())
                .setEdgeIdLSB(edge.getId().getId().getLeastSignificantBits())
                .setTenantIdMSB(edge.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(edge.getTenantId().getId().getLeastSignificantBits())
                .setName(edge.getName())
                .setRoutingKey(edge.getRoutingKey())
                .setType(edge.getType())
                .setCloudType("CE")
                .build();
    }

    @Override
    public void close() {
        connected = false;
        try {
            outputStream.onCompleted();
        } catch (Exception e) {
            log.debug("[{}] Failed to close output stream: {}", sessionId, e.getMessage());
        }
    }
}
