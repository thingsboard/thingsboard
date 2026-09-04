// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.user.UserService;

public class TenantAdminUsersEdgeEventFetcher extends BaseUsersEdgeEventFetcher {

    public TenantAdminUsersEdgeEventFetcher(UserService userService) {
        super(userService);
    }

    @Override
    protected PageData<User> findUsers(TenantId tenantId, PageLink pageLink) {
        return userService.findTenantAdmins(tenantId, pageLink);
    }
}