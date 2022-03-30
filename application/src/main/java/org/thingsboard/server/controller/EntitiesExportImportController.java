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
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.exportimport.EntitiesExportImportService;
import org.thingsboard.server.service.exportimport.exporting.EntityExportSettings;
import org.thingsboard.server.service.exportimport.exporting.data.EntityExportData;
import org.thingsboard.server.service.exportimport.importing.EntityImportResult;
import org.thingsboard.server.service.exportimport.importing.EntityImportSettings;
import org.thingsboard.server.service.query.EntityQueryService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CREATED_TIME;

@RestController
@RequestMapping("/api/entities")
@TbCoreComponent
@RequiredArgsConstructor
public class EntitiesExportImportController extends BaseController {

    private final EntitiesExportImportService exportImportService;
    private final EntityQueryService entityQueryService;


    // TODO [viacheslav]: export and import of batches
    // TODO [viacheslav]: api to export and import whole customer, whole tenant


    @PostMapping("/exportByFilter")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<?>> exportEntitiesByFilter(@RequestBody EntityFilter filter,
                                                            @RequestParam(defaultValue = "false") boolean exportInboundRelations,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "100") int pageSize) throws ThingsboardException {
        EntityDataPageLink pageLink = new EntityDataPageLink();
        pageLink.setPage(page);
        pageLink.setPageSize(pageSize);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, CREATED_TIME), EntityDataSortOrder.Direction.DESC));
        EntityDataQuery entityDataQuery = new EntityDataQuery(filter, pageLink, List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, CREATED_TIME)),
                Collections.emptyList(), Collections.emptyList());

        SecurityUser user = getCurrentUser();
        EntityExportSettings exportSettings = toExportSettings(exportInboundRelations);

        try {
            // FIXME [viacheslav]: check read permission for relation fromId
            return entityQueryService.findEntityDataByQuery(user, entityDataQuery).getData().stream()
                    .map(EntityData::getEntityId)
                    .map(entityId -> {
                        return exportImportService.exportEntity(user.getTenantId(), entityId, exportSettings);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping("/exportByQuery")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<?>> exportEntitiesByQuery(@RequestBody EntityDataQuery query,
                                                           @RequestParam(defaultValue = "false") boolean exportInboundRelations) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        EntityExportSettings exportSettings = toExportSettings(exportInboundRelations);
// FIXME [viacheslav]: check read permission for relation fromId
        try {
            return entityQueryService.findEntityDataByQuery(user, query).getData().stream()
                    .map(EntityData::getEntityId)
                    .map(entityId -> exportImportService.exportEntity(user.getTenantId(), entityId, exportSettings))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping("/export/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public EntityExportData<?> exportEntity(@ApiParam(allowableValues = "DEVICE, DEVICE_PROFILE, ASSET, RULE_CHAIN, DASHBOARD, CUSTOMER")
                                            @PathVariable EntityType entityType,
                                            @PathVariable("entityId") UUID entityUuid,
                                            @RequestParam(defaultValue = "false") boolean exportInboundRelations) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
        checkEntityId(entityId, Operation.READ);

        SecurityUser user = getCurrentUser();
        EntityExportSettings exportSettings = toExportSettings(exportInboundRelations);

        try { // FIXME [viacheslav]: check read permission for relation fromId
            return exportImportService.exportEntity(user.getTenantId(), entityId, exportSettings);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PostMapping("/import")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityImportResult<ExportableEntity<EntityId>>> importEntity(@RequestBody List<EntityExportData<ExportableEntity<EntityId>>> exportDataList,
                                                                             @RequestParam(defaultValue = "false") boolean importInboundRelations) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        EntityImportSettings importSettings = toImportSettings(importInboundRelations);

        for (EntityExportData<ExportableEntity<EntityId>> exportData : exportDataList) {
            checkPermissionsForImport(user, exportData, importSettings);
        }

        try {
            return exportImportService.importEntities(user.getTenantId(), exportDataList, importSettings);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    public void checkPermissionsForExport(SecurityUser user, EntityId entityId, EntityExportSettings exportSettings) throws ThingsboardException {
        checkEntityId(entityId, Operation.READ);
        if (exportSettings.isExportInboundRelations()) {
            for (EntityRelation entityRelation : relationService.findByTo(user.getTenantId(), entityId, RelationTypeGroup.COMMON)) {
                EntityId fromId = entityRelation.getFrom();
                checkEntityId(fromId, Operation.READ);
            }
        }
    }

    public void checkPermissionsForImport(SecurityUser user, EntityExportData<ExportableEntity<EntityId>> exportData, EntityImportSettings importSettings) throws ThingsboardException {
        ExportableEntity<EntityId> existingEntity = exportImportService.findEntityByExternalId(user.getTenantId(), exportData.getMainEntity().getId());
        if (existingEntity != null) {
            checkEntityId(existingEntity.getId(), Operation.WRITE);
        } else {
            accessControlService.checkPermission(user, Resource.of(exportData.getEntityType()), Operation.CREATE);
        }

        if (importSettings.isImportInboundRelations() && CollectionUtils.isNotEmpty(exportData.getInboundRelations())) {
            for (EntityRelation entityRelation : exportData.getInboundRelations()) {
                ExportableEntity<EntityId> entityFrom = exportImportService.findEntityByExternalId(user.getTenantId(), entityRelation.getFrom());
                if (entityFrom != null) {
                    accessControlService.checkPermission(user, Resource.of(entityFrom.getId().getEntityType()), Operation.WRITE, entityFrom.getId(), entityFrom);
                } else {
                    throw new ThingsboardException("Relation with non-existing entity", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
        }
    }


    private EntityImportSettings toImportSettings(boolean importInboundRelations) {
        EntityImportSettings importSettings = EntityImportSettings.builder()
                .importInboundRelations(importInboundRelations)
                .build();
        return importSettings;
    }

    private EntityExportSettings toExportSettings(boolean exportInboundRelations) {
        return EntityExportSettings.builder()
                .exportInboundRelations(exportInboundRelations)
                .build();
    }

}
