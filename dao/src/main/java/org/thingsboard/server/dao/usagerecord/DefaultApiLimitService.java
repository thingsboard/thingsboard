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
package org.thingsboard.server.dao.usagerecord;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DefaultApiLimitService implements ApiLimitService {

    private final EntityService entityService;
    private final TbTenantProfileCache tenantProfileCache;

    @Override
    public boolean checkEntitiesLimit(TenantId tenantId, EntityType entityType) {
        long limit = getLimit(tenantId, profileConfiguration -> profileConfiguration.getEntitiesLimit(entityType));
        if (limit <= 0) {
            return true;
        }

        EntityTypeFilter filter = new EntityTypeFilter();
        filter.setEntityType(entityType);
        long currentCount = entityService.countEntitiesByQuery(tenantId, new CustomerId(EntityId.NULL_UUID), new EntityCountQuery(filter));
        return currentCount < limit;
    }

    @Override
    public long getLimit(TenantId tenantId, Function<DefaultTenantProfileConfiguration, Number> extractor) {
        if (tenantId == null || tenantId.isSysTenantId()) {
            return 0L;
        }
        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        if (tenantProfile == null) {
            throw new IllegalArgumentException("Tenant profile not found for tenant " + tenantId);
        }
        Number value = extractor.apply(tenantProfile.getDefaultProfileConfiguration());
        if (value == null) {
            return 0L;
        }
        return Math.max(0, value.longValue());
    }

}
