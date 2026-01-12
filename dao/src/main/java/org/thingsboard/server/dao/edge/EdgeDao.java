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
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface EdgeDao.
 *
 */
public interface EdgeDao extends Dao<Edge>, TenantEntityDao<Edge> {

    Edge save(TenantId tenantId, Edge edge);

    EdgeInfo findEdgeInfoById(TenantId tenantId, UUID edgeId);

    PageData<Edge> findActiveEdges(PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantId(UUID tenantId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantId(UUID tenantId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> edgeIds);

    PageData<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> edgeIds);

    Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String name);

    ListenableFuture<List<EntitySubtype>> findTenantEdgeTypesAsync(UUID tenantId);

    Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantId(UUID tenantId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantIdAndEntityId(UUID tenantId, UUID entityId, EntityType entityType, PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantIdAndEntityId(UUID tenantId, UUID entityId, EntityType entityType, PageLink pageLink);

    PageData<Edge> findEdgesByTenantProfileId(UUID tenantProfileId, PageLink pageLink);

}
