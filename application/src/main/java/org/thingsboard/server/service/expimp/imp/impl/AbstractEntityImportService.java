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
package org.thingsboard.server.service.expimp.imp.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.export.EntityExportData;
import org.thingsboard.server.common.data.export.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.expimp.ExportableEntitiesService;
import org.thingsboard.server.service.expimp.imp.EntityImportResult;
import org.thingsboard.server.service.expimp.imp.EntityImportService;

public abstract class AbstractEntityImportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityImportService<I, E, D> {

    @Autowired @Lazy
    private ExportableEntitiesService exportableEntitiesService;

    // FIXME what if exporting and importing back already exported entity ? (save version and then load it back)
    /*
     * export entity -> id from env1 -> import this entity -> ...
     *
     * maybe find not only by external id but by internal too ? but then what if we will try
     * */
    @Override
    public final EntityImportResult<E> importEntity(TenantId tenantId, D exportData) {
        E entity = exportData.getMainEntity();
        E existingEntity = findByExternalId(tenantId, entity.getId());

        entity.setExternalId(entity.getId());
        entity.setTenantId(tenantId);

        if (existingEntity == null) {
            entity.setId(null);
        } else {
            entity.setId(existingEntity.getId());
        }

        E savedEntity = prepareAndSaveEntity(tenantId, entity, existingEntity, exportData);

        EntityImportResult<E> importResult = new EntityImportResult<>();
        importResult.setSavedEntity(savedEntity);
        importResult.setOldEntity(existingEntity);
        return importResult;
    }

    protected abstract E prepareAndSaveEntity(TenantId tenantId, E entity, E existingEntity, D exportData);


    private E findByExternalId(TenantId tenantId, I externalId) {
        return exportableEntitiesService.findEntityByExternalId(tenantId, externalId);
    }

    // TODO or maybe set as additional config whether to update related entities when device already exists ?
    // TODO: or also whether to ignore not found internal ids

    // FIXME: review use cases for version controlling: in the same tenant, between tenants, between environments and different tenants
    protected final <ID extends EntityId> ID getInternalId(TenantId tenantId, ID externalId) {
        if (externalId == null) {
            return null;
        }
        HasId<ID> entity = exportableEntitiesService.findEntityByExternalId(tenantId, externalId);
        if (entity == null) {
            throw new IllegalStateException("Cannot find " + externalId.getEntityType() + " by external id " + externalId);
        }
        return entity.getId();
    }

}
