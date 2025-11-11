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
package org.thingsboard.server.service.cf;

import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface CalculatedFieldCache {

    CalculatedField getCalculatedField(CalculatedFieldId calculatedFieldId);

    List<CalculatedField> getCalculatedFieldsByEntityId(EntityId entityId);

    List<CalculatedFieldLink> getCalculatedFieldLinksByEntityId(EntityId entityId);

    CalculatedFieldCtx getCalculatedFieldCtx(CalculatedFieldId calculatedFieldId);

    List<CalculatedFieldCtx> getCalculatedFieldCtxsByEntityId(EntityId entityId);

    Stream<CalculatedFieldCtx> getCalculatedFieldCtxsByType(CalculatedFieldType cfType);

    boolean hasCalculatedFields(TenantId tenantId, EntityId entityId, Predicate<CalculatedFieldCtx> filter);

    void addCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    void updateCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    void evict(CalculatedFieldId calculatedFieldId);

    EntityId getProfileId(TenantId tenantId, EntityId entityId);

    Set<EntityId> getDynamicEntities(TenantId tenantId, EntityId entityId);

    void updateOwnerEntity(TenantId tenantId, EntityId entityId);

    void addOwnerEntity(TenantId tenantId, EntityId entityId);

    void evictEntity(EntityId entityId);

    void evictOwner(EntityId owner);

}
