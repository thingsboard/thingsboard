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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.EntitiesExportImportService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.request.CustomEntityFilterExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.CustomEntityQueryExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityListExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityTypeExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.ExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.SingleEntityExportRequest;
import org.thingsboard.server.service.sync.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.importing.data.request.ImportRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CREATED_TIME;

@RestController
@RequestMapping("/api/entities")
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class EntitiesExportImportController extends BaseController {

    private final EntitiesExportImportService exportImportService;
    private final EntityService entityService;

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<?>> exportEntities(@RequestBody ExportRequest exportRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            return exportEntitiesByRequest(user, exportRequest);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping(value = "/export", params = {"multiple"})
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<?>> exportEntities(@RequestBody List<ExportRequest> exportRequests) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            List<EntityExportData<?>> exportDataList = new ArrayList<>();
            for (ExportRequest exportRequest : exportRequests) {
                exportDataList.addAll(exportEntitiesByRequest(user, exportRequest));
            }
            return exportDataList;
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    private List<EntityExportData<?>> exportEntitiesByRequest(SecurityUser user, ExportRequest request) throws ThingsboardException {
        List<EntityId> entitiesIds = findEntitiesForRequest(user, request);

        List<EntityExportData<?>> exportDataList = new ArrayList<>();
        for (EntityId entityId : entitiesIds) {
            exportDataList.add(exportImportService.exportEntity(user, entityId, request.getExportSettings()));
        }
        return exportDataList;
    }

    private List<EntityId> findEntitiesForRequest(SecurityUser user, ExportRequest request) {
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

                CustomerId customerId = Optional.ofNullable(exportRequest.getCustomerId()).orElse(emptyId(EntityType.CUSTOMER));
                return findEntitiesByFilter(user.getTenantId(), customerId, entityTypeFilter, exportRequest.getPage(), exportRequest.getPageSize());
            }
            case CUSTOM_ENTITY_FILTER: {
                CustomEntityFilterExportRequest exportRequest = (CustomEntityFilterExportRequest) request;
                EntityFilter filter = exportRequest.getFilter();

                CustomerId customerId = Optional.ofNullable(exportRequest.getCustomerId()).orElse(emptyId(EntityType.CUSTOMER));
                return findEntitiesByFilter(user.getTenantId(), customerId, filter, exportRequest.getPage(), exportRequest.getPageSize());
            }
            case CUSTOM_ENTITY_QUERY: {
                CustomEntityQueryExportRequest exportRequest = (CustomEntityQueryExportRequest) request;
                EntityDataQuery query = exportRequest.getQuery();

                CustomerId customerId = Optional.ofNullable(exportRequest.getCustomerId()).orElse(emptyId(EntityType.CUSTOMER));
                return findEntitiesByQuery(user.getTenantId(), customerId, query);
            }
            default:
                throw new IllegalArgumentException("Export request is not supported");
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


    @PostMapping("/import")
    public List<EntityImportResult<?>> importEntities(@RequestBody ImportRequest importRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            List<EntityImportResult<?>> importResults = exportImportService.importEntities(user, importRequest.getExportDataList(), importRequest.getImportSettings());

            importResults.stream()
                    .map(EntityImportResult::getSendEventsCallback)
                    .filter(Objects::nonNull)
                    .forEach(sendEventsCallback -> {
                        try {
                            sendEventsCallback.run();
                        } catch (Exception e) {
                            log.error("Failed to send event for entity", e);
                        }
                    });

            return importResults;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
