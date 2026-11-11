// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.edqs.fields.EntityViewFields;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.entityview.EntityViewDao;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.convertTenantEntityTypesToDto;

@Component
@Slf4j
@SqlDao
public class JpaEntityViewDao extends JpaAbstractDao<EntityViewEntity, EntityView> implements EntityViewDao, TenantEntityDao<EntityView> {

    @Autowired
    private EntityViewRepository entityViewRepository;

    @Override
    protected Class<EntityViewEntity> getEntityClass() {
        return EntityViewEntity.class;
    }

    @Override
    protected JpaRepository<EntityViewEntity, UUID> getRepository() {
        return entityViewRepository;
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name) {
        return Optional.ofNullable(
                DaoUtil.getData(entityViewRepository.findFirstByTenantIdAndName(tenantId, name)));
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId,
                                                                       UUID customerId,
                                                                       PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)
                ));
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)
                ));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndEntityId(UUID tenantId, UUID entityId) {
        return DaoUtil.convertDataList(
                entityViewRepository.findAllByTenantIdAndEntityId(tenantId, entityId));
    }

    @Override
    public boolean existsByTenantIdAndEntityId(UUID tenantId, UUID entityId) {
        return entityViewRepository.existsByTenantIdAndEntityId(tenantId, entityId);
    }

    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> entityViewIds) {
        return DaoUtil.getEntitiesByTenantIdAndIdIn(entityViewIds, ids ->
                entityViewRepository.findEntityViewsByTenantIdAndIdIn(tenantId, ids), service);
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndIds(UUID tenantId, List<UUID> entityViewIds) {
        return DaoUtil.convertDataList(entityViewRepository.findEntityViewsByTenantIdAndIdIn(tenantId, entityViewIds));
    }

    @Override
    public PageData<EntityView> findEntityViewsByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(entityViewRepository
                .findByEntityGroupId(
                        groupId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityView> findEntityViewsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(entityViewRepository
                .findByEntityGroupIds(
                        groupIds,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityView> findEntityViewsByEntityGroupIdsAndType(List<UUID> groupIds, String type, PageLink pageLink) {
        return DaoUtil.toPageData(entityViewRepository
                .findByEntityGroupIdsAndType(
                        groupIds,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantEntityViewTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEntityTypesToDto(tenantId, EntityType.ENTITY_VIEW, entityViewRepository.findTenantEntityViewTypes(tenantId)));
    }

    @Override
    public EntityView findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(entityViewRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<EntityView> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findEntityViewsByTenantId(tenantId, pageLink);
    }

    @Override
    public EntityViewId getExternalIdByInternal(EntityViewId internalId) {
        return Optional.ofNullable(entityViewRepository.getExternalIdById(internalId.getId()))
                .map(EntityViewId::new).orElse(null);
    }

    @Override
    public EntityView findByTenantIdAndName(UUID tenantId, String name) {
        return findEntityViewByTenantIdAndName(tenantId, name).orElse(null);
    }

    @Override
    public PageData<EntityViewId> findIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        Page<UUID> ids;
        if (customerId != null) {
            ids = entityViewRepository.findIdsByTenantIdAndCustomerId(tenantId, customerId, DaoUtil.toPageable(pageLink));
        } else {
            ids = entityViewRepository.findIdsByTenantIdAndNullCustomerId(tenantId, DaoUtil.toPageable(pageLink));
        }
        return DaoUtil.pageToPageData(ids, EntityViewId::new);
    }

    @Override
    public PageData<EntityView> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<EntityViewFields> findNextBatch(UUID id, int batchSize) {
        return entityViewRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public List<EntityInfo> findEntityInfosByNamePrefix(TenantId tenantId, String name) {
        return entityViewRepository.findEntityInfosByNamePrefix(tenantId.getId(), name);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }

}
