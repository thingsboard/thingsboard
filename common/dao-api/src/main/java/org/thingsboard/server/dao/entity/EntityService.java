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
package org.thingsboard.server.dao.entity;

import com.google.common.util.concurrent.FluentFuture;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NameLabelAndCustomerDetails;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface EntityService {

    Optional<String> fetchEntityName(TenantId tenantId, EntityId entityId);

    Optional<String> fetchEntityLabel(TenantId tenantId, EntityId entityId);

    Optional<CustomerId> fetchEntityCustomerId(TenantId tenantId, EntityId entityId);

    FluentFuture<Optional<CustomerId>> fetchEntityCustomerIdAsync(TenantId tenantId, EntityId entityId);

    Optional<HasId<?>> fetchEntity(TenantId tenantId, EntityId entityId);

    Map<EntityId, EntityInfo> fetchEntityInfos(TenantId tenantId, CustomerId customerId, Set<EntityId> entityIds);

    Optional<NameLabelAndCustomerDetails> fetchNameLabelAndCustomerDetails(TenantId tenantId, EntityId entityId);

    long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query);

    PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query);

}
