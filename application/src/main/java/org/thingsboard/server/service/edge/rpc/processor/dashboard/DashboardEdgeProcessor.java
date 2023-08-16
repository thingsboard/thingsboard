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
package org.thingsboard.server.service.edge.rpc.processor.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class DashboardEdgeProcessor extends BaseDashboardProcessor {

    public ListenableFuture<Void> processDashboardMsgFromEdge(TenantId tenantId, Edge edge, DashboardUpdateMsg dashboardUpdateMsg) {
        log.trace("[{}] executing processDashboardMsgFromEdge [{}] from edge [{}]", tenantId, dashboardUpdateMsg, edge.getName());
        DashboardId dashboardId = new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getSync().set(true);

            switch (dashboardUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    Dashboard dashboardToDelete = dashboardService.findDashboardById(tenantId, dashboardId);
                    if (dashboardToDelete != null) {
                        dashboardService.unassignDashboardFromEdge(tenantId, dashboardId, edge.getId());
                    }
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
            edgeSynchronizationManager.getSync().remove();
        }
    }

    private void saveOrUpdateDashboard(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg, Edge edge) {
        CustomerId customerId = safeGetCustomerId(dashboardUpdateMsg.getCustomerIdMSB(), dashboardUpdateMsg.getCustomerIdLSB());
        boolean created = super.saveOrUpdateDashboard(tenantId, dashboardId, dashboardUpdateMsg, customerId);
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), dashboardId);
            pushDashboardCreatedEventToRuleEngine(tenantId, edge, dashboardId);
            dashboardService.assignDashboardToEdge(tenantId, dashboardId, edge.getId());
        }
    }

    private void pushDashboardCreatedEventToRuleEngine(TenantId tenantId, Edge edge, DashboardId dashboardId) {
        try {
            Dashboard dashboard = dashboardService.findDashboardById(tenantId, dashboardId);
            ObjectNode entityNode = JacksonUtil.OBJECT_MAPPER.valueToTree(dashboard);
            TbMsg tbMsg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, dashboardId, null,
                    getActionTbMsgMetaData(edge, null), TbMsgDataType.JSON, JacksonUtil.OBJECT_MAPPER.writeValueAsString(entityNode));
            tbClusterService.pushMsgToRuleEngine(tenantId, dashboardId, tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.debug("Successfully send ENTITY_CREATED EVENT to rule engine [{}]", dashboard);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("Failed to send ENTITY_CREATED EVENT to rule engine [{}]", dashboard, t);
                }
            });
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push dashboard action to rule engine: {}", dashboardId, DataConstants.ENTITY_CREATED, e);
        }
    }

    public DownlinkMsg convertDashboardEventToDownlink(EdgeEvent edgeEvent) {
        DashboardId dashboardId = new DashboardId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Dashboard dashboard = dashboardService.findDashboardById(edgeEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DashboardUpdateMsg dashboardUpdateMsg =
                            dashboardMsgConstructor.constructDashboardUpdatedMsg(msgType, dashboard);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDashboardUpdateMsg(dashboardUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                DashboardUpdateMsg dashboardUpdateMsg =
                        dashboardMsgConstructor.constructDashboardDeleteMsg(dashboardId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDashboardUpdateMsg(dashboardUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }
}
