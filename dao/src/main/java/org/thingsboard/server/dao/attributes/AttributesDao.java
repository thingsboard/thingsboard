// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.tuple.Pair;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
public interface AttributesDao {

    Optional<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, AttributeScope attributeScope, String attributeKey);

    List<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, AttributeScope attributeScope, Collection<String> attributeKey);

    List<AttributeKvEntry> findAll(TenantId tenantId, EntityId entityId, AttributeScope attributeScope);

    ListenableFuture<Long> save(TenantId tenantId, EntityId entityId, AttributeScope attributeScope, AttributeKvEntry attribute);

    List<ListenableFuture<String>> removeAll(TenantId tenantId, EntityId entityId, AttributeScope attributeScope, List<String> keys);

    List<ListenableFuture<TbPair<String, Long>>> removeAllWithVersions(TenantId tenantId, EntityId entityId, AttributeScope attributeScope, List<String> keys);

    List<AttributeKvEntity> findNextBatch(UUID entityId, int attributeType, int attributeKey, int batchSize);

    List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId);

    List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds);

    List<String> findAllKeysByEntityIdsAndAttributeType(TenantId tenantId, List<EntityId> entityIds, String attributeType);

    List<Pair<AttributeScope, String>> removeAllByEntityId(TenantId tenantId, EntityId entityId);

}
