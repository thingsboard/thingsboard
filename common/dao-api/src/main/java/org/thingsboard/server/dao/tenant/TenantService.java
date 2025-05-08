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
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.function.Consumer;

public interface TenantService extends EntityDaoService {

    Tenant findTenantById(TenantId tenantId);

    TenantInfo findTenantInfoById(TenantId tenantId);

    ListenableFuture<Tenant> findTenantByIdAsync(TenantId callerId, TenantId tenantId);

    Tenant saveTenant(Tenant tenant);

    Tenant saveTenant(Tenant tenant, Consumer<TenantId> defaultEntitiesCreator);

    boolean tenantExists(TenantId tenantId);

    void deleteTenant(TenantId tenantId);

    PageData<Tenant> findTenants(PageLink pageLink);

    PageData<TenantInfo> findTenantInfos(PageLink pageLink);

    List<TenantId> findTenantIdsByTenantProfileId(TenantProfileId tenantProfileId);

    void deleteTenants();

    PageData<TenantId> findTenantsIds(PageLink pageLink);
}
