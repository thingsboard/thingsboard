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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.export.EntityExportData;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.expimp.EntitiesExportImportService;
import org.thingsboard.server.service.expimp.imp.EntityImportResult;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.UUID;

@RestController
@RequestMapping("/api/entities")
@TbCoreComponent
@RequiredArgsConstructor
public class EntitiesExportImportController extends BaseController {

    private final EntitiesExportImportService exportImportService;


    @PostMapping("/export/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public EntityExportData<?> exportEntity(@ApiParam(allowableValues = "DEVICE, DEVICE_PROFILE, ASSET, RULE_CHAIN, DASHBOARD, CUSTOMER") @PathVariable EntityType entityType,
                                            @PathVariable("entityId") UUID entityUuid) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
        try {
            return exportEntity(getCurrentUser(), entityId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


//    @PostMapping("/export/batch")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public EntitiesExportResponse exportEntities(@RequestBody EntitiesExportRequest exportRequest) throws ThingsboardException {
//        TenantId tenantId = getTenantId();
//
//        EntitiesExportResponse exportResponse = new EntitiesExportResponse();
//
//        Map<EntityType, List<EntityExportData<HasId<EntityId>>>> result = new HashMap<>();
//        exportRequest.getEntities().forEach((entityType, entityIds) -> {
//            List<EntityExportData<HasId<EntityId>>> exportDataForEntityType = new LinkedList<>();
//            entityIds.forEach(entityId -> {
//                EntityExportData<HasId<EntityId>> exportData = exportImportService.exportEntity(tenantId, entityId);
//                exportDataForEntityType.add(exportData);
//            });
//            result.put(entityType, exportDataForEntityType);
//        });
//
//        exportResponse.setExportData(result);
//        return exportResponse;
//    }
    // TODO: export and import of batches
    // TODO: api to export and import whole customer, whole tenant


    @PostMapping("/import")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public <E extends HasId<I> & HasName & HasTenantId, I extends EntityId, D extends EntityExportData<E>> EntityImportResult<E> importEntity(@RequestBody D exportData) throws ThingsboardException {
        try {
            return importEntity(getCurrentUser(), exportData);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    private <E extends HasId<I>, I extends EntityId> EntityExportData<HasId<EntityId>> exportEntity(SecurityUser user, I entityId) throws ThingsboardException {
        checkEntityId(entityId, Operation.READ);
        return exportImportService.exportEntity(getTenantId(), entityId);
    }

    private <E extends HasId<I> & HasName & HasTenantId, I extends EntityId, D extends EntityExportData<E>> EntityImportResult<E> importEntity(SecurityUser user, D exportData) throws ThingsboardException {
        E existingEntity = exportImportService.findEntityByExternalId(user.getTenantId(), exportData.getMainEntity().getId());
        if (existingEntity != null) {
            checkEntityId(existingEntity.getId(), Operation.WRITE); // todo maybe need to extract permission check to BaseController and put there permission checks from other controllers
        } else {
            checkEntity(null, exportData.getMainEntity(), Resource.of(exportData.getEntityType()));
        }

        EntityImportResult<E> importResult = exportImportService.importEntity(getTenantId(), exportData);
        onEntityUpdatedOrCreated(user, importResult.getSavedEntity(), importResult.getOldEntity(), importResult.getOldEntity() == null);

        return importResult;
    }

}
