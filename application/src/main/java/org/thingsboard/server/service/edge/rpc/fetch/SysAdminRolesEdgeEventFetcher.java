// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.role.RoleService;

public class SysAdminRolesEdgeEventFetcher extends BaseRolesEdgeEventFetcher {

    public SysAdminRolesEdgeEventFetcher(RoleService roleService) {
        super(roleService);
    }

    @Override
    PageData<Role> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return roleService.findRolesByTenantId(TenantId.SYS_TENANT_ID, pageLink);
    }

}
