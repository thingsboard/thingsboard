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
package org.thingsboard.server.service.cf.cache;

import org.springframework.context.ApplicationListener;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.Collection;

public interface CalculatedFieldEntityProfileCache extends ApplicationListener<PartitionChangeEvent> {

    void add(TenantId tenantId, EntityId profileId, EntityId entityId);

    void update(TenantId tenantId, EntityId oldProfileId, EntityId newProfileId, EntityId entityId);

    void evict(TenantId tenantId, EntityId entityId);

    Collection<EntityId> getMyEntityIdsByProfileId(TenantId tenantId, EntityId profileId);

    int getEntityIdPartition(TenantId tenantId, EntityId entityId);
}
