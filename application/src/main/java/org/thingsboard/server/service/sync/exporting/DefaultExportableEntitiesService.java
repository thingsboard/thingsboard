/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.exporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.sync.exporting.data.request.CustomEntityFilterExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.CustomEntityQueryExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityListExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityTypeExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.ExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.SingleEntityExportRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CREATED_TIME;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultExportableEntitiesService implements ExportableEntitiesService {

    private final Map<EntityType, Dao<?>> daos = new HashMap<>();

    private final EntityService entityService;
    private final AccessControlService accessControlService;


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndExternalId(TenantId tenantId, I externalId) {
        EntityType entityType = externalId.getEntityType();
        Dao<E> dao = getDao(entityType);

        if (dao instanceof ExportableEntityDao) {
            ExportableEntityDao<E> exportableEntityDao = (ExportableEntityDao<E>) dao;
            return exportableEntityDao.findByTenantIdAndExternalId(tenantId.getId(), externalId.getId());
        } else {
            return null;
        }
    }

    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityByTenantIdAndId(TenantId tenantId, I id) {
        EntityType entityType = id.getEntityType();
        Dao<E> dao = getDao(entityType);

        E entity = dao.findById(tenantId, id.getId());
        if (((HasTenantId) entity).getTenantId().equals(tenantId)) {
            return entity;
        }
        return null;
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndName(TenantId tenantId, EntityType entityType, String name) {
        Dao<E> dao = getDao(entityType);

        if (dao instanceof ExportableEntityDao) {
            ExportableEntityDao<E> exportableEntityDao = (ExportableEntityDao<E>) dao;
            try {
                return exportableEntityDao.findByTenantIdAndName(tenantId.getId(), name);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        return null;
    }


    @Override
    public List<EntityId> findEntitiesForRequest(TenantId tenantId, ExportRequest request) {
        switch (request.getType()) {
            case SINGLE_ENTITY: {
                return List.of(((SingleEntityExportRequest) request).getEntityId());
            }
            case ENTITY_LIST: {
                return ((EntityListExportRequest) request).getEntitiesIds();
            }
            case ENTITY_TYPE: {
                EntityTypeExportRequest exportRequest = (EntityTypeExportRequest) request;
                EntityTypeFilter entityTypeFilter = new EntityTypeFilter();
                entityTypeFilter.setEntityType(exportRequest.getEntityType());

                CustomerId customerId = Optional.ofNullable(exportRequest.getCustomerId()).orElse(new CustomerId(EntityId.NULL_UUID));
                return findEntitiesByFilter(tenantId, customerId, entityTypeFilter, exportRequest.getPage(), exportRequest.getPageSize());
            }
            case CUSTOM_ENTITY_FILTER: {
                CustomEntityFilterExportRequest exportRequest = (CustomEntityFilterExportRequest) request;
                EntityFilter filter = exportRequest.getFilter();

                CustomerId customerId = Optional.ofNullable(exportRequest.getCustomerId()).orElse(new CustomerId(EntityId.NULL_UUID));
                return findEntitiesByFilter(tenantId, customerId, filter, exportRequest.getPage(), exportRequest.getPageSize());
            }
            case CUSTOM_ENTITY_QUERY: {
                CustomEntityQueryExportRequest exportRequest = (CustomEntityQueryExportRequest) request;
                EntityDataQuery query = exportRequest.getQuery();

                CustomerId customerId = Optional.ofNullable(exportRequest.getCustomerId()).orElse(new CustomerId(EntityId.NULL_UUID));
                return findEntitiesByQuery(tenantId, customerId, query);
            }
            default: {
                throw new IllegalArgumentException("Export request is not supported");
            }
        }
    }

    private List<EntityId> findEntitiesByFilter(TenantId tenantId, CustomerId customerId, EntityFilter filter, int page, int pageSize) {
        EntityDataPageLink pageLink = new EntityDataPageLink();
        pageLink.setPage(page);
        pageLink.setPageSize(pageSize);
        EntityKey sortProperty = new EntityKey(EntityKeyType.ENTITY_FIELD, CREATED_TIME);
        pageLink.setSortOrder(new EntityDataSortOrder(sortProperty, EntityDataSortOrder.Direction.DESC));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, List.of(sortProperty), Collections.emptyList(), Collections.emptyList());
        return findEntitiesByQuery(tenantId, customerId, query);
    }

    private List<EntityId> findEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        try {
            return entityService.findEntityDataByQuery(tenantId, customerId, query).getData().stream()
                    .map(EntityData::getEntityId)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Failed to find entity data by query: {}", e.getMessage());
            throw new IllegalArgumentException("Entity filter cannot be processed");
        }
    }


    @Override
    public void checkPermission(SecurityUser user, HasId<? extends EntityId> entity, EntityType entityType, Operation operation) throws ThingsboardException {
        if (entity instanceof HasTenantId) {
            accessControlService.checkPermission(user, Resource.of(entityType), operation, entity.getId(), (HasTenantId) entity);
        } else {
            accessControlService.checkPermission(user, Resource.of(entityType), operation);
        }
    }

    @Override
    public void checkPermission(SecurityUser user, EntityId entityId, Operation operation) throws ThingsboardException {
        HasId<EntityId> entity = findEntityByTenantIdAndId(user.getTenantId(), entityId);
        checkPermission(user, entity, entityId.getEntityType(), operation);
    }


    @SuppressWarnings("unchecked")
    private <E> Dao<E> getDao(EntityType entityType) {
        return (Dao<E>) daos.get(entityType);
    }

    @Autowired
    private void setDaos(Collection<Dao<?>> daos) {
        daos.forEach(dao -> {
            if (dao.getEntityType() != null) {
                this.daos.put(dao.getEntityType(), dao);
            }
        });
    }

}
