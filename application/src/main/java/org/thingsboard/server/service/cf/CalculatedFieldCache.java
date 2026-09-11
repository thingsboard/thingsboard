// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf;

import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
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

    Stream<CalculatedFieldCtx> getCalculatedFieldCtxsByType(TenantId tenantId, CalculatedFieldType cfType);

    boolean hasCalculatedFields(TenantId tenantId, EntityId entityId, Predicate<CalculatedFieldCtx> filter);

    void addCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    void updateCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    void evict(CalculatedFieldId calculatedFieldId);

    void handleTenantProfileUpdate(TenantProfileId tenantProfileId);

    EntityId getProfileId(TenantId tenantId, EntityId entityId);

    Set<EntityId> getDynamicEntities(TenantId tenantId, EntityId entityId);

    void updateOwnerEntity(TenantId tenantId, EntityId entityId);

    void addOwnerEntity(TenantId tenantId, EntityId entityId);

    void evictOwnerEntity(EntityId entityId);

    void evictOwner(EntityId owner);

}
