// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.role.RoleService;

@Slf4j
public class CustomerRolesEdgeEventFetcher extends BaseRolesEdgeEventFetcher {

    private final CustomerId customerId;

    public CustomerRolesEdgeEventFetcher(RoleService roleService, CustomerId customerId) {
        super(roleService);
        this.customerId = customerId;
    }

    @Override
    PageData<Role> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return roleService.findRolesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
    }
}
