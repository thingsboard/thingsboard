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
package org.thingsboard.server.service.edge.rpc.processor.dashboard;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@TbCoreComponent
public class DashboardEdgeProcessor extends BaseDashboardProcessor implements DashboardProcessor {

    @Override
    public ListenableFuture<Void> processDashboardMsgFromEdge(TenantId tenantId, Edge edge, DashboardUpdateMsg dashboardUpdateMsg) {
        log.trace("[{}] executing processDashboardMsgFromEdge [{}] from edge [{}]", tenantId, dashboardUpdateMsg, edge.getId());
        DashboardId dashboardId = new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (dashboardUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    deleteDashboard(tenantId, edge, dashboardId);
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(dashboardUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
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

    private void saveOrUpdateDashboard(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg, Edge edge) {
        boolean created = super.saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg, edge.getCustomerId());
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), dashboardId);
            pushDashboardCreatedEventToRuleEngine(tenantId, edge, dashboardId);
            edgeCtx.getDashboardService().assignDashboardToEdge(tenantId, dashboardId, edge.getId());
        }
    }

    private void pushDashboardCreatedEventToRuleEngine(TenantId tenantId, Edge edge, DashboardId dashboardId) {
        Dashboard dashboard = edgeCtx.getDashboardService().findDashboardById(tenantId, dashboardId);
        pushEntityEventToRuleEngine(tenantId, edge, dashboard, TbMsgType.ENTITY_CREATED);
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        DashboardId dashboardId = new DashboardId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED, ASSIGNED_TO_EDGE, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER -> {
                Dashboard dashboard = edgeCtx.getDashboardService().findDashboardById(edgeEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DashboardUpdateMsg dashboardUpdateMsg = EdgeMsgConstructorUtils.constructDashboardUpdatedMsg(msgType, dashboard);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDashboardUpdateMsg(dashboardUpdateMsg)
                            .build();
                }
            }
            case DELETED, UNASSIGNED_FROM_EDGE -> {
                DashboardUpdateMsg dashboardUpdateMsg = EdgeMsgConstructorUtils.constructDashboardDeleteMsg(dashboardId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDashboardUpdateMsg(dashboardUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    protected Set<ShortCustomerInfo> filterNonExistingCustomers(TenantId tenantId, CustomerId edgeCustomerId, Set<ShortCustomerInfo> currentAssignedCustomers, Set<ShortCustomerInfo> newAssignedCustomers) {
        boolean edgeCustomerPresentInNewAssignments = newAssignedCustomers.stream()
                .map(ShortCustomerInfo::getCustomerId)
                .anyMatch(edgeCustomerId::equals);

        if (edgeCustomerPresentInNewAssignments) {
            Set<ShortCustomerInfo> result = new HashSet<>(newAssignedCustomers);
            result.addAll(currentAssignedCustomers);
            return result;
        } else {
            return currentAssignedCustomers.stream()
                    .filter(info -> !edgeCustomerId.equals(info.getCustomerId()))
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.DASHBOARD;
    }
}
