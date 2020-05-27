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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface EdgeDao.
 *
 */
public interface EdgeDao extends Dao<Edge> {

    /**
     * Save or update edge object
     *
     * @param edge the edge object
     * @return saved edge object
     */
    Edge save(TenantId tenantId, Edge edge);

    /**
     * Find edges by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of edge objects
     */
    List<Edge> findEdgesByTenantId(UUID tenantId, TextPageLink pageLink);

    /**
     * Find edges by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of edge objects
     */
    List<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink);

    /**
     * Find edges by tenantId and edges Ids.
     *
     * @param tenantId the tenantId
     * @param edgeIds the edge Ids
     * @return the list of edge objects
     */
    ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> edgeIds);

    /**
     * Find edges by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of edge objects
     */
    List<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink);

    /**
     * Find edges by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of edge objects
     */
    List<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink);


    /**
     * Find edges by tenantId, customerId and edges Ids.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param edgeIds the edge Ids
     * @return the list of edge objects
     */
    ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> edgeIds);

    /**
     * Find edges by tenantId and edge name.
     *
     * @param tenantId the tenantId
     * @param name the edge name
     * @return the optional edge object
     */
    Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find tenants edge types.
     *
     * @return the list of tenant edge type objects
     */
    ListenableFuture<List<EntitySubtype>> findTenantEdgeTypesAsync(UUID tenantId);

    /**
     * Find edge by routing Key.
     *
     * @param routingKey the edge routingKey
     * @return the optional edge object
     */
    Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey);

    /**
     * Find edges by tenantId, ruleChainId and page link.
     *
     * @param tenantId the tenantId
     * @param ruleChainId the ruleChainId
     * @param pageLink the page link
     * @return the list of rule chain objects
     */
    ListenableFuture<List<Edge>> findEdgesByTenantIdAndRuleChainId(UUID tenantId, UUID ruleChainId, TimePageLink pageLink);
}
