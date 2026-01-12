/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.tenant.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.entitiy.queue.TbQueueService;

import java.util.List;

@Slf4j
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbTenantProfileService extends AbstractTbEntityService implements TbTenantProfileService {
    private final TbQueueService tbQueueService;
    private final TenantProfileService tenantProfileService;
    private final TenantService tenantService;
    private final TbTenantProfileCache tenantProfileCache;

    @Override
    public TenantProfile save(TenantId tenantId, TenantProfile tenantProfile, TenantProfile oldTenantProfile) throws ThingsboardException {
        TenantProfile savedTenantProfile = checkNotNull(tenantProfileService.saveTenantProfile(tenantId, tenantProfile));
        tenantProfileCache.put(savedTenantProfile);

        List<TenantId> tenantIds = tenantService.findTenantIdsByTenantProfileId(savedTenantProfile.getId());
        tbQueueService.updateQueuesByTenants(tenantIds, savedTenantProfile, oldTenantProfile);

        return savedTenantProfile;
    }

    @Override
    public void delete(TenantId tenantId, TenantProfile tenantProfile) throws ThingsboardException {
        tenantProfileService.deleteTenantProfile(tenantId, tenantProfile.getId());
    }
}
