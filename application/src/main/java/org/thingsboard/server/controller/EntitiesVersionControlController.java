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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;

@RestController
@RequestMapping("/api/entities/vc")
@RequiredArgsConstructor
public class EntitiesVersionControlController extends BaseController {

    private final EntitiesVersionControlService versionControlService;


    // search request - export request with settings

//
//    @PostMapping("/version/{entityType}/{entityId}")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public EntityVersion saveEntityVersion(@PathVariable EntityType entityType,
//                                           @PathVariable("entityId") UUID id,
//                                           @RequestParam String branch,
//                                           @RequestBody String versionName) throws Exception {
//        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, id);
//        return versionControlService.saveEntityVersion(getTenantId(), entityId, branch, versionName);
//    }
//
//    @PostMapping("/version/{entityType}")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public EntityVersion saveEntitiesVersion(@PathVariable EntityType entityType,
//                                             @RequestParam UUID[] ids,
//                                             @RequestParam String branch,
//                                             @RequestBody String versionName) throws Exception {
//        List<EntityId> entitiesIds = Arrays.stream(ids)
//                .map(id -> EntityIdFactory.getByTypeAndUuid(entityType, id))
//                .collect(Collectors.toList());
//        return versionControlService.saveEntitiesVersion(getTenantId(), entitiesIds, branch, versionName);
//    }
//
//
//
//    @GetMapping("/version/{entityType}/{entityId}")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public List<EntityVersion> listEntityVersions(@PathVariable EntityType entityType,
//                                                  @PathVariable("entityId") UUID entityUuid,
//                                                  @RequestParam String branch) throws Exception {
//        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
//        return versionControlService.listEntityVersions(getTenantId(), entityId, branch, Integer.MAX_VALUE);
//    }
//
//    @GetMapping("/version/{entityType}")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public List<EntityVersion> listEntityTypeVersions(@PathVariable EntityType entityType,
//                                                      @RequestParam String branch) throws Exception {
//        return versionControlService.listEntityTypeVersions(getTenantId(), entityType, branch, Integer.MAX_VALUE);
//    }
//
//    @GetMapping("/version")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public List<EntityVersion> listVersions(@RequestParam String branch) throws Exception {
//        return versionControlService.listVersions(getTenantId(), branch, Integer.MAX_VALUE);
//    }
//
//
//
//    @GetMapping("/files/version/{versionId}")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public List<String> listFilesAtVersion(@RequestParam String branch,
//                                           @PathVariable String versionId) throws Exception {
//        return versionControlService.listFilesAtVersion(getTenantId(), branch, versionId);
//    }
//
//
//
//    @GetMapping("/entity/{entityType}/{entityId}/{versionId}")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public EntityExportData<ExportableEntity<EntityId>> getEntityAtVersion(@PathVariable EntityType entityType,
//                                                                           @PathVariable("entityId") UUID entityUuid,
//                                                                           @RequestParam String branch,
//                                                                           @PathVariable String versionId) throws Exception {
//        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
//        return versionControlService.getEntityAtVersion(getTenantId(), entityId, branch, versionId);
//    }
//
//    @PostMapping("/entity/{entityType}/{entityId}/{versionId}")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public EntityImportResult<ExportableEntity<EntityId>> loadEntityVersion(@PathVariable EntityType entityType,
//                                                                            @PathVariable("entityId") UUID entityUuid,
//                                                                            @RequestParam String branch,
//                                                                            @PathVariable String versionId) throws Exception {
//        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
//        EntityImportResult<ExportableEntity<EntityId>> result = versionControlService.loadEntityVersion(getTenantId(), entityId, branch, versionId);
//        onEntityUpdatedOrCreated(getCurrentUser(), result.getSavedEntity(), result.getOldEntity(), result.getOldEntity() == null);
//        return result;
//    }
//
//    @PostMapping("/entity/{versionId}")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public List<EntityImportResult<ExportableEntity<EntityId>>> loadAllAtVersion(@RequestParam String branch,
//                                                                                 @PathVariable String versionId) throws Exception {
//        SecurityUser user = getCurrentUser();
//        List<EntityImportResult<ExportableEntity<EntityId>>> resultList = versionControlService.loadAllAtVersion(user.getTenantId(), branch, versionId);
//        resultList.forEach(result -> {
//            onEntityUpdatedOrCreated(user, result.getSavedEntity(), result.getOldEntity(), result.getOldEntity() == null);
//        });
//        return resultList;
//    }
//
//
//
//    @GetMapping("/branches")
//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    public Set<String> getAllowedBranches() throws ThingsboardException {
//        return versionControlService.getAllowedBranches(getTenantId());
//    }
//
//
//    @PostMapping("/settings")
//    @PreAuthorize("hasAuthority('SYS_ADMIN')")
//    public void saveSettings(@RequestBody EntitiesVersionControlSettings settings) throws Exception {
//        versionControlService.saveSettings(settings);
//    }
//
//    @GetMapping("/settings")
//    @PreAuthorize("hasAuthority('SYS_ADMIN')")
//    public EntitiesVersionControlSettings getSettings() {
//        return versionControlService.getSettings();
//    }
//
//
//
//    @PostMapping("/repository/reset")
//    @PreAuthorize("hasAuthority('SYS_ADMIN')")
//    public void resetLocalRepository() throws Exception {
//        versionControlService.resetRepository();
//    }

}
