// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.user.UserService;

public class CustomerUsersEdgeEventFetcher extends BaseUsersEdgeEventFetcher {

    private final CustomerId customerId;

    public CustomerUsersEdgeEventFetcher(UserService userService, CustomerId customerId) {
        super(userService);
        this.customerId = customerId;
    }

    @Override
    protected PageData<User> findUsers(TenantId tenantId, PageLink pageLink) {
        return userService.findCustomerUsers(tenantId, customerId, pageLink);
    }

}
