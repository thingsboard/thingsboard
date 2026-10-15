// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.pat;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Set;

public interface ApiKeyDao extends Dao<ApiKey> {

    ApiKey findByValue(String value);

    ApiKey findInternalByDescription(TenantId tenantId, String description);

    Set<String> deleteByTenantId(TenantId tenantId);

    Set<String> deleteByUserId(TenantId tenantId, UserId userId);

    int deleteAllByExpirationTimeBefore(long ts);

    List<ApiKey> findByTenantIdAndUserId(TenantId tenantId, UserId userId);

    PageData<ApiKey> findByTenantId(TenantId tenantId, PageLink pageLink);

}
