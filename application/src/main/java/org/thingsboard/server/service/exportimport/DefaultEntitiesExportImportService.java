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
package org.thingsboard.server.service.exportimport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.exportimport.exporting.EntityExportService;
import org.thingsboard.server.service.exportimport.exporting.EntityExportSettings;
import org.thingsboard.server.service.exportimport.exporting.data.EntityExportData;
import org.thingsboard.server.service.exportimport.importing.EntityImportResult;
import org.thingsboard.server.service.exportimport.importing.EntityImportService;
import org.thingsboard.server.service.exportimport.importing.EntityImportSettings;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// FIXME [viacheslav]: review packages and classes naming
@Service
@TbCoreComponent
public class DefaultEntitiesExportImportService implements EntitiesExportImportService {

    private final Map<EntityType, EntityExportService<?, ?, ?>> exportServices = new HashMap<>();
    private final Map<EntityType, EntityImportService<?, ?, ?>> importServices = new HashMap<>();
    private final Map<EntityType, ExportableEntityDao<?>> daos = new HashMap<>();

    protected static final List<EntityType> SUPPORTED_ENTITY_TYPES = List.of(
            EntityType.CUSTOMER, EntityType.ASSET, EntityType.RULE_CHAIN,
            EntityType.DEVICE_PROFILE, EntityType.DEVICE, EntityType.DASHBOARD
    );


    // TODO [viacheslav]: export and import of the whole tenant
    // TODO [viacheslav]: export and import of the whole customer ?
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(TenantId tenantId, I entityId, EntityExportSettings exportSettings) {
        EntityType entityType = entityId.getEntityType();
        EntityExportService<I, E, EntityExportData<E>> exportService = getExportService(entityType);

        return exportService.getExportData(tenantId, entityId, exportSettings);
    }


    // FIXME [viacheslav]: somehow validate export data
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
                .map(exportData -> importEntity(tenantId, exportData, importSettings))
                .collect(Collectors.toList());
    }


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityById(TenantId tenantId, I id) {
        ExportableEntityDao<E> dao = getDao(id.getEntityType());
        return dao.findByTenantIdAndId(tenantId.getId(), id.getId());
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByExternalId(TenantId tenantId, I externalId) {
        ExportableEntityDao<E> dao = getDao(externalId.getEntityType());
        return Optional.ofNullable(dao.findByTenantIdAndExternalId(tenantId.getId(), externalId.getId()))
                .orElseGet(() -> findEntityById(tenantId, externalId));
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

    @SuppressWarnings("unchecked")
    private <E> ExportableEntityDao<E> getDao(EntityType entityType) {
        return (ExportableEntityDao<E>) daos.get(entityType);
    }

    @Autowired
    private void setServices(Collection<EntityExportService<?, ?, ?>> exportServices,
                             Collection<EntityImportService<?, ?, ?>> importServices,
                             Collection<ExportableEntityDao<?>> daos) {
        exportServices.forEach(entityExportService -> {
            this.exportServices.put(entityExportService.getEntityType(), entityExportService);
        });
        importServices.forEach(entityImportService -> {
            this.importServices.put(entityImportService.getEntityType(), entityImportService);
        });
        daos.forEach(dao -> {
            this.daos.put(dao.getEntityType(), dao);
        });
    }

}
