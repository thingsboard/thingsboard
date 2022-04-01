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

import lombok.RequiredArgsConstructor;
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
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.EntitiesExportImportService;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.importing.EntityImportResult;
import org.thingsboard.server.service.sync.importing.EntityImportSettings;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CREATED_TIME;

@RestController
@RequestMapping("/api/entities")
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
@TbCoreComponent
@RequiredArgsConstructor
public class EntitiesExportImportController extends BaseController {

    private final EntitiesExportImportService exportImportService;
    private final ExportableEntitiesService exportableEntitiesService;
    private final EntityService entityService;


    @PostMapping("/export/{entityType}/{id}")
    public EntityExportData<ExportableEntity<EntityId>> exportSingleEntity(@PathVariable EntityType entityType,
                                                                           @PathVariable UUID id,
                                                                           @RequestParam Map<String, String> exportSettingsParams) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, id);
        try {
            return exportEntity(getTenantId(), entityId, toExportSettings(exportSettingsParams));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping(value = "/export/{entityType}", params = "ids")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByIds(@PathVariable EntityType entityType,
                                                                                  @RequestParam UUID[] ids,
                                                                                  @RequestParam Map<String, String> exportSettingsParams) throws ThingsboardException {
        List<EntityId> entitiesIds = Arrays.stream(ids)
                .map(id -> EntityIdFactory.getByTypeAndUuid(entityType, id))
                .collect(Collectors.toList());
        try {
            return exportEntitiesByIds(getTenantId(), entitiesIds, toExportSettings(exportSettingsParams));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping(value = "/export/{entityType}")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByEntityType(@PathVariable EntityType entityType,
                                                                                         @RequestParam Map<String, String> exportSettingsParams,
                                                                                         @RequestParam(defaultValue = "0") int page,
                                                                                         @RequestParam(defaultValue = "2147483647") int pageSize,
                                                                                         @RequestParam(name = "customerId", required = false) UUID customerUuid) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        CustomerId customerId = toCustomerId(customerUuid);

        EntityTypeFilter entityTypeFilter = new EntityTypeFilter();
        entityTypeFilter.setEntityType(entityType);
        try {
            return exportEntitiesByFilter(tenantId, customerId, entityTypeFilter, page, pageSize, toExportSettings(exportSettingsParams));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping("/exportByFilter")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByFilter(@RequestBody EntityFilter filter,
                                                                                     @RequestParam Map<String, String> exportSettingsParams,
                                                                                     @RequestParam(defaultValue = "0") int page,
                                                                                     @RequestParam(defaultValue = "2147483647") int pageSize,
                                                                                     @RequestParam(name = "customerId", required = false) UUID customerUuid) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        CustomerId customerId = toCustomerId(customerUuid);
        try {
            return exportEntitiesByFilter(tenantId, customerId, filter, page, pageSize, toExportSettings(exportSettingsParams));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    // FIXME: too aggressive
    @PostMapping("/exportByFilters")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportAllEntitiesByFilters(@RequestBody List<EntityFilter> filters,
                                                                                         @RequestParam Map<String, String> exportSettingsParams,
                                                                                         @RequestParam(name = "customerId", required = false) UUID customerUuid) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        CustomerId customerId = toCustomerId(customerUuid);
        try {
            List<EntityExportData<ExportableEntity<EntityId>>> exportDataList = new ArrayList<>();
            for (EntityFilter filter : filters) {
                exportDataList.addAll(exportEntitiesByFilter(tenantId, customerId, filter, 0, Integer.MAX_VALUE, toExportSettings(exportSettingsParams)));
            }
            return exportDataList;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping("/exportByQuery")
    public List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByQuery(@RequestBody EntityDataQuery entitiesQuery,
                                                                                    @RequestParam Map<String, String> exportSettingsParams,
                                                                                    @RequestParam(name = "customerId", required = false) UUID customerUuid) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        CustomerId customerId = toCustomerId(customerUuid);
        try {
            return exportEntitiesByQuery(tenantId, customerId, entitiesQuery, toExportSettings(exportSettingsParams));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    private List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByFilter(TenantId tenantId, CustomerId customerId, EntityFilter filter, int page, int pageSize, EntityExportSettings exportSettings) throws ThingsboardException {
        EntityDataPageLink pageLink = new EntityDataPageLink();
        pageLink.setPage(page);
        pageLink.setPageSize(pageSize);
        EntityKey sortProperty = new EntityKey(EntityKeyType.ENTITY_FIELD, CREATED_TIME);
        pageLink.setSortOrder(new EntityDataSortOrder(sortProperty, EntityDataSortOrder.Direction.DESC));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, List.of(sortProperty), Collections.emptyList(), Collections.emptyList());
        return exportEntitiesByQuery(tenantId, customerId, query, exportSettings);
    }

    private List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query, EntityExportSettings exportSettings) throws ThingsboardException {
        List<EntityId> entitiesIds = entityService.findEntityDataByQuery(tenantId, customerId, query).getData().stream()
                .map(EntityData::getEntityId)
                .collect(Collectors.toList());
        return exportEntitiesByIds(tenantId, entitiesIds, exportSettings);
    }

    private List<EntityExportData<ExportableEntity<EntityId>>> exportEntitiesByIds(TenantId tenantId, List<EntityId> entitiesIds, EntityExportSettings exportSettings) throws ThingsboardException {
        List<EntityExportData<ExportableEntity<EntityId>>> exportDataList = new ArrayList<>();
        for (EntityId entityId : entitiesIds) {
            exportDataList.add(exportEntity(tenantId, entityId, exportSettings));
        }
        return exportDataList;
    }

    private <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(TenantId tenantId, I entityId, EntityExportSettings exportSettings) throws ThingsboardException {
        checkEntityId(entityId, Operation.READ);

        List<EntityRelation> relations = new LinkedList<>();
        if (exportSettings.isExportInboundRelations()) {
            relations.addAll(relationService.findByTo(tenantId, entityId, RelationTypeGroup.COMMON));
        }
        if (exportSettings.isExportOutboundRelations()) {
            relations.addAll(relationService.findByFrom(tenantId, entityId, RelationTypeGroup.COMMON));
        }
        for (EntityRelation relation : relations) {
            if (!relation.getFrom().equals(entityId)) {
                checkEntityId(relation.getFrom(), Operation.READ);
            } else if (!relation.getTo().equals(entityId)) {
                checkEntityId(relation.getTo(), Operation.READ);
            }
        }

        return exportImportService.exportEntity(tenantId, entityId, exportSettings);
    }


    @PostMapping("/import")
    public List<EntityImportResult<ExportableEntity<EntityId>>> importEntity(@RequestBody List<EntityExportData<ExportableEntity<EntityId>>> exportDataList,
                                                                             @RequestParam Map<String, String> importSettingsParams) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        EntityImportSettings importSettings = toImportSettings(importSettingsParams);

        try {
            return importEntities(user, exportDataList, importSettings)
                    .stream().peek(entityImportResult -> {
                        onEntityUpdatedOrCreated(user, entityImportResult.getSavedEntity(), entityImportResult.getOldEntity(), entityImportResult.getOldEntity() == null);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    public List<EntityImportResult<ExportableEntity<EntityId>>> importEntities(SecurityUser user, List<EntityExportData<ExportableEntity<EntityId>>> exportDataList, EntityImportSettings importSettings) throws ThingsboardException {
        for (EntityExportData<ExportableEntity<EntityId>> exportData : exportDataList) {
            ExportableEntity<EntityId> existingEntity = exportableEntitiesService.findEntityByExternalId(user.getTenantId(), exportData.getEntity().getId());
            if (existingEntity != null) {
                accessControlService.checkPermission(user, Resource.of(exportData.getEntityType()), Operation.WRITE, existingEntity.getId(), existingEntity);
            } else {
                exportData.getEntity().setTenantId(user.getTenantId());
                accessControlService.checkPermission(user, Resource.of(exportData.getEntityType()), Operation.CREATE, null, exportData.getEntity());
            }

            List<EntityRelation> relations = new LinkedList<>();
            if (importSettings.isImportInboundRelations() && exportData.getInboundRelations() != null) {
                relations.addAll(exportData.getInboundRelations());
            }
            if (importSettings.isImportOutboundRelations() && exportData.getOutboundRelations() != null) {
                relations.addAll(exportData.getOutboundRelations());
            }
            for (EntityRelation relation : relations) {
                EntityId otherEntityId = null;
                if (!relation.getFrom().equals(exportData.getEntity().getId())) {
                    otherEntityId = relation.getFrom();
                } else if (!relation.getTo().equals(exportData.getEntity().getId())) {
                    otherEntityId = relation.getTo();
                }
                if (otherEntityId != null) {
                    ExportableEntity<EntityId> otherEntity = exportableEntitiesService.findEntityByExternalId(user.getTenantId(), otherEntityId);
                    if (otherEntity != null) {
                        checkEntityId(otherEntity.getId(), Operation.WRITE);
                    } else {
                        throw new IllegalArgumentException("Relation is referencing non-existing entity");
                    }
                }
            }
        }

        return exportImportService.importEntities(user.getTenantId(), exportDataList, importSettings);
    }


    private EntityExportSettings toExportSettings(Map<String, String> exportSettingsParams) {
        return EntityExportSettings.builder()
                .exportInboundRelations(getParam(exportSettingsParams, "exportInboundRelations", false, Boolean::parseBoolean))
                .exportOutboundRelations(getParam(exportSettingsParams, "exportOutboundRelations", false, Boolean::parseBoolean))
                .build();
    }

    private EntityImportSettings toImportSettings(Map<String, String> importSettingsParams) {
        return EntityImportSettings.builder()
                .importInboundRelations(getParam(importSettingsParams, "importInboundRelations", false, Boolean::parseBoolean))
                .importOutboundRelations(getParam(importSettingsParams, "importOutboundRelations", false, Boolean::parseBoolean))
                .removeExistingRelations(getParam(importSettingsParams, "removeExistingRelations", true, Boolean::parseBoolean))
                .updateReferencesToOtherEntities(getParam(importSettingsParams, "updateReferencesToOtherEntities", true, Boolean::parseBoolean))
                .build();
    }

    protected <T> T getParam(Map<String, String> requestParams, String key, T defaultValue, Function<String, T> parsingFunction) {
        return parsingFunction.apply(requestParams.getOrDefault(key, defaultValue.toString()));
    }

    private CustomerId toCustomerId(UUID customerUuid) {
        return new CustomerId(Optional.ofNullable(customerUuid).orElse(EntityId.NULL_UUID));
    }

}
