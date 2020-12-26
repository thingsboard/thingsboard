/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
public interface AttributesDao {

    ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String attributeType, String attributeKey);

    ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String attributeType, Collection<String> attributeKey);

    ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, String attributeType);

    ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, String attributeType, AttributeKvEntry attribute);

    ListenableFuture<List<Void>> removeAll(TenantId tenantId, EntityId entityId, String attributeType, List<String> keys);

    List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId);

    List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds);
}
