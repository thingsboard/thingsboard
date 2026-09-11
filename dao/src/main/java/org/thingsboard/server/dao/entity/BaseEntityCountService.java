// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

@Service
public class BaseEntityCountService extends AbstractCachedEntityService<EntityCountCacheKey, Long, EntityCountCacheEvictEvent> implements EntityCountService {

    @Lazy
    @Autowired
    private EntityServiceRegistry entityServiceRegistry;

    @Override
    public long countByTenantIdAndEntityType(TenantId tenantId, EntityType entityType) {
        return cache.getAndPutInTransaction(new EntityCountCacheKey(tenantId, entityType),
                () -> entityServiceRegistry.getServiceByEntityType(entityType).countByTenantId(tenantId), false);
    }

    @Override
    public void publishCountEntityEvictEvent(TenantId tenantId, EntityType entityType) {
        publishEvictEvent(new EntityCountCacheEvictEvent(tenantId, entityType));
    }

    @TransactionalEventListener(classes = EntityCountCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(EntityCountCacheEvictEvent event) {
        cache.evict(new EntityCountCacheKey(event.getTenantId(), event.getEntityType()));
    }
}
