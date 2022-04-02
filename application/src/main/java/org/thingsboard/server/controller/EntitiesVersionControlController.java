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
import org.springframework.web.bind.annotation.GetMapping;
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
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.importing.EntityImportResult;
import org.thingsboard.server.service.sync.vcs.DefaultEntitiesVersionControlService;
import org.thingsboard.server.service.sync.vcs.data.EntitiesVersionControlSettings;
import org.thingsboard.server.service.sync.vcs.data.EntityVersion;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/entities/vc")
@RequiredArgsConstructor
public class EntitiesVersionControlController extends BaseController {

    private final DefaultEntitiesVersionControlService versionControlService;



    @PostMapping("/version/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public EntityVersion saveEntityVersion(@PathVariable EntityType entityType,
                                           @PathVariable("entityId") UUID entityUuid,
                                           @RequestParam String branch,
                                           @RequestBody String versionName) throws Exception {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
        return versionControlService.saveEntityVersion(getTenantId(), entityId, branch, versionName);
    }

    @GetMapping("/version/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityVersion> listEntityVersions(@PathVariable EntityType entityType,
                                                  @PathVariable("entityId") UUID entityUuid,
                                                  @RequestParam String branch) throws Exception {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
        return versionControlService.listEntityVersions(getTenantId(), entityId, branch, Integer.MAX_VALUE);
    }



    @GetMapping("/entity/{entityType}/{entityId}/{versionId}")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public EntityExportData<ExportableEntity<EntityId>> getEntityAtVersion(@PathVariable EntityType entityType,
                                                                           @PathVariable("entityId") UUID entityUuid,
                                                                           @PathVariable String versionId) throws Exception {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
        return versionControlService.getEntityAtVersion(getTenantId(), entityId, versionId);
    }

    @PostMapping("/entity/{entityType}/{entityId}/{versionId}")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public EntityImportResult<ExportableEntity<EntityId>> loadEntityVersion(@PathVariable EntityType entityType,
                                                                            @PathVariable("entityId") UUID entityUuid,
                                                                            @PathVariable String versionId) throws Exception {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
        return versionControlService.loadEntityVersion(getTenantId(), entityId, versionId);
    }



    @GetMapping("/branches")
    public Set<String> getAllowedBranches() throws ThingsboardException {
        return versionControlService.getAllowedBranches(getTenantId());
    }


    @PostMapping("/settings")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public void saveSettings(@RequestBody EntitiesVersionControlSettings settings) throws Exception {
        versionControlService.saveSettings(settings);
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public EntitiesVersionControlSettings getSettings() {
        return versionControlService.getSettings();
    }



    @PostMapping("/repository/reset")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public void resetLocalRepository() throws Exception {
        versionControlService.resetRepository();
    }

}
