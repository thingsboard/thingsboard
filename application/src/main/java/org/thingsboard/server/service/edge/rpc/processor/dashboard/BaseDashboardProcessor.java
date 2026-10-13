// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Slf4j
public abstract class BaseDashboardProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<Dashboard> dashboardValidator;

    protected boolean saveOrUpdateDashboard(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg) throws ThingsboardException {
        boolean created = false;
        Dashboard dashboard = JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
        if (dashboard == null) {
            throw new RuntimeException("[{" + tenantId + "}] dashboardUpdateMsg {" + dashboardUpdateMsg + "} cannot be converted to dashboard");
        }
        Dashboard dashboardById = edgeCtx.getDashboardService().findDashboardById(tenantId, dashboardId);
        if (dashboardById == null) {
            created = true;
            dashboard.setId(null);
        } else {
            dashboard.setId(dashboardId);
            changeOwnerIfRequired(tenantId, null, dashboardId);
        }

        if (isSaveRequired(dashboardById, dashboard)) {
            dashboardValidator.validate(dashboard, Dashboard::getTenantId);
            if (created) {
                dashboard.setId(dashboardId);
            }
            Dashboard savedDashboard = edgeCtx.getDashboardService().saveDashboard(dashboard, false);
            if (created) {
                edgeCtx.getEntityGroupService().addEntityToEntityGroupAll(savedDashboard.getTenantId(), savedDashboard.getOwnerId(), savedDashboard.getId());
            }
        }
        safeAddToEntityGroup(tenantId, dashboardUpdateMsg, dashboardId);
        return created;
    }

    private void safeAddToEntityGroup(TenantId tenantId, DashboardUpdateMsg dashboardUpdateMsg, DashboardId dashboardId) {
        if (dashboardUpdateMsg.hasEntityGroupIdMSB() && dashboardUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(dashboardUpdateMsg.getEntityGroupIdMSB(),
                    dashboardUpdateMsg.getEntityGroupIdLSB());
            safeAddEntityToGroup(tenantId, new EntityGroupId(entityGroupUUID), dashboardId);
        }
    }

    protected void deleteDashboard(TenantId tenantId, DashboardId dashboardId) {
        deleteDashboard(tenantId, null, dashboardId);
    }

    protected void deleteDashboard(TenantId tenantId, Edge edge, DashboardId dashboardId) {
        Dashboard dashboardById = edgeCtx.getDashboardService().findDashboardById(tenantId, dashboardId);
        if (dashboardById != null) {
            edgeCtx.getDashboardService().deleteDashboard(tenantId, dashboardId);
            pushEntityEventToRuleEngine(tenantId, edge, dashboardById, TbMsgType.ENTITY_DELETED);
        }
    }

}
