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
package org.thingsboard.server.service.expimp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.export.EntityExportData;
import org.thingsboard.server.common.data.export.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.expimp.exp.EntityExportService;
import org.thingsboard.server.service.expimp.exp.EntityExportSettings;
import org.thingsboard.server.service.expimp.imp.EntityImportResult;
import org.thingsboard.server.service.expimp.imp.EntityImportService;
import org.thingsboard.server.service.expimp.imp.EntityImportSettings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// FIXME [viacheslav]: review packages and classes naming
@Service
@TbCoreComponent
public class DefaultEntitiesExportImportService implements EntitiesExportImportService {

    private final Map<EntityType, EntityExportService<?, ?, ?>> exportServices = new HashMap<>();
    private final Map<EntityType, EntityImportService<?, ?, ?>> importServices = new HashMap<>();
    private final Map<EntityType, ExportableEntityDao<?>> daos = new HashMap<>();


    // TODO [viacheslav]: export and import of the whole tenant
    // TODO [viacheslav]: export and import of the whole customer ?
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(TenantId tenantId, I entityId, EntityExportSettings exportSettings) {
        EntityType entityType = entityId.getEntityType();
        EntityExportService<I, E, EntityExportData<E>> exportService = getExportService(entityType);

        return exportService.getExportData(tenantId, entityId, exportSettings);
    }

    // FIXME [viacheslav]: somehow validate export data
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(TenantId tenantId, EntityExportData<E> exportData, EntityImportSettings importSettings) {
        EntityType entityType = exportData.getEntityType();
        EntityImportService<I, E, EntityExportData<E>> importService = getImportService(entityType);

        return importService.importEntity(tenantId, exportData, importSettings);
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
        return (EntityExportService<I, E, D>) exportServices.get(entityType);
    }

    @SuppressWarnings("unchecked")
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityImportService<I, E, D> getImportService(EntityType entityType) {
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
