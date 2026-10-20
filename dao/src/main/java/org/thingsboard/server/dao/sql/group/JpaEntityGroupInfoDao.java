// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.group.EntityGroupInfoDao;
import org.thingsboard.server.dao.model.sql.EntityGroupInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@SqlDao
public class JpaEntityGroupInfoDao extends JpaAbstractDao<EntityGroupInfoEntity, EntityGroupInfo> implements EntityGroupInfoDao {

    @Autowired
    private EntityGroupInfoRepository entityGroupInfoRepository;

    @Override
    protected Class<EntityGroupInfoEntity> getEntityClass() {
        return EntityGroupInfoEntity.class;
    }

    @Override
    protected JpaRepository<EntityGroupInfoEntity, UUID> getRepository() {
        return entityGroupInfoRepository;
    }

    @Override
    public EntityInfo findEntityGroupEntityInfoById(TenantId tenantId, UUID entityGroupId) {
        return entityGroupInfoRepository.findEntityGroupEntityInfoById(entityGroupId);
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupsByType(UUID tenantId, UUID parentEntityId,
                                                            EntityType parentEntityType, EntityType groupType, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupInfoRepository
                .findEntityGroupsByType(
                        parentEntityId,
                        parentEntityType,
                        groupType,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByType(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType, PageLink pageLink) {
        return DaoUtil.pageToPageData(entityGroupInfoRepository
                .findEntityGroupEntityInfosByType(
                        parentEntityId,
                        parentEntityType,
                        groupType,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupsByOwnerIdsAndType(UUID tenantId, List<UUID> ownerIds, EntityType groupType, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupInfoRepository
                .findEntityGroupsByOwnerIdsAndType(
                        ownerIds,
                        groupType,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByOwnerIdsAndType(UUID tenantId, List<UUID> ownerIds, EntityType groupType, PageLink pageLink) {
        return DaoUtil.pageToPageData(entityGroupInfoRepository
                .findEntityGroupEntityInfosByOwnerIdsAndType(
                        ownerIds,
                        groupType,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupsByIds(UUID tenantId, List<UUID> entityGroupIds, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupInfoRepository
                .findEntityGroupsByIds(
                        entityGroupIds,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByIds(UUID tenantId, List<UUID> entityGroupIds, PageLink pageLink) {
        return DaoUtil.pageToPageData(entityGroupInfoRepository
                .findEntityGroupEntityInfosByIds(
                        entityGroupIds,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupsByTypeOrIds(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType,
                                                                 List<UUID> entityGroupIds, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupInfoRepository
                .findEntityGroupsByTypeOrIds(
                        parentEntityId,
                        parentEntityType,
                        groupType,
                        entityGroupIds,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByTypeOrIds(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType, List<UUID> entityGroupIds, PageLink pageLink) {
        return DaoUtil.pageToPageData(entityGroupInfoRepository
                .findEntityGroupEntityInfosByTypeOrIds(
                        parentEntityId,
                        parentEntityType,
                        groupType,
                        entityGroupIds,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityGroupInfo> findEdgeEntityGroupsByOwnerIdAndType(UUID tenantId, UUID edgeId, UUID ownerId, EntityType ownerType, String relationType, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupInfoRepository.findEdgeEntityGroupsByOwnerIdAndType(
                edgeId,
                ownerId.toString(),
                ownerType.name(),
                relationType,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<EntityGroupInfo> findEntityGroupByTypeAndName(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType, String name) {
        return Optional.ofNullable(DaoUtil.getData(entityGroupInfoRepository.findEntityGroupByTypeAndName(
                parentEntityId,
                parentEntityType,
                groupType,
                name)));
    }
}
