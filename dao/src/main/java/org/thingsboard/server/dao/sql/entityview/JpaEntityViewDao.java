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
package org.thingsboard.server.dao.sql.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entityview.EntityViewDao;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;
import org.thingsboard.server.dao.model.sql.EntityViewInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

/**
 * Created by Victor Basanets on 8/31/2017.
 */
@Component
@SqlDao
public class JpaEntityViewDao extends JpaAbstractSearchTextDao<EntityViewEntity, EntityView>
        implements EntityViewDao {

    @Autowired
    private EntityViewRepository entityViewRepository;

    @Override
    protected Class<EntityViewEntity> getEntityClass() {
        return EntityViewEntity.class;
    }

    @Override
    protected CrudRepository<EntityViewEntity, String> getCrudRepository() {
        return entityViewRepository;
    }

    @Override
    public EntityViewInfo findEntityViewInfoById(TenantId tenantId, UUID entityViewId) {
        return DaoUtil.getData(entityViewRepository.findEntityViewInfoById(fromTimeUUID(entityViewId)));
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantId(
                        fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findEntityViewInfosByTenantId(
                        fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EntityViewInfoEntity.entityViewInfoColumnMap)));
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndType(
                        fromTimeUUID(tenantId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findEntityViewInfosByTenantIdAndType(
                        fromTimeUUID(tenantId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EntityViewInfoEntity.entityViewInfoColumnMap)));
    }

    @Override
    public Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name) {
        return Optional.ofNullable(
                DaoUtil.getData(entityViewRepository.findByTenantIdAndName(fromTimeUUID(tenantId), name)));
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId,
                                                                       UUID customerId,
                                                                       PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndCustomerId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)
                ));
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findEntityViewInfosByTenantIdAndCustomerId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EntityViewInfoEntity.entityViewInfoColumnMap)));
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndCustomerIdAndType(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)
                ));
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findEntityViewInfosByTenantIdAndCustomerIdAndType(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EntityViewInfoEntity.entityViewInfoColumnMap)));
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(UUID tenantId, UUID entityId) {
        return service.submit(() -> DaoUtil.convertDataList(
                entityViewRepository.findAllByTenantIdAndEntityId(UUIDConverter.fromTimeUUID(tenantId), UUIDConverter.fromTimeUUID(entityId))));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantEntityViewTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEntityViewTypesToDto(tenantId, entityViewRepository.findTenantEntityViewTypes(fromTimeUUID(tenantId))));
    }

    private List<EntitySubtype> convertTenantEntityViewTypesToDto(UUID tenantId, List<String> types) {
        List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(new TenantId(tenantId), EntityType.ENTITY_VIEW, type));
            }
        }
        return list;
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndEdgeId(UUID tenantId,
                                                               UUID edgeId,
                                                               PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndEdgeId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(edgeId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)
                ));
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndEdgeIdAndType(UUID tenantId, UUID edgeId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndEdgeIdAndType(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(edgeId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)
                ));
    }
}
