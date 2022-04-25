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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.EntitiesExportImportService;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.request.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.data.request.ExportRequest;
import org.thingsboard.server.service.sync.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.importing.data.EntityImportSettings;
import org.thingsboard.server.service.sync.importing.data.request.ImportRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/entities")
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class EntitiesExportImportController extends BaseController {

    private final EntitiesExportImportService exportImportService;
    private final ExportableEntitiesService exportableEntitiesService;


    @PostMapping("/export")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<?>> exportEntities(@RequestBody ExportRequest exportRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            return exportEntitiesByRequest(user, exportRequest);
        } catch (Exception e) {
            log.warn("Failed to export entities for request {}", exportRequest, e);
            throw handleException(e);
        }
    }

    @PostMapping(value = "/export", params = {"multiple"})
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<?>> exportEntities(@RequestBody List<ExportRequest> exportRequests) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            List<EntityExportData<?>> result = new ArrayList<>();
            for (ExportRequest exportRequest : exportRequests) {
                List<EntityExportData<?>> exportDataList = exportEntitiesByRequest(user, exportRequest);
                result.addAll(exportDataList);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to export entities for requests {}", exportRequests, e);
            throw handleException(e);
        }
    }

    private List<EntityExportData<?>> exportEntitiesByRequest(SecurityUser user, ExportRequest exportRequest) throws ThingsboardException {
        List<EntityId> entities = exportableEntitiesService.findEntitiesForRequest(user.getTenantId(), exportRequest);

        EntityExportSettings exportSettings = exportRequest.getExportSettings();
        if (exportSettings == null) {
            exportSettings = EntityExportSettings.builder()
                    .exportRelations(false)
                    .build();
        }

        List<EntityExportData<?>> exportDataList = new ArrayList<>();
        for (EntityId entityId : entities) {
            EntityExportData<?> exportData = exportImportService.exportEntity(user, entityId, exportSettings);
            exportDataList.add(exportData);
        }
        return exportDataList;
    }


    @PostMapping("/import")
    public List<EntityImportResult<?>> importEntities(@RequestBody ImportRequest importRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            EntityImportSettings importSettings = importRequest.getImportSettings();
            if (importSettings == null) {
                importSettings = EntityImportSettings.builder()
                        .findExistingByName(false)
                        .updateRelations(false)
                        .build();
            }

            List<EntityImportResult<?>> importResults = exportImportService.importEntities(user, importRequest.getExportDataList(), importSettings);

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
            log.warn("Failed to import entities for request {}", importRequest, e);
            throw handleException(e);
        }
    }

}
