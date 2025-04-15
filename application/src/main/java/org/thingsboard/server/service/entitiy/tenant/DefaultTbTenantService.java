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
package org.thingsboard.server.service.entitiy.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.entitiy.queue.TbQueueService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbTenantService extends AbstractTbEntityService implements TbTenantService {

    private final TenantService tenantService;
    private final TbTenantProfileCache tenantProfileCache;
    private final InstallScripts installScripts;
    private final TbQueueService tbQueueService;
    private final TenantProfileService tenantProfileService;
    private final EntitiesVersionControlService versionControlService;

    @Override
    public Tenant save(Tenant tenant, User user) throws Exception {
        ActionType actionType = tenant.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = tenant.getId() != null ? tenant.getId() : TenantId.SYS_TENANT_ID;
        
        try {
            boolean created = tenant.getId() == null;
            Tenant oldTenant = !created ? tenantService.findTenantById(tenant.getId()) : null;

            Tenant savedTenant = tenantService.saveTenant(tenant, tenantId2 -> {
                installScripts.createDefaultRuleChains(tenantId2);
                installScripts.createDefaultEdgeRuleChains(tenantId2);
                if (!isTestProfile()) {
                    installScripts.createDefaultTenantDashboards(tenantId2, null);
                }
            });
            tenantProfileCache.evict(savedTenant.getId());

            TenantProfile oldTenantProfile = oldTenant != null ? tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, oldTenant.getTenantProfileId()) : null;
            TenantProfile newTenantProfile = tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, savedTenant.getTenantProfileId());
            tbQueueService.updateQueuesByTenants(Collections.singletonList(savedTenant.getTenantId()), newTenantProfile, oldTenantProfile);
            
            logEntityActionService.logEntityAction(tenantId, savedTenant.getId(), savedTenant, null, actionType, user);
            return savedTenant;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.TENANT), tenant, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(Tenant tenant, User user) throws Exception {
        TenantId tenantId = tenant.getId();
        ActionType actionType = ActionType.DELETED;
        
        try {
            tenantService.deleteTenant(tenantId);
            tenantProfileCache.evict(tenantId);
            versionControlService.deleteVersionControlSettings(tenantId).get(1, TimeUnit.MINUTES);
            
            logEntityActionService.logEntityAction(TenantId.SYS_TENANT_ID, tenantId, tenant, null, actionType, user, tenantId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(TenantId.SYS_TENANT_ID, emptyId(EntityType.TENANT), actionType, user, e, tenantId.toString());
            throw e;
        }
    }
}
