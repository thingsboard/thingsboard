// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.secret;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.Dao;

import java.util.List;

public interface SecretInfoDao extends Dao<SecretInfo> {

    PageData<SecretInfo> findByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    SecretInfo findByName(TenantId tenantId, CustomerId customerId, String name);

    List<String> findAllNamesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

}
