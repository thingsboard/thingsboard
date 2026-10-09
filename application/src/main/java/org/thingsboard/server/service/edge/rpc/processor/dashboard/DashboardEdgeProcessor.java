// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.dashboard;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.edge.rpc.EdgeVersionComparator;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class DashboardEdgeProcessor extends BaseDashboardProcessor implements DashboardProcessor {

    @Override
    public ListenableFuture<Void> processDashboardMsgFromEdge(TenantId tenantId, Edge edge, DashboardUpdateMsg dashboardUpdateMsg, EdgeVersion edgeVersion) {
        log.trace("[{}] executing processDashboardMsgFromEdge [{}] from edge [{}]", tenantId, dashboardUpdateMsg, edge.getId());
        DashboardId dashboardId = new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (dashboardUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    if (dashboardUpdateMsg.hasEntityGroupIdMSB() && dashboardUpdateMsg.hasEntityGroupIdLSB()) {
                        EntityGroupId entityGroupId = new EntityGroupId(
                                new UUID(dashboardUpdateMsg.getEntityGroupIdMSB(), dashboardUpdateMsg.getEntityGroupIdLSB()));
                        edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, dashboardId);
                    } else if (EdgeVersionComparator.INSTANCE.compare(edgeVersion, EdgeVersion.V_4_3_0) >= 0) {
                        deleteDashboard(tenantId, edge, dashboardId);
                    } else {
                        removeDashboardFromEdgeAllDashboardGroup(tenantId, edge, dashboardId);
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(dashboardUpdateMsg.getMsgType());
            };
        } catch (DataValidationException | ThingsboardException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed dashboard violated {}", tenantId, dashboardUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    private void saveOrUpdateDashboard(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg, Edge edge) throws ThingsboardException {
        boolean created = super.saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg);
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), dashboardId);
            pushDashboardCreatedEventToRuleEngine(tenantId, edge, dashboardId);
        }
        addDashboardToEdgeAllDashboardGroup(tenantId, edge, dashboardId);
    }

    private void pushDashboardCreatedEventToRuleEngine(TenantId tenantId, Edge edge, DashboardId dashboardId) {
        Dashboard dashboard = edgeCtx.getDashboardService().findDashboardById(tenantId, dashboardId);
        pushEntityEventToRuleEngine(tenantId, edge, dashboard, TbMsgType.ENTITY_CREATED);
    }

    private void addDashboardToEdgeAllDashboardGroup(TenantId tenantId, Edge edge, DashboardId dashboardId) {
        Dashboard dashboard = edgeCtx.getDashboardService().findDashboardById(tenantId, dashboardId);
        addEntityToEdgeAllGroup(tenantId, edge, dashboard);
    }

    private void removeDashboardFromEdgeAllDashboardGroup(TenantId tenantId, Edge edge, DashboardId dashboardId) {
        Dashboard dashboardToDelete = edgeCtx.getDashboardService().findDashboardById(tenantId, dashboardId);
        removeEntityFromEdgeAllGroup(tenantId, edge, dashboardToDelete);
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        DashboardId dashboardId = new DashboardId(edgeEvent.getEntityId());
        EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
        switch (edgeEvent.getAction()) {
            case ADDED, ADDED_TO_ENTITY_GROUP, UPDATED, ASSIGNED_TO_EDGE -> {
                Dashboard dashboard = edgeCtx.getDashboardService().findDashboardById(edgeEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DashboardUpdateMsg dashboardUpdateMsg = EdgeMsgConstructorUtils.constructDashboardUpdatedMsg(msgType, dashboard, entityGroupId);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDashboardUpdateMsg(dashboardUpdateMsg)
                            .build();
                }
            }
            case DELETED, REMOVED_FROM_ENTITY_GROUP, UNASSIGNED_FROM_EDGE, CHANGE_OWNER -> {
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDashboardUpdateMsg(EdgeMsgConstructorUtils.constructDashboardDeleteMsg(dashboardId, entityGroupId))
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.DASHBOARD;
    }

}
