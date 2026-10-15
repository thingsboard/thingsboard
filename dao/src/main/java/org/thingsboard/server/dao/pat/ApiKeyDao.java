// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

    PageData<ApiKey> findByTenantId(TenantId tenantId, PageLink pageLink);

    List<ApiKey> findByTenantIdAndUserId(TenantId tenantId, UserId userId);

    Set<String> deleteByTenantId(TenantId tenantId);

    Set<String> deleteByUserId(TenantId tenantId, UserId userId);

    int deleteAllByExpirationTimeBefore(long ts);

}
