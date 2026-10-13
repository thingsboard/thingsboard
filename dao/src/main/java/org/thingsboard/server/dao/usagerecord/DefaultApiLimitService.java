// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.usagerecord;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DefaultApiLimitService implements ApiLimitService {

    private final EntityService entityService;
    private final TbTenantProfileCache tenantProfileCache;

    private static final KeyFilter edgeTemplateExcludeFilter;

    static {
        edgeTemplateExcludeFilter = new KeyFilter();
        edgeTemplateExcludeFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "edgeTemplate"));
        edgeTemplateExcludeFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate predicate = new BooleanFilterPredicate();
        predicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        predicate.setValue(new FilterPredicateValue<>(false));
        edgeTemplateExcludeFilter.setPredicate(predicate);
    }

    @Override
    public boolean checkEntitiesLimit(TenantId tenantId, EntityType entityType) {
        long limit = getLimit(tenantId, profileConfiguration -> profileConfiguration.getEntitiesLimit(entityType));
        if (limit <= 0) {
            return true;
        }

        EntityTypeFilter filter = new EntityTypeFilter();
        filter.setEntityType(entityType);
        EntityCountQuery query;
        if (EntityType.INTEGRATION.equals(entityType) || EntityType.CONVERTER.equals(entityType)) {
            query = new EntityCountQuery(filter, List.of(edgeTemplateExcludeFilter));
        } else {
            query = new EntityCountQuery(filter);
        }
        long currentCount = entityService.countEntitiesByQuery(tenantId, new CustomerId(EntityId.NULL_UUID), MergedUserPermissions.ALL, query);
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
