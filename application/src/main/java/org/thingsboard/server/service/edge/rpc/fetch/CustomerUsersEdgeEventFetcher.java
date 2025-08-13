/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
