// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
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

    PageData<Edge> findActiveEdges(PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantId(UUID tenantId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantId(UUID tenantId, PageLink pageLink);

    Long countEdges();

    PageData<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> edgeIds);

    PageData<Edge> findEdgesByEntityGroupId(UUID groupId, PageLink pageLink);

    PageData<Edge> findEdgesByEntityGroupIds(List<UUID> groupIds, PageLink pageLink);

    PageData<Edge> findEdgesByEntityGroupIdsAndType(List<UUID> groupIds, String type, PageLink pageLink);

    PageData<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> edgeIds);

    Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String name);

    ListenableFuture<List<EntitySubtype>> findTenantEdgeTypesAsync(UUID tenantId);

    Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey);

    PageData<Edge> findEdgesByTenantIdAndEntityId(UUID tenantId, UUID entityId, EntityType entityType, PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantIdAndEntityId(UUID tenantId, UUID entityId, EntityType entityType, PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantIdAndEntityIds(UUID tenantId, List<UUID> entityIds, EntityType entityType, PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantIdAndEntityGroupIds(UUID tenantId, List<UUID> entityGroupIds, EntityType groupType, PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantIdAndGroupEntityId(UUID tenantId, UUID entityId, EntityType groupType, PageLink pageLink);

    PageData<Edge> findEdgesByTenantProfileId(UUID tenantProfileId, PageLink pageLink);

}
