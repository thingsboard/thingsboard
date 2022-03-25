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
import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.exportimport.EntitiesExportImportService;
import org.thingsboard.server.service.exportimport.exporting.EntityExportSettings;
import org.thingsboard.server.service.exportimport.exporting.data.EntityExportData;
import org.thingsboard.server.service.exportimport.importing.EntityImportResult;
import org.thingsboard.server.service.exportimport.importing.EntityImportSettings;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/entities")
@TbCoreComponent
@RequiredArgsConstructor
public class EntitiesExportImportController extends BaseController {

    private final EntitiesExportImportService exportImportService;


    @PostMapping("/export/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public EntityExportData<?> exportEntity(@ApiParam(allowableValues = "DEVICE, DEVICE_PROFILE, ASSET, RULE_CHAIN, DASHBOARD, CUSTOMER")
                                            @PathVariable EntityType entityType,
                                            @PathVariable("entityId") UUID entityUuid,
                                            @RequestParam(defaultValue = "false") boolean exportInboundRelations) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
        EntityExportSettings exportSettings = EntityExportSettings.builder()
                .exportInboundRelations(exportInboundRelations)
                .build();
        try {
            return exportEntity(getCurrentUser(), entityId, exportSettings);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PostMapping("/import")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public EntityImportResult<ExportableEntity<EntityId>> importEntity(@RequestBody EntityExportData<ExportableEntity<EntityId>> exportData,
                                                                       @RequestParam(defaultValue = "false") boolean importInboundRelations) throws ThingsboardException {
        EntityImportSettings importSettings = EntityImportSettings.builder()
                .importInboundRelations(importInboundRelations)
                .build();
        try {
            return importEntity(getCurrentUser(), exportData, importSettings);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    // TODO [viacheslav]: export and import of batches
    // TODO [viacheslav]: api to export and import whole customer, whole tenant

    private EntityExportData<ExportableEntity<EntityId>> exportEntity(SecurityUser user, EntityId entityId, EntityExportSettings exportSettings) throws ThingsboardException {
        checkEntityId(entityId, Operation.READ);
        return exportImportService.exportEntity(user.getTenantId(), entityId, exportSettings);
    }

    private EntityImportResult<ExportableEntity<EntityId>> importEntity(SecurityUser user, EntityExportData<ExportableEntity<EntityId>> exportData, EntityImportSettings importSettings) throws ThingsboardException {
        ExportableEntity<?> existingEntity = exportImportService.findEntityByExternalId(user.getTenantId(), exportData.getMainEntity().getId());
        if (existingEntity != null) {// todo [viacheslav] maybe need to extract permission check to BaseController and put there permission checks from other controllers
            checkEntityId(existingEntity.getId(), Operation.WRITE);
            if (importSettings.isImportInboundRelations() && CollectionUtils.isNotEmpty(exportData.getInboundRelations())) {
                for (EntityId fromId : exportData.getInboundRelations().stream().map(EntityRelation::getFrom).collect(Collectors.toSet())) {
                    // FIXME [viacheslav]: fromId is external
//                    checkEntityId(fromId, Operation.WRITE);
                }
            }
        } else {
            checkEntity(null, exportData.getMainEntity(), Resource.of(exportData.getEntityType()));
        }

        EntityImportResult<ExportableEntity<EntityId>> importResult = exportImportService.importEntity(getTenantId(), exportData, importSettings);
        onEntityUpdatedOrCreated(user, importResult.getSavedEntity(), importResult.getOldEntity(), importResult.getOldEntity() == null);

        return importResult;
    }

}
