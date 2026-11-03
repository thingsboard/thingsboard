// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

    Tenant findTenantByName(String name);

    void deleteTenants();

    PageData<TenantId> findTenantsIds(PageLink pageLink);

}
