/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeQueueEntry;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.ConnectResponseCode;
import org.thingsboard.server.gen.edge.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.RequestMsg;
import org.thingsboard.server.gen.edge.RequestMsgType;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@Data
public final class EdgeGrpcSession implements Cloneable {

    private final UUID sessionId;
    private final BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener;
    private final Consumer<EdgeId> sessionCloseListener;
    private final ObjectMapper objectMapper;

    private EdgeContextComponent ctx;
    private Edge edge;
    private StreamObserver<RequestMsg> inputStream;
    private StreamObserver<ResponseMsg> outputStream;
    private boolean connected;

    private EdgeService edgeService;
    private AssetService assetService;
    private DeviceService deviceService;
    private AttributesService attributesService;

    EdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream,
                    BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener, Consumer<EdgeId> sessionCloseListener,
                    EdgeService edgeService, AssetService assetService,  DeviceService deviceService, AttributesService attributesService, ObjectMapper objectMapper) {
        this.sessionId = UUID.randomUUID();
        this.ctx = ctx;
        this.outputStream = outputStream;
        this.sessionOpenListener = sessionOpenListener;
        this.sessionCloseListener = sessionCloseListener;
        this.objectMapper = objectMapper;
        this.edgeService = edgeService;
        this.assetService = assetService;
        this.deviceService = deviceService;
        this.attributesService = attributesService;
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
        // TODO: this 100 value must be chagned properly
        TimePageLink pageLink = new TimePageLink(30, queueStartTs + 1000);
        TimePageData<Event> pageData;
        UUID ifOffset = null;
        do {
            pageData =  edgeService.findQueueEvents(edge.getTenantId(), edge.getId(), pageLink);
            if (!pageData.getData().isEmpty()) {
                for (Event event : pageData.getData()) {
                    EdgeQueueEntry entry;
                    try {
                        entry = objectMapper.treeToValue(event.getBody(), EdgeQueueEntry.class);
                        UpdateMsgType msgType = getResponseMsgType(entry.getType());
                        switch (entry.getEntityType()) {
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
                        }
                    } catch (Exception e) {
                        log.error("Exception during processing records from queue", e);
                    }
                    ifOffset = event.getUuidId();
                }
            }
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        if (ifOffset != null) {
            Long newStartTs = UUIDs.unixTimestamp(ifOffset);
            updateQueueStartTs(newStartTs);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("Error during sleep", e);
        }
    }

    private void updateQueueStartTs(Long newStartTs) {
        List<AttributeKvEntry> attributes = Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry("queueStartTs", newStartTs), System.currentTimeMillis()));
        attributesService.save(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, attributes);
    }

    private ListenableFuture<Long> getQueueStartTs() {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                attributesService.find(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, "queueStartTs");
        return Futures.transform(future, attributeKvEntryOpt -> {
                    if (attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent()) {
                        AttributeKvEntry attributeKvEntry = attributeKvEntryOpt.get();
                        return attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
                    } else {
                        return 0L;
                    }
                } );
    }

    private void onDeviceUpdated(UpdateMsgType msgType, Device device) {
        outputStream.onNext(ResponseMsg.newBuilder()
                .setDeviceUpdateMsg(constructDeviceUpdatedMsg(msgType, device))
                .build());
    }

    private void onAssetUpdated(UpdateMsgType msgType, Asset asset) {
        outputStream.onNext(ResponseMsg.newBuilder()
                .setAssetUpdateMsg(constructAssetUpdatedMsg(msgType, asset))
                .build());
    }

    private void onEntityViewUpdated(UpdateMsgType msgType, EntityView entityView) {
        outputStream.onNext(ResponseMsg.newBuilder()
                .setEntityViewUpdateMsg(constructEntityViewUpdatedMsg(msgType, entityView))
                .build());
    }

    private void onRuleChainUpdated(UpdateMsgType msgType, RuleChain ruleChain) {
        outputStream.onNext(ResponseMsg.newBuilder()
                .setRuleChainUpdateMsg(constructRuleChainUpdatedMsg(msgType, ruleChain))
                .build());
    }

    private void onDashboardUpdated(UpdateMsgType msgType, Dashboard dashboard) {
        outputStream.onNext(ResponseMsg.newBuilder()
                .setDashboardUpdateMsg(constructDashboardUpdatedMsg(msgType, dashboard))
                .build());
    }

    private UpdateMsgType getResponseMsgType(String msgType) {
        switch (msgType) {
            case DataConstants.ENTITY_UPDATED:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case DataConstants.ENTITY_CREATED:
            case DataConstants.ENTITY_ASSIGNED_TO_EDGE:
                return UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DataConstants.ENTITY_DELETED:
            case DataConstants.ENTITY_UNASSIGNED_FROM_EDGE:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported mstType [" + msgType + "]");
        }
    }

    private RuleChainUpdateMsg constructRuleChainUpdatedMsg(UpdateMsgType msgType, RuleChain ruleChain) {
        RuleChainUpdateMsg.Builder builder = RuleChainUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(ruleChain.getId().getId().getMostSignificantBits())
                .setIdLSB(ruleChain.getId().getId().getLeastSignificantBits())
                .setName(ruleChain.getName())
                .setRoot(ruleChain.isRoot())
                .setDebugMode(ruleChain.isDebugMode())
                .setConfiguration(JacksonUtil.toString(ruleChain.getConfiguration()));
        if (ruleChain.getFirstRuleNodeId() != null) {
             builder.setFirstRuleNodeIdMSB(ruleChain.getFirstRuleNodeId().getId().getMostSignificantBits())
                    .setFirstRuleNodeIdLSB(ruleChain.getFirstRuleNodeId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    private DashboardUpdateMsg constructDashboardUpdatedMsg(UpdateMsgType msgType, Dashboard dashboard) {
        DashboardUpdateMsg.Builder builder = DashboardUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(dashboard.getId().getId().getMostSignificantBits())
                .setIdLSB(dashboard.getId().getId().getLeastSignificantBits())
                .setName(dashboard.getName());
        return builder.build();
    }

    private DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device) {
        DeviceUpdateMsg.Builder builder = DeviceUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setName(device.getName())
                .setType(device.getName());
        return builder.build();
    }

    private AssetUpdateMsg constructAssetUpdatedMsg(UpdateMsgType msgType, Asset asset) {
        AssetUpdateMsg.Builder builder = AssetUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setName(asset.getName())
                .setType(asset.getName());
        return builder.build();
    }

    private EntityViewUpdateMsg constructEntityViewUpdatedMsg(UpdateMsgType msgType, EntityView entityView) {
        String relatedName;
        String relatedType;
        org.thingsboard.server.gen.edge.EntityType relatedEntityType;
        if (entityView.getEntityId().getEntityType().equals(EntityType.DEVICE)) {
            Device device = deviceService.findDeviceById(entityView.getTenantId(), new DeviceId(entityView.getEntityId().getId()));
            relatedName = device.getName();
            relatedType = device.getType();
            relatedEntityType = org.thingsboard.server.gen.edge.EntityType.DEVICE;
        } else {
            Asset asset = assetService.findAssetById(entityView.getTenantId(), new AssetId(entityView.getEntityId().getId()));
            relatedName = asset.getName();
            relatedType = asset.getType();
            relatedEntityType = org.thingsboard.server.gen.edge.EntityType.ASSET;
        }
        EntityViewUpdateMsg.Builder builder = EntityViewUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setName(entityView.getName())
                .setType(entityView.getName())
                .setRelatedName(relatedName)
                .setRelatedType(relatedType)
                .setRelatedEntityType(relatedEntityType);
        return builder.build();
    }
    
    private UplinkResponseMsg processUplinkMsg(UplinkMsg uplinkMsg) {
        return null;
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

    private EdgeConfiguration constructEdgeConfigProto(Edge edge) throws JsonProcessingException {
        return EdgeConfiguration.newBuilder()
                .setTenantIdMSB(edge.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(edge.getTenantId().getId().getLeastSignificantBits())
                .setName(edge.getName())
                .setRoutingKey(edge.getRoutingKey())
                .setType(edge.getType().toString())
                .build();
    }
}
