// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

public interface EdgeService extends EntityDaoService {

    Edge findEdgeById(TenantId tenantId, EdgeId edgeId);

    EdgeInfo findEdgeInfoById(TenantId tenantId, EdgeId edgeId);

    ListenableFuture<Edge> findEdgeByIdAsync(TenantId tenantId, EdgeId edgeId);

    Edge findEdgeByTenantIdAndName(TenantId tenantId, String name);

    ListenableFuture<Edge> findEdgeByTenantIdAndNameAsync(TenantId tenantId, String name);

    Optional<Edge> findEdgeByRoutingKey(TenantId tenantId, String routingKey);

    PageData<Edge> findActiveEdges(PageLink pageLink);

    Edge saveEdge(Edge edge);

    Edge assignEdgeToCustomer(TenantId tenantId, EdgeId edgeId, CustomerId customerId);

    Edge unassignEdgeFromCustomer(TenantId tenantId, EdgeId edgeId);

    void deleteEdge(TenantId tenantId, EdgeId edgeId);

    PageData<EdgeId> findEdgeIdsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantId(TenantId tenantId, PageLink pageLink);

    ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(TenantId tenantId, List<EdgeId> edgeIds);

    void deleteEdgesByTenantId(TenantId tenantId);

    PageData<Edge> findEdgesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<EdgeId> edgeIds);

    void unassignCustomerEdges(TenantId tenantId, CustomerId customerId);

    ListenableFuture<List<Edge>> findEdgesByQuery(TenantId tenantId, EdgeSearchQuery query);

    ListenableFuture<List<EntitySubtype>> findEdgeTypesByTenantId(TenantId tenantId);

    void assignDefaultRuleChainsToEdge(TenantId tenantId, EdgeId edgeId);

    PageData<Edge> findEdgesByTenantIdAndEntityId(TenantId tenantId, EntityId ruleChainId, PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantIdAndEntityId(TenantId tenantId, EntityId ruleChainId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantProfileId(TenantProfileId tenantProfileId, PageLink pageLink);

    List<EdgeId> findAllRelatedEdgeIds(TenantId tenantId, EntityId entityId);

    PageData<EdgeId> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);

    String findMissingToRelatedRuleChains(TenantId tenantId, EdgeId edgeId, String tbRuleChainInputNodeClassName);

    Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) throws Exception;

    ListenableFuture<Boolean> isEdgeActiveAsync(TenantId tenantId, EdgeId edgeId, String activityState);

}
