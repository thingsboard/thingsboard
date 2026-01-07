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
