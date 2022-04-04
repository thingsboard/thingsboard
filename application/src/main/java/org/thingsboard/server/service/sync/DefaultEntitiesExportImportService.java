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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.exporting.EntityExportService;
import org.thingsboard.server.service.sync.exporting.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.importing.EntityImportResult;
import org.thingsboard.server.service.sync.importing.EntityImportService;
import org.thingsboard.server.service.sync.importing.EntityImportSettings;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
public class DefaultEntitiesExportImportService implements EntitiesExportImportService, ExportableEntitiesService {

    private final Map<EntityType, EntityExportService<?, ?, ?>> exportServices = new HashMap<>();
    private final Map<EntityType, EntityImportService<?, ?, ?>> importServices = new HashMap<>();
    private final Map<EntityType, Dao<?>> daos = new HashMap<>();

    protected static final List<EntityType> SUPPORTED_ENTITY_TYPES = List.of(
            EntityType.CUSTOMER, EntityType.ASSET, EntityType.RULE_CHAIN,
            EntityType.DEVICE_PROFILE, EntityType.DEVICE, EntityType.DASHBOARD
    );


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(TenantId tenantId, I entityId, EntityExportSettings exportSettings) {
        EntityType entityType = entityId.getEntityType();
        EntityExportService<I, E, EntityExportData<E>> exportService = getExportService(entityType);

        return exportService.getExportData(tenantId, entityId, exportSettings);
    }


    // TODO [viacheslav]: validate export data
    @Transactional
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(TenantId tenantId, EntityExportData<E> exportData, EntityImportSettings importSettings) {
        EntityType entityType = exportData.getEntityType();
        EntityImportService<I, E, EntityExportData<E>> importService = getImportService(entityType);

        return importService.importEntity(tenantId, exportData, importSettings);
    }

    @Transactional
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> List<EntityImportResult<E>> importEntities(TenantId tenantId, List<EntityExportData<E>> exportDataList, EntityImportSettings importSettings) {
        return exportDataList.stream()
                .sorted(Comparator.comparing(exportData -> SUPPORTED_ENTITY_TYPES.indexOf(exportData.getEntityType())))
                // TODO [viacheslav]: order for rule chains (depending on references)
                .map(exportData -> importEntity(tenantId, exportData, importSettings))
                .collect(Collectors.toList());
    }


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByExternalId(TenantId tenantId, I externalId) {
        EntityType entityType = externalId.getEntityType();
        if (SUPPORTED_ENTITY_TYPES.contains(entityType)) {
            ExportableEntityDao<E> dao = (ExportableEntityDao<E>) getDao(entityType);
            E entity = dao.findByTenantIdAndExternalId(tenantId.getId(), externalId.getId());
            if (entity != null) {
                return entity;
            }
        }
        return findEntityById(tenantId, externalId);
    }

    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityById(TenantId tenantId, I id) {
        Dao<E> dao = (Dao<E>) getDao(id.getEntityType());
        return dao.findById(tenantId, id.getId());
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
