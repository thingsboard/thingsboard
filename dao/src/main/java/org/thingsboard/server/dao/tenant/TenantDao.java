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
package org.thingsboard.server.dao.tenant;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface TenantDao extends Dao<Tenant> {

    TenantInfo findTenantInfoById(TenantId tenantId, UUID id);

    Tenant save(TenantId tenantId, Tenant tenant);

    PageData<Tenant> findTenants(TenantId tenantId, PageLink pageLink);

    PageData<TenantInfo> findTenantInfos(TenantId tenantId, PageLink pageLink);

    PageData<TenantId> findTenantsIds(PageLink pageLink);

    List<TenantId> findTenantIdsByTenantProfileId(TenantProfileId tenantProfileId);

    Tenant findTenantByName(TenantId tenantId, String name);

    ListenableFuture<List<Tenant>> findTenantsByIdsAsync(UUID tenantId, List<UUID> tenantIds);

}
