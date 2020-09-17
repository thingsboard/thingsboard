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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.mapping.Result;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.EntitySubtypeEntity;
import org.thingsboard.server.dao.model.nosql.EdgeEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_BY_CUSTOMER_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_BY_TENANT_AND_NAME_VIEW_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_BY_TENANT_AND_ROUTING_KEY_VIEW_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_ROUTING_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_SUBTYPE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_SUBTYPE_ENTITY_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_SUBTYPE_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraEdgeDao extends CassandraAbstractSearchTextDao<EdgeEntity, Edge> implements EdgeDao {

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<EdgeEntity> getColumnFamilyClass() {
        return EdgeEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return EDGE_COLUMN_FAMILY_NAME;
    }

    @Override
    public Edge save(TenantId tenantId, Edge domain) {
        Edge savedEdge = super.save(tenantId, domain);
        EntitySubtype entitySubtype = new EntitySubtype(savedEdge.getTenantId(), EntityType.EDGE, savedEdge.getType());
        EntitySubtypeEntity entitySubtypeEntity = new EntitySubtypeEntity(entitySubtype);
        Statement saveStatement = cluster.getMapper(EntitySubtypeEntity.class).saveQuery(entitySubtypeEntity);
        executeWrite(tenantId, saveStatement);
        return savedEdge;
    }

    @Override
    public List<Edge> findEdgesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find edge by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<EdgeEntity> edgeEntities = findPageWithTextSearch(new TenantId(tenantId), EDGE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(EDGE_TENANT_ID_PROPERTY, tenantId)), pageLink);

        log.trace("Found edges [{}] by tenantId [{}] and pageLink [{}]", edgeEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(edgeEntities);
    }

    @Override
    public List<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        log.debug("Try to find edges by tenantId [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        List<EdgeEntity> edgeEntities = findPageWithTextSearch(new TenantId(tenantId), EDGE_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(EDGE_TYPE_PROPERTY, type),
                        eq(EDGE_TENANT_ID_PROPERTY, tenantId)), pageLink);
        log.trace("Found edges [{}] by tenantId [{}], type [{}] and pageLink [{}]", edgeEntities, tenantId, type, pageLink);
        return DaoUtil.convertDataList(edgeEntities);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> edgeIds) {
        log.debug("Try to find edges by tenantId [{}] and edge Ids [{}]", tenantId, edgeIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(EDGE_TENANT_ID_PROPERTY, tenantId));
        query.and(in(ID_PROPERTY, edgeIds));
        return findListByStatementAsync(new TenantId(tenantId), query);
    }

    @Override
    public List<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        log.debug("Try to find edges by tenantId [{}], customerId[{}] and pageLink [{}]", tenantId, customerId, pageLink);
        List<EdgeEntity> edgeEntities = findPageWithTextSearch(new TenantId(tenantId), EDGE_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(EDGE_CUSTOMER_ID_PROPERTY, customerId),
                        eq(EDGE_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found edges [{}] by tenantId [{}], customerId [{}] and pageLink [{}]", edgeEntities, tenantId, customerId, pageLink);
        return DaoUtil.convertDataList(edgeEntities);
    }

    @Override
    public List<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        log.debug("Try to find edges by tenantId [{}], customerId [{}], type [{}] and pageLink [{}]", tenantId, customerId, type, pageLink);
        List<EdgeEntity> edgeEntities = findPageWithTextSearch(new TenantId(tenantId), EDGE_BY_CUSTOMER_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(EDGE_TYPE_PROPERTY, type),
                        eq(EDGE_CUSTOMER_ID_PROPERTY, customerId),
                        eq(EDGE_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found edges [{}] by tenantId [{}], customerId [{}], type [{}] and pageLink [{}]", edgeEntities, tenantId, customerId, type, pageLink);
        return DaoUtil.convertDataList(edgeEntities);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> edgeIds) {
        log.debug("Try to find edges by tenantId [{}], customerId [{}] and edges Ids [{}]", tenantId, customerId, edgeIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(EDGE_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(EDGE_CUSTOMER_ID_PROPERTY, customerId));
        query.and(in(ID_PROPERTY, edgeIds));
        return findListByStatementAsync(new TenantId(tenantId), query);
    }

    @Override
    public Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String edgeName) {
        Select select = select().from(EDGE_BY_TENANT_AND_NAME_VIEW_NAME);
        Select.Where query = select.where();
        query.and(eq(EDGE_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(EDGE_NAME_PROPERTY, edgeName));
        return Optional.ofNullable(DaoUtil.getData(findOneByStatement(new TenantId(tenantId), query)));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantEdgeTypesAsync(UUID tenantId) {
        Select select = select().from(ENTITY_SUBTYPE_COLUMN_FAMILY_NAME);
        Select.Where query = select.where();
        query.and(eq(ENTITY_SUBTYPE_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(ENTITY_SUBTYPE_ENTITY_TYPE_PROPERTY, EntityType.EDGE));
        query.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
        ResultSetFuture resultSetFuture = executeAsyncRead(new TenantId(tenantId), query);
        return Futures.transform(resultSetFuture, new Function<ResultSet, List<EntitySubtype>>() {
            @Nullable
            @Override
            public List<EntitySubtype> apply(@Nullable ResultSet resultSet) {
                Result<EntitySubtypeEntity> result = cluster.getMapper(EntitySubtypeEntity.class).map(resultSet);
                if (result != null) {
                    List<EntitySubtype> entitySubtypes = new ArrayList<>();
                    result.all().forEach((entitySubtypeEntity) ->
                            entitySubtypes.add(entitySubtypeEntity.toEntitySubtype())
                    );
                    return entitySubtypes;
                } else {
                    return Collections.emptyList();
                }
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey) {
        Select select = select().from(EDGE_BY_TENANT_AND_ROUTING_KEY_VIEW_NAME);
        Select.Where query = select.where();
        query.and(eq(EDGE_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(EDGE_ROUTING_KEY_PROPERTY, routingKey));
        return Optional.ofNullable(DaoUtil.getData(findOneByStatement(new TenantId(tenantId), query)));
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndRuleChainId(UUID tenantId, UUID ruleChainId) {
        log.debug("Try to find edges by tenantId [{}], ruleChainId [{}]", tenantId, ruleChainId);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByToAndType(new TenantId(tenantId), new RuleChainId(ruleChainId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        return transformFromRelationToEdge(tenantId, relations);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndDashboardId(UUID tenantId, UUID dashboardId) {
        log.debug("Try to find edges by tenantId [{}], dashboardId [{}]", tenantId, dashboardId);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByToAndType(new TenantId(tenantId), new DashboardId(dashboardId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        return transformFromRelationToEdge(tenantId, relations);
    }

    private ListenableFuture<List<Edge>> transformFromRelationToEdge(UUID tenantId, ListenableFuture<List<EntityRelation>> relations) {
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<Edge>> edgeFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                edgeFutures.add(findByIdAsync(new TenantId(tenantId), relation.getFrom().getId()));
            }
            return Futures.successfulAsList(edgeFutures);
        }, MoreExecutors.directExecutor());
    }
}
