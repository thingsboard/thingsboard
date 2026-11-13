// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.customer;

import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface CustomerInfoDao extends Dao<CustomerInfo> {

    PageData<CustomerInfo> findCustomersByTenantId(UUID tenantId, PageLink pageLink);

    PageData<CustomerInfo> findTenantCustomersByTenantId(UUID tenantId, PageLink pageLink);

    PageData<CustomerInfo> findCustomersByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<CustomerInfo> findCustomersByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink);

}
