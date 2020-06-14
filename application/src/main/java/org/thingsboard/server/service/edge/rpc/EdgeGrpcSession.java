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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeQueueEntry;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.ConnectResponseCode;
import org.thingsboard.server.gen.edge.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.RequestMsg;
import org.thingsboard.server.gen.edge.RequestMsgType;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
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

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";

    private final UUID sessionId;
    private final BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener;
    private final Consumer<EdgeId> sessionCloseListener;
    private final ObjectMapper objectMapper;

    private EdgeContextComponent ctx;
    private Edge edge;
    private StreamObserver<RequestMsg> inputStream;
    private StreamObserver<ResponseMsg> outputStream;
    private boolean connected;

    EdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream, BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener,
                    Consumer<EdgeId> sessionCloseListener, ObjectMapper objectMapper) {
        this.sessionId = UUID.randomUUID();
        this.ctx = ctx;
        this.outputStream = outputStream;
        this.sessionOpenListener = sessionOpenListener;
        this.sessionCloseListener = sessionCloseListener;
        this.objectMapper = objectMapper;
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
                        ctx.getSyncEdgeService().sync(ctx, edge, outputStream);
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
        TimePageData<Event> pageData;
        UUID ifOffset = null;
        do {
            pageData = ctx.getEdgeNotificationService().findQueueEvents(edge.getTenantId(), edge.getId(), pageLink);
            if (isConnected() && !pageData.getData().isEmpty()) {
                log.trace("[{}] [{}] event(s) are going to be processed.", this.sessionId, pageData.getData().size());
                for (Event event : pageData.getData()) {
                    log.trace("[{}] Processing event [{}]", this.sessionId, event);
                    try {
                        EdgeQueueEntry entry = objectMapper.treeToValue(event.getBody(), EdgeQueueEntry.class);
                        UpdateMsgType msgType = getResponseMsgType(entry.getType());
                        switch (msgType) {
                            case ENTITY_DELETED_RPC_MESSAGE:
                            case ENTITY_UPDATED_RPC_MESSAGE:
                            case ENTITY_CREATED_RPC_MESSAGE:
                            case ALARM_ACK_RPC_MESSAGE:
                            case ALARM_CLEAR_RPC_MESSAGE:
                                processEntityCRUDMessage(entry, msgType);
                                break;
                            case RULE_CHAIN_CUSTOM_MESSAGE:
                                processCustomDownlinkMessage(entry);
                                break;
                        }
                        if (ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                            pushEntityAttributesToEdge(entry);
                        }
                    } catch (Exception e) {
                        log.error("Exception during processing records from queue", e);
                    }
                    ifOffset = event.getUuidId();
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

    private void pushEntityAttributesToEdge(EdgeQueueEntry entry) throws IOException {
        EntityId entityId = null;
        String entityName = null;
        switch (entry.getEntityType()) {
            case EDGE:
                Edge edge = objectMapper.readValue(entry.getData(), Edge.class);
                entityId = edge.getId();
                entityName = edge.getName();
                break;
            case DEVICE:
                Device device = objectMapper.readValue(entry.getData(), Device.class);
                entityId = device.getId();
                entityName = device.getName();
                break;
            case ASSET:
                Asset asset = objectMapper.readValue(entry.getData(), Asset.class);
                entityId = asset.getId();
                entityName = asset.getName();
                break;
            case ENTITY_VIEW:
                EntityView entityView = objectMapper.readValue(entry.getData(), EntityView.class);
                entityId = entityView.getId();
                entityName = entityView.getName();
                break;
            case DASHBOARD:
                Dashboard dashboard = objectMapper.readValue(entry.getData(), Dashboard.class);
                entityId = dashboard.getId();
                entityName = dashboard.getName();
                break;
        }
        if (entityId != null) {
            final EntityId finalEntityId = entityId;
            final String finalEntityName = entityName;
            ListenableFuture<List<AttributeKvEntry>> ssAttrFuture = ctx.getAttributesService().findAll(edge.getTenantId(), entityId, DataConstants.SERVER_SCOPE);
            Futures.transform(ssAttrFuture, ssAttributes -> {
                if (ssAttributes != null && !ssAttributes.isEmpty()) {
                    try {
                        TbMsgMetaData metaData = new TbMsgMetaData();
                        ObjectNode entityNode = objectMapper.createObjectNode();
                        metaData.putValue("scope", DataConstants.SERVER_SCOPE);
                        for (AttributeKvEntry attr : ssAttributes) {
                            if (attr.getDataType() == DataType.BOOLEAN) {
                                entityNode.put(attr.getKey(), attr.getBooleanValue().get());
                            } else if (attr.getDataType() == DataType.DOUBLE) {
                                entityNode.put(attr.getKey(), attr.getDoubleValue().get());
                            } else if (attr.getDataType() == DataType.LONG) {
                                entityNode.put(attr.getKey(), attr.getLongValue().get());
                            } else {
                                entityNode.put(attr.getKey(), attr.getValueAsString());
                            }
                        }
                        TbMsg tbMsg = TbMsg.newMsg(DataConstants.ATTRIBUTES_UPDATED, finalEntityId, metaData, TbMsgDataType.JSON
                                , objectMapper.writeValueAsString(entityNode));
                        log.debug("Sending donwlink entity data msg, entityName [{}], tbMsg [{}]", finalEntityName, tbMsg);
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setDownlinkMsg(constructDownlinkEntityDataMsg(finalEntityName, finalEntityId, tbMsg))
                                .build());
                    } catch (Exception e) {
                        log.error("[{}] Failed to send attribute updates to the edge", edge.getName(), e);
                    }
                }
                return null;
            }, MoreExecutors.directExecutor());
            ListenableFuture<List<AttributeKvEntry>> shAttrFuture = ctx.getAttributesService().findAll(edge.getTenantId(), entityId, DataConstants.SHARED_SCOPE);
            ListenableFuture<List<AttributeKvEntry>> clAttrFuture = ctx.getAttributesService().findAll(edge.getTenantId(), entityId, DataConstants.CLIENT_SCOPE);
        }
    }

    private void processCustomDownlinkMessage(EdgeQueueEntry entry) throws IOException {
        log.trace("Executing processCustomDownlinkMessage, entry [{}]", entry);
        TbMsg tbMsg = TbMsg.fromBytes(Base64.decodeBase64(entry.getData()), TbMsgCallback.EMPTY);
        String entityName = null;
        EntityId entityId = null;
        switch (entry.getEntityType()) {
            case DEVICE:
                Device device = ctx.getDeviceService().findDeviceById(edge.getTenantId(), new DeviceId(tbMsg.getOriginator().getId()));
                entityName = device.getName();
                entityId = device.getId();
                break;
            case ASSET:
                Asset asset = ctx.getAssetService().findAssetById(edge.getTenantId(), new AssetId(tbMsg.getOriginator().getId()));
                entityName = asset.getName();
                entityId = asset.getId();
                break;
            case ENTITY_VIEW:
                EntityView entityView = ctx.getEntityViewService().findEntityViewById(edge.getTenantId(), new EntityViewId(tbMsg.getOriginator().getId()));
                entityName = entityView.getName();
                entityId = entityView.getId();
                break;

        }
        if (entityName != null && entityId != null) {
            log.debug("Sending downlink entity data msg, entityName [{}], tbMsg [{}]", entityName, tbMsg);
            outputStream.onNext(ResponseMsg.newBuilder()
                    .setDownlinkMsg(constructDownlinkEntityDataMsg(entityName, entityId, tbMsg))
                    .build());
        }
    }

    private void processEntityCRUDMessage(EdgeQueueEntry entry, UpdateMsgType msgType) throws java.io.IOException {
        log.trace("Executing processEntityCRUDMessage, entry [{}], msgType [{}]", entry, msgType);
        switch (entry.getEntityType()) {
            case EDGE:
                Edge edge = objectMapper.readValue(entry.getData(), Edge.class);
                onEdgeUpdated(msgType, edge);
                break;
            case DEVICE:
                Device device = objectMapper.readValue(entry.getData(), Device.class);
                onDeviceUpdated(msgType, device);
                break;
            case ASSET:
                Asset asset = objectMapper.readValue(entry.getData(), Asset.class);
                onAssetUpdated(msgType, asset);
                break;
            case ENTITY_VIEW:
                EntityView entityView = objectMapper.readValue(entry.getData(), EntityView.class);
                onEntityViewUpdated(msgType, entityView);
                break;
            case DASHBOARD:
                Dashboard dashboard = objectMapper.readValue(entry.getData(), Dashboard.class);
                onDashboardUpdated(msgType, dashboard);
                break;
            case RULE_CHAIN:
                RuleChain ruleChain = objectMapper.readValue(entry.getData(), RuleChain.class);
                onRuleChainUpdated(msgType, ruleChain);
                break;
            case RULE_CHAIN_METADATA:
                RuleChainMetaData ruleChainMetaData = objectMapper.readValue(entry.getData(), RuleChainMetaData.class);
                onRuleChainMetadataUpdated(msgType, ruleChainMetaData);
                break;
            case ALARM:
                Alarm alarm = objectMapper.readValue(entry.getData(), Alarm.class);
                onAlarmUpdated(msgType, alarm);
                break;
            case USER:
                User user = objectMapper.readValue(entry.getData(), User.class);
                onUserUpdated(msgType, user);
                break;
            case RELATION:
                EntityRelation entityRelation = objectMapper.readValue(entry.getData(), EntityRelation.class);
                onEntityRelationUpdated(msgType, entityRelation);
                break;
        }
    }

    private void updateQueueStartTs(Long newStartTs) {
        newStartTs = ++newStartTs; // increments ts by 1 - next edge event search starts from current offset + 1
        List<AttributeKvEntry> attributes = Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs), System.currentTimeMillis()));
        ctx.getAttributesService().save(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, attributes);
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
        }, MoreExecutors.directExecutor());
    }

    private void onEdgeUpdated(UpdateMsgType msgType, Edge edge) {
        // TODO: voba add configuration update to edge
        this.edge = edge;
    }

    private void onDeviceUpdated(UpdateMsgType msgType, Device device) {
        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                .setDeviceUpdateMsg(ctx.getDeviceUpdateMsgConstructor().constructDeviceUpdatedMsg(msgType, device))
                .build();
        outputStream.onNext(ResponseMsg.newBuilder()
                .setEntityUpdateMsg(entityUpdateMsg)
                .build());
    }

    private void onAssetUpdated(UpdateMsgType msgType, Asset asset) {
        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                .setAssetUpdateMsg(ctx.getAssetUpdateMsgConstructor().constructAssetUpdatedMsg(msgType, asset))
                .build();
        outputStream.onNext(ResponseMsg.newBuilder()
                .setEntityUpdateMsg(entityUpdateMsg)
                .build());
    }

    private void onEntityViewUpdated(UpdateMsgType msgType, EntityView entityView) {
        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                .setEntityViewUpdateMsg(ctx.getEntityViewUpdateMsgConstructor().constructEntityViewUpdatedMsg(msgType, entityView))
                .build();
        outputStream.onNext(ResponseMsg.newBuilder()
                .setEntityUpdateMsg(entityUpdateMsg)
                .build());
    }

    private void onRuleChainUpdated(UpdateMsgType msgType, RuleChain ruleChain) {
        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                .setRuleChainUpdateMsg(ctx.getRuleChainUpdateMsgConstructor().constructRuleChainUpdatedMsg(edge.getRootRuleChainId(), msgType, ruleChain))
                .build();
        outputStream.onNext(ResponseMsg.newBuilder()
                .setEntityUpdateMsg(entityUpdateMsg)
                .build());
    }

    private void onRuleChainMetadataUpdated(UpdateMsgType msgType, RuleChainMetaData ruleChainMetaData) {
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

    private void onDashboardUpdated(UpdateMsgType msgType, Dashboard dashboard) {
        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                .setDashboardUpdateMsg(ctx.getDashboardUpdateMsgConstructor().constructDashboardUpdatedMsg(msgType, dashboard))
                .build();
        outputStream.onNext(ResponseMsg.newBuilder()
                .setEntityUpdateMsg(entityUpdateMsg)
                .build());
    }

    private void onAlarmUpdated(UpdateMsgType msgType, Alarm alarm) {
        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                .setAlarmUpdateMsg(ctx.getAlarmUpdateMsgConstructor().constructAlarmUpdatedMsg(edge.getTenantId(), msgType, alarm))
                .build();
        outputStream.onNext(ResponseMsg.newBuilder()
                .setEntityUpdateMsg(entityUpdateMsg)
                .build());
    }

    private void onUserUpdated(UpdateMsgType msgType, User user) {
        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                .setUserUpdateMsg(ctx.getUserUpdateMsgConstructor().constructUserUpdatedMsg(msgType, user))
                .build();
        outputStream.onNext(ResponseMsg.newBuilder()
                .setEntityUpdateMsg(entityUpdateMsg)
                .build());
    }

    private void onEntityRelationUpdated(UpdateMsgType msgType, EntityRelation entityRelation) {
        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                .setRelationUpdateMsg(ctx.getRelationUpdateMsgConstructor().constructRelationUpdatedMsg(msgType, entityRelation))
                .build();
        outputStream.onNext(ResponseMsg.newBuilder()
                .setEntityUpdateMsg(entityUpdateMsg)
                .build());
    }

    private UpdateMsgType getResponseMsgType(String msgType) {
        if (msgType.equals(SessionMsgType.POST_TELEMETRY_REQUEST.name()) ||
                msgType.equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name()) ||
                msgType.equals(DataConstants.ATTRIBUTES_UPDATED) ||
                msgType.equals(DataConstants.ATTRIBUTES_DELETED)) {
            return UpdateMsgType.RULE_CHAIN_CUSTOM_MESSAGE;
        } else {
            switch (msgType) {
                case DataConstants.ENTITY_UPDATED:
                    return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
                case DataConstants.ENTITY_CREATED:
                case DataConstants.ENTITY_ASSIGNED_TO_EDGE:
                    return ENTITY_CREATED_RPC_MESSAGE;
                case DataConstants.ENTITY_DELETED:
                case DataConstants.ENTITY_UNASSIGNED_FROM_EDGE:
                    return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
                case DataConstants.ALARM_ACK:
                    return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
                case DataConstants.ALARM_CLEAR:
                    return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
                default:
                    throw new RuntimeException("Unsupported msgType [" + msgType + "]");
            }
        }
    }

    private DownlinkMsg constructDownlinkEntityDataMsg(String entityName, EntityId entityId, TbMsg tbMsg) {
        EntityDataProto entityData = EntityDataProto.newBuilder()
                .setEntityName(entityName)
                .setTbMsg(ByteString.copyFrom(TbMsg.toByteArray(tbMsg)))
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .build();

        DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                .addAllEntityData(Collections.singletonList(entityData));

        return builder.build();
    }

    private CustomerUpdateMsg constructCustomerUpdatedMsg(UpdateMsgType msgType, Customer customer) {
        CustomerUpdateMsg.Builder builder = CustomerUpdateMsg.newBuilder()
                .setMsgType(msgType);
        return builder.build();
    }

    private UserUpdateMsg constructUserUpdatedMsg(UpdateMsgType msgType, User user) {
        UserUpdateMsg.Builder builder = UserUpdateMsg.newBuilder()
                .setMsgType(msgType);
        return builder.build();
    }

    private UplinkResponseMsg processUplinkMsg(UplinkMsg uplinkMsg) {
        try {
            if (uplinkMsg.getEntityDataList() != null && !uplinkMsg.getEntityDataList().isEmpty()) {
                for (EntityDataProto entityData : uplinkMsg.getEntityDataList()) {
                    TbMsg tbMsg = null;
                    TbMsg originalTbMsg = TbMsg.fromBytes(entityData.getTbMsg().toByteArray(), TbMsgCallback.EMPTY);
                    if (originalTbMsg.getOriginator().getEntityType() == EntityType.DEVICE) {
                        String deviceName = entityData.getEntityName();
                        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(edge.getTenantId(), deviceName);
                        if (device != null) {
                            tbMsg = TbMsg.newMsg(originalTbMsg.getType(), device.getId(), originalTbMsg.getMetaData().copy(),
                                    originalTbMsg.getDataType(), originalTbMsg.getData());
                        }
                    } else {
                        tbMsg = originalTbMsg;
                    }
                    if (tbMsg != null) {
                        ctx.getTbClusterService().pushMsgToRuleEngine(edge.getTenantId(), tbMsg.getOriginator(), tbMsg, null);
                    }
                }
            }
            if (uplinkMsg.getDeviceUpdateMsgList() != null && !uplinkMsg.getDeviceUpdateMsgList().isEmpty()) {
                for (DeviceUpdateMsg deviceUpdateMsg : uplinkMsg.getDeviceUpdateMsgList()) {
                    onDeviceUpdate(deviceUpdateMsg);
                }
            }
            if (uplinkMsg.getAlarmUpdateMsgList() != null && !uplinkMsg.getAlarmUpdateMsgList().isEmpty()) {
                for (AlarmUpdateMsg alarmUpdateMsg : uplinkMsg.getAlarmUpdateMsgList()) {
                    onAlarmUpdate(alarmUpdateMsg);
                }
            }
            if (uplinkMsg.getRuleChainMetadataRequestMsgList() != null && !uplinkMsg.getRuleChainMetadataRequestMsgList().isEmpty()) {
                for (RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg : uplinkMsg.getRuleChainMetadataRequestMsgList()) {
                    ctx.getSyncEdgeService().syncRuleChainMetadata(edge, ruleChainMetadataRequestMsg, outputStream);
                }
            }
        } catch (Exception e) {
            return UplinkResponseMsg.newBuilder().setSuccess(false).setErrorMsg(e.getMessage()).build();
        }

        return UplinkResponseMsg.newBuilder().setSuccess(true).build();
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
                        Device savedDevice = createDevice(deviceUpdateMsg);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setDeviceUpdateMsg(ctx.getDeviceUpdateMsgConstructor().constructDeviceUpdatedMsg(UpdateMsgType.DEVICE_CONFLICT_RPC_MESSAGE, savedDevice))
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                    } else {
                        createDevice(deviceUpdateMsg);
                    }
                }
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
        updateDeviceCredentials(deviceUpdateMsg, device);
    }

    private void updateDeviceCredentials(DeviceUpdateMsg deviceUpdateMsg, Device device) {
        log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                device.getName(), deviceUpdateMsg.getCredentialsId(), deviceUpdateMsg.getCredentialsValue());

        DeviceCredentials deviceCredentials = ctx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(edge.getTenantId(), device.getId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(deviceUpdateMsg.getCredentialsType()));
        deviceCredentials.setCredentialsId(deviceUpdateMsg.getCredentialsId());
        deviceCredentials.setCredentialsValue(deviceUpdateMsg.getCredentialsValue());
        ctx.getDeviceCredentialsService().updateDeviceCredentials(edge.getTenantId(), deviceCredentials);
        log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                device.getName(), deviceUpdateMsg.getCredentialsId(), deviceUpdateMsg.getCredentialsValue());

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
            device = ctx.getDeviceService().assignDeviceToEdge(edge.getTenantId(), device.getId(), edge.getId());
            createRelationFromEdge(device.getId());
            ctx.getRelationService().saveRelationAsync(TenantId.SYS_TENANT_ID, new EntityRelation(edge.getId(), device.getId(), "Created"));
            ctx.getDeviceStateService().onDeviceAdded(device);
            updateDeviceCredentials(deviceUpdateMsg, device);
        } finally {
            deviceCreationLock.unlock();
        }
        return device;
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
                        existentAlarm.setDetails(objectMapper.readTree(alarmUpdateMsg.getDetails()));
                        ctx.getAlarmService().createOrUpdateAlarm(existentAlarm);
                        break;
                    case ALARM_ACK_RPC_MESSAGE:
                        if (existentAlarm != null) {
                            ctx.getAlarmService().ackAlarm(edge.getTenantId(), existentAlarm.getId(), alarmUpdateMsg.getAckTs());
                        }
                        break;
                    case ALARM_CLEAR_RPC_MESSAGE:
                        if (existentAlarm != null) {
                            ctx.getAlarmService().clearAlarm(edge.getTenantId(), existentAlarm.getId(), objectMapper.readTree(alarmUpdateMsg.getDetails()), alarmUpdateMsg.getAckTs());
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
                .setTenantIdMSB(edge.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(edge.getTenantId().getId().getLeastSignificantBits())
                .setName(edge.getName())
                .setRoutingKey(edge.getRoutingKey())
                .setType(edge.getType().toString())
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
