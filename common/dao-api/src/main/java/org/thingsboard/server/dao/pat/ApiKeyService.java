// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.pat;

import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface ApiKeyService extends EntityDaoService {

    ApiKey saveApiKey(TenantId tenantId, ApiKeyInfo apiKey);

    ApiKey saveApiKey(TenantId tenantId, ApiKeyInfo apiKeyInfo, String value, boolean doValidate);

    void deleteApiKey(TenantId tenantId, ApiKey apiKey, boolean force);

    void deleteByUserId(TenantId tenantId, UserId userId);

    ApiKey findApiKeyByValue(String value);

    ApiKey findApiKeyById(TenantId tenantId, ApiKeyId apiKeyId);

    PageData<ApiKeyInfo> findApiKeysByUserId(TenantId tenantId, UserId userId, PageLink pageLink);

    List<ApiKey> findApiKeysByUserId(TenantId tenantId, UserId userId);

    PageData<ApiKey> findApiKeysByTenantId(TenantId tenantId, PageLink pageLink);

}
