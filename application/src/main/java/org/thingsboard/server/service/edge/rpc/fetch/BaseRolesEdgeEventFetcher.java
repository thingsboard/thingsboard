// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.role.RoleService;

@AllArgsConstructor
@Slf4j
public abstract class BaseRolesEdgeEventFetcher extends BasePageableEdgeEventFetcher<Role> {

    protected final RoleService roleService;

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Role role) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ROLE,
                EdgeEventActionType.ADDED, role.getId(), null);
    }
}
