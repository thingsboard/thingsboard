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
package org.thingsboard.server.dao.sql.edge;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.model.sql.EdgeEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

@Component
@SqlDao
@Slf4j
public class JpaEdgeDao extends JpaAbstractSearchTextDao<EdgeEntity, Edge> implements EdgeDao {

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<EdgeEntity> getEntityClass() {
        return EdgeEntity.class;
    }

    @Override
    protected CrudRepository<EdgeEntity, String> getCrudRepository() {
        return edgeRepository;
    }

    @Override
    public List<Edge> findEdgesByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                edgeRepository.findByTenantId(
                        fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        PageRequest.of(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> edgeIds) {
        return service.submit(() -> DaoUtil.convertDataList(edgeRepository.findEdgesByTenantIdAndIdIn(UUIDConverter.fromTimeUUID(tenantId), fromTimeUUIDs(edgeIds))));
    }

    @Override
    public List<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                edgeRepository.findByTenantIdAndCustomerId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        PageRequest.of(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> edgeIds) {
        return service.submit(() -> DaoUtil.convertDataList(
                edgeRepository.findEdgesByTenantIdAndCustomerIdAndIdIn(fromTimeUUID(tenantId), fromTimeUUID(customerId), fromTimeUUIDs(edgeIds))));
    }

    @Override
    public Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String name) {
        Edge edge = DaoUtil.getData(edgeRepository.findByTenantIdAndName(fromTimeUUID(tenantId), name));
        return Optional.ofNullable(edge);
    }

    @Override
    public List<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                edgeRepository.findByTenantIdAndType(
                        fromTimeUUID(tenantId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        PageRequest.of(0, pageLink.getLimit())));
    }

    @Override
    public List<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                edgeRepository.findByTenantIdAndCustomerIdAndType(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        PageRequest.of(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantEdgeTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEdgeTypesToDto(tenantId, edgeRepository.findTenantEdgeTypes(fromTimeUUID(tenantId))));
    }

    @Override
    public Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey) {
        Edge edge = DaoUtil.getData(edgeRepository.findByRoutingKey(routingKey));
        return Optional.ofNullable(edge);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndRuleChainId(UUID tenantId, UUID ruleChainId) {
        log.debug("Try to find edges by tenantId [{}], ruleChainId [{}]", tenantId, ruleChainId);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByToAndType(new TenantId(tenantId), new RuleChainId(ruleChainId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<Edge>> edgeFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                edgeFutures.add(findByIdAsync(new TenantId(tenantId), relation.getFrom().getId()));
            }
            return Futures.successfulAsList(edgeFutures);
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndDashboardId(UUID tenantId, UUID dashboardId) {
        log.debug("Try to find edges by tenantId [{}], dashboardId [{}]", tenantId, dashboardId);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByToAndType(new TenantId(tenantId), new DashboardId(dashboardId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<Edge>> edgeFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                edgeFutures.add(findByIdAsync(new TenantId(tenantId), relation.getFrom().getId()));
            }
            return Futures.successfulAsList(edgeFutures);
        }, MoreExecutors.directExecutor());
    }

    private List<EntitySubtype> convertTenantEdgeTypesToDto(UUID tenantId, List<String> types) {
        List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(new TenantId(tenantId), EntityType.EDGE, type));
            }
        }
        return list;
    }

}
