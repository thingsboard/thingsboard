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
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Optional;

public interface EdgeService {

    Edge findEdgeById(TenantId tenantId, EdgeId edgeId);

    EdgeInfo findEdgeInfoById(TenantId tenantId, EdgeId edgeId);

    ListenableFuture<Edge> findEdgeByIdAsync(TenantId tenantId, EdgeId edgeId);

    Edge findEdgeByTenantIdAndName(TenantId tenantId, String name);

    Optional<Edge> findEdgeByRoutingKey(TenantId tenantId, String routingKey);

    Edge saveEdge(Edge edge);

    Edge assignEdgeToCustomer(TenantId tenantId, EdgeId edgeId, CustomerId customerId);

    Edge unassignEdgeFromCustomer(TenantId tenantId, EdgeId edgeId);

    void deleteEdge(TenantId tenantId, EdgeId edgeId);

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

    ListenableFuture<List<Edge>> findEdgesByTenantIdAndRuleChainId(TenantId tenantId, RuleChainId ruleChainId);

    ListenableFuture<List<Edge>> findEdgesByTenantIdAndDashboardId(TenantId tenantId, DashboardId dashboardId);

    ListenableFuture<List<EdgeId>> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId);

    Object checkInstance(Object request);

    Object activateInstance(String licenseSecret, String releaseDate);
}
