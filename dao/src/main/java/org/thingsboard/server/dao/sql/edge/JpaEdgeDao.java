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
package org.thingsboard.server.dao.sql.edge;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edqs.fields.EdgeFields;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.model.sql.EdgeEntity;
import org.thingsboard.server.dao.model.sql.EdgeInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.convertTenantEntityTypesToDto;

@Component
@Slf4j
@SqlDao
public class JpaEdgeDao extends JpaAbstractDao<EdgeEntity, Edge> implements EdgeDao {

    @Autowired
    private EdgeRepository edgeRepository;

    @Override
    protected Class<EdgeEntity> getEntityClass() {
        return EdgeEntity.class;
    }

    @Override
    protected JpaRepository<EdgeEntity, UUID> getRepository() {
        return edgeRepository;
    }

    @Override
    public EdgeInfo findEdgeInfoById(TenantId tenantId, UUID edgeId) {
        return DaoUtil.getData(edgeRepository.findEdgeInfoById(edgeId));
    }

    @Override
    public PageData<Edge> findActiveEdges(PageLink pageLink) {
        return DaoUtil.toPageData(edgeRepository.findActiveEdges(DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                edgeRepository.findIdsByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink))).mapData(EdgeId::fromUUID);
    }

    @Override
    public PageData<Edge> findEdgesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> edgeIds) {
        return service.submit(() -> DaoUtil.convertDataList(edgeRepository.findEdgesByTenantIdAndIdIn(tenantId, edgeIds)));
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> edgeIds) {
        return service.submit(() -> DaoUtil.convertDataList(
                edgeRepository.findEdgesByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, edgeIds)));
    }

    @Override
    public Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String name) {
        Edge edge = DaoUtil.getData(edgeRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(edge);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantEdgeTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEntityTypesToDto(tenantId, EntityType.EDGE, edgeRepository.findTenantEdgeTypes(tenantId)));
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @Override
    public Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey) {
        Edge edge = DaoUtil.getData(edgeRepository.findByRoutingKey(routingKey));
        return Optional.ofNullable(edge);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndEntityId(UUID tenantId, UUID entityId, EntityType entityType, PageLink pageLink) {
        log.debug("Try to find edges by tenantId [{}], entityId [{}], entityType [{}], pageLink [{}]", tenantId, entityId, entityType, pageLink);
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndEntityId(
                        tenantId,
                        entityId,
                        entityType.name(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByTenantIdAndEntityId(UUID tenantId, UUID entityId, EntityType entityType, PageLink pageLink) {
        log.debug("Try to find edge ids by tenantId [{}], entityId [{}], entityType [{}], pageLink [{}]", tenantId, entityId, entityType, pageLink);
        return DaoUtil.pageToPageData(
                edgeRepository.findIdsByTenantIdAndEntityId(
                        tenantId,
                        entityId,
                        entityType.name(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink))).mapData(EdgeId::fromUUID);
    }

    @Override
    public PageData<Edge> findEdgesByTenantProfileId(UUID tenantProfileId, PageLink pageLink) {
        log.debug("Try to find edges by tenantProfileId [{}], pageLink [{}]", tenantProfileId, pageLink);
        return DaoUtil.toPageData(
                edgeRepository.findByTenantProfileId(
                        tenantProfileId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return edgeRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public PageData<Edge> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findEdgesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<EdgeFields> findNextBatch(UUID id, int batchSize) {
        return edgeRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

}
