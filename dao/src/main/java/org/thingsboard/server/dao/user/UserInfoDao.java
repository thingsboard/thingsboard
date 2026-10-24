// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.user;

import org.thingsboard.server.common.data.UserInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface UserInfoDao extends Dao<UserInfo> {

    PageData<UserInfo> findUsersByTenantId(UUID tenantId, PageLink pageLink);

    PageData<UserInfo> findTenantUsersByTenantId(UUID tenantId, PageLink pageLink);

    PageData<UserInfo> findUsersByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<UserInfo> findUsersByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink);

}
