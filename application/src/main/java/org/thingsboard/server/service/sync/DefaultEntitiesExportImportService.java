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
package org.thingsboard.server.service.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.sync.exporting.EntityExportService;
import org.thingsboard.server.service.sync.exporting.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.importing.EntityImportResult;
import org.thingsboard.server.service.sync.importing.EntityImportService;
import org.thingsboard.server.service.sync.importing.EntityImportSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultEntitiesExportImportService implements EntitiesExportImportService, ExportableEntitiesService {

    private final Map<EntityType, EntityExportService<?, ?, ?>> exportServices = new HashMap<>();
    private final Map<EntityType, EntityImportService<?, ?, ?>> importServices = new HashMap<>();
    private final Map<EntityType, Dao<?>> daos = new HashMap<>();

    private final AccessControlService accessControlService;

    protected static final List<EntityType> SUPPORTED_ENTITY_TYPES = List.of(
            EntityType.CUSTOMER, EntityType.ASSET, EntityType.RULE_CHAIN,
            EntityType.DEVICE_PROFILE, EntityType.DEVICE, EntityType.DASHBOARD
    );


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(SecurityUser user, I entityId, EntityExportSettings exportSettings) throws ThingsboardException {
        EntityType entityType = entityId.getEntityType();
        EntityExportService<I, E, EntityExportData<E>> exportService = getExportService(entityType);

        return exportService.getExportData(user, entityId, exportSettings);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(SecurityUser user, EntityExportData<E> exportData, EntityImportSettings importSettings) throws ThingsboardException {
        if (exportData.getEntity() == null || exportData.getEntity().getId() == null) {
            throw new DataValidationException("Invalid entity data");
        }

        EntityType entityType = exportData.getEntityType();
        EntityImportService<I, E, EntityExportData<E>> importService = getImportService(entityType);

        // TODO [viacheslav]: might throw DataIntegrityViolationException with cause of ConstraintViolationException: need to give normal error
        return importService.importEntity(user, exportData, importSettings);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<EntityImportResult<ExportableEntity<EntityId>>> importEntities(SecurityUser user, List<EntityExportData<ExportableEntity<EntityId>>> exportDataList, EntityImportSettings importSettings) throws ThingsboardException {
        exportDataList.sort(Comparator.comparing(exportData -> SUPPORTED_ENTITY_TYPES.indexOf(exportData.getEntityType())));

        List<EntityImportResult<ExportableEntity<EntityId>>> importResults = new ArrayList<>();
        for (EntityExportData<ExportableEntity<EntityId>> exportData : exportDataList) {
            importResults.add(importEntity(user, exportData, importSettings));
        }
        return importResults;
    }


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndExternalId(TenantId tenantId, I externalId) {
        EntityType entityType = externalId.getEntityType();
        if (SUPPORTED_ENTITY_TYPES.contains(entityType)) {
            ExportableEntityDao<E> dao = (ExportableEntityDao<E>) getDao(entityType);
            return dao.findByTenantIdAndExternalId(tenantId.getId(), externalId.getId());
        } else {
            return null;
        }
    }

    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityByTenantIdAndId(TenantId tenantId, I id) {
        Dao<E> dao = (Dao<E>) getDao(id.getEntityType());
        E entity = dao.findById(tenantId, id.getId());
        if (entity instanceof HasTenantId && !((HasTenantId) entity).getTenantId().equals(tenantId)) {
            return null;
        }
        return entity;
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndName(TenantId tenantId, EntityType entityType, String name) {
        ExportableEntityDao<E> dao = (ExportableEntityDao<E>) getDao(entityType);
        return dao.findFirstByTenantIdAndName(tenantId.getId(), name);
    }


    @Override
    public void checkPermission(SecurityUser user, HasId<? extends EntityId> entity, EntityType entityType, Operation operation) throws ThingsboardException {
        if (entity instanceof HasTenantId) {
            accessControlService.checkPermission(user, Resource.of(entityType), operation, entity.getId(), (HasTenantId) entity);
        } else if (entity != null) {
            accessControlService.checkPermission(user, Resource.of(entityType), operation);
        }
    }

    @Override
    public void checkPermission(SecurityUser user, EntityId entityId, Operation operation) throws ThingsboardException {
        HasId<EntityId> entity = findEntityByTenantIdAndId(user.getTenantId(), entityId);
        checkPermission(user, entity, entityId.getEntityType(), operation);
    }


    @SuppressWarnings("unchecked")
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityExportService<I, E, D> getExportService(EntityType entityType) {
        if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("Export for entity type " + entityType + " is not supported");
        }
        return (EntityExportService<I, E, D>) exportServices.get(entityType);
    }

    @SuppressWarnings("unchecked")
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityImportService<I, E, D> getImportService(EntityType entityType) {
        if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("Import for entity type " + entityType + " is not supported");
        }
        return (EntityImportService<I, E, D>) importServices.get(entityType);
    }

    private Dao<?> getDao(EntityType entityType) {
        return daos.get(entityType);
    }

    @Autowired
    private void setServices(Collection<EntityExportService<?, ?, ?>> exportServices,
                             Collection<EntityImportService<?, ?, ?>> importServices,
                             Collection<Dao<?>> daos) {
        exportServices.forEach(entityExportService -> {
            this.exportServices.put(entityExportService.getEntityType(), entityExportService);
        });
        importServices.forEach(entityImportService -> {
            this.importServices.put(entityImportService.getEntityType(), entityImportService);
        });
        daos.forEach(dao -> {
            if (dao.getEntityType() != null) {
                this.daos.put(dao.getEntityType(), dao);
            }
        });
    }

}
