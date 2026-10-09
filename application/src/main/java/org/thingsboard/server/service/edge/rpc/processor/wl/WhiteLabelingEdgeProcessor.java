// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.wl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class WhiteLabelingEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    protected WhiteLabelingService whiteLabelingService;

    @Autowired
    protected DomainService domainService;

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        try {
            switch (edgeEvent.getAction()) {
                case ADDED, UPDATED -> {
                    EntityId entityId = JacksonUtil.convertValue(edgeEvent.getBody(), EntityId.class);
                    if (entityId == null) {
                        return null;
                    }
                    TenantId tenantId = EntityType.TENANT.equals(entityId.getEntityType()) ? (TenantId) entityId : edgeEvent.getTenantId();
                    CustomerId customerId = EntityType.CUSTOMER.equals(entityId.getEntityType()) ? new CustomerId(entityId.getId()) : null;
                    WhiteLabeling whiteLabeling = whiteLabelingService.findByEntityId(tenantId, customerId, getWhiteLabelingType(edgeEvent.getType()));
                    if (whiteLabeling == null) {
                        return null;
                    }
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    WhiteLabelingProto whiteLabelingProto = EdgeMsgConstructorUtils.constructWhiteLabeling(msgType, whiteLabeling);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setWhiteLabelingProto(whiteLabelingProto);
                    if (WhiteLabelingType.LOGIN.equals(whiteLabeling.getType()) && whiteLabeling.getDomainId() != null) {
                        DomainInfo domainInfo = domainService.findDomainInfoById(tenantId, whiteLabeling.getDomainId());
                        OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg =
                                EdgeMsgConstructorUtils.constructOAuth2DomainUpdateMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, domainInfo);
                        builder.addOAuth2DomainUpdateMsg(oAuth2DomainUpdateMsg);
                    }
                    return builder.build();
                }
                case DELETED -> {
                    WhiteLabeling whiteLabeling = JacksonUtil.convertValue(edgeEvent.getBody(), WhiteLabeling.class);
                    WhiteLabelingProto whiteLabelingProto = EdgeMsgConstructorUtils.constructWhiteLabeling(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, whiteLabeling);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setWhiteLabelingProto(whiteLabelingProto).build();
                }
            }
        } catch (Exception e) {
            log.error("Can't process white labeling msg [{}]", edgeEvent, e);
        }
        return null;
    }

    private WhiteLabelingType getWhiteLabelingType(EdgeEventType type) {
        return switch (type) {
            case WHITE_LABELING -> WhiteLabelingType.GENERAL;
            case MAIL_TEMPLATES -> WhiteLabelingType.MAIL_TEMPLATES;
            case LOGIN_WHITE_LABELING -> WhiteLabelingType.LOGIN;
            default -> null;
        };
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(EdgeEventType.valueOf(edgeNotificationMsg.getEntityType()),
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId sourceEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        JsonNode body = actionType.equals(EdgeEventActionType.DELETED) ? JacksonUtil.toJsonNode(edgeNotificationMsg.getBody()) : JacksonUtil.valueToTree(entityId);
        switch (entityId.getEntityType()) {
            case TENANT:
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                    PageDataIterable<TenantId> tenantIds = new PageDataIterable<>(link -> edgeCtx.getTenantService().findTenantsIds(link), 1024);
                    for (TenantId tenantId1 : tenantIds) {
                        futures.addAll(processActionForAllEdgesByTenantId(tenantId1, type, actionType, null, body, sourceEdgeId, null));
                    }
                } else {
                    futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, null, body, sourceEdgeId, null);
                }
                return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
            case CUSTOMER:
                List<EdgeId> edgesByCustomerId =
                        edgeCtx.getCustomersHierarchyEdgeService().findAllEdgesInHierarchyByCustomerId(tenantId, new CustomerId(entityId.getId()));
                if (edgesByCustomerId != null) {
                    for (EdgeId edgeId : edgesByCustomerId) {
                        saveEdgeEvent(tenantId, edgeId, type, actionType, null, body);
                    }
                }
                return Futures.immediateFuture(null);
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.WHITE_LABELING;
    }

}
