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

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.sync.vc.data.EntitiesVersionControlSettings;
import org.thingsboard.server.service.sync.vc.data.EntityVersion;
import org.thingsboard.server.service.sync.vc.data.VersionCreationResult;
import org.thingsboard.server.service.sync.vc.data.VersionLoadResult;
import org.thingsboard.server.service.sync.vc.data.VersionedEntityInfo;
import org.thingsboard.server.service.sync.vc.data.request.create.VersionCreateRequest;
import org.thingsboard.server.service.sync.vc.data.request.load.VersionLoadRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/entities/vc")
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
@RequiredArgsConstructor
public class EntitiesVersionControlController extends BaseController {

    private final EntitiesVersionControlService versionControlService;


    @PostMapping("/version")
    public VersionCreationResult saveEntitiesVersion(@RequestBody VersionCreateRequest request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            return versionControlService.saveEntitiesVersion(user, request);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @GetMapping("/version/{branch}/{entityType}/{externalEntityUuid}")
    public List<EntityVersion> listEntityVersions(@PathVariable String branch,
                                                  @PathVariable EntityType entityType,
                                                  @PathVariable UUID externalEntityUuid) throws ThingsboardException {
        try {
            EntityId externalEntityId = EntityIdFactory.getByTypeAndUuid(entityType, externalEntityUuid);
            return versionControlService.listEntityVersions(getTenantId(), branch, externalEntityId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @GetMapping("/version/{branch}/{entityType}")
    public List<EntityVersion> listEntityTypeVersions(@PathVariable String branch,
                                                      @PathVariable EntityType entityType) throws ThingsboardException {
        try {
            return versionControlService.listEntityTypeVersions(getTenantId(), branch, entityType);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @GetMapping("/version/{branch}")
    public List<EntityVersion> listVersions(@PathVariable String branch) throws ThingsboardException {
        try {
            return versionControlService.listVersions(getTenantId(), branch);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @GetMapping("/entity/{branch}/{entityType}/{versionId}")
    public List<VersionedEntityInfo> listEntitiesAtVersion(@PathVariable String branch,
                                                           @PathVariable EntityType entityType,
                                                           @PathVariable String versionId) throws ThingsboardException {
        try {
            return versionControlService.listEntitiesAtVersion(getTenantId(), entityType, branch, versionId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @GetMapping("/entity/{branch}/{versionId}")
    public List<VersionedEntityInfo> listAllEntitiesAtVersion(@PathVariable String branch,
                                                              @PathVariable String versionId) throws ThingsboardException {
        try {
            return versionControlService.listAllEntitiesAtVersion(getTenantId(), branch, versionId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PostMapping("/entity")
    public List<VersionLoadResult> loadEntitiesVersion(@RequestBody VersionLoadRequest request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            String versionId = request.getVersionId();
            if (versionId == null) {
                List<EntityVersion> versions = versionControlService.listVersions(user.getTenantId(), request.getBranch());
                if (versions.size() > 0) {
                    versionId = versions.get(0).getId();
                } else {
                    throw new IllegalArgumentException("No versions available in branch");
                }
            }

            return versionControlService.loadEntitiesVersion(user, request);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiModelProperty(notes = "" +
            "")
    @GetMapping("/branches")
    public List<BranchInfo> listBranches() throws ThingsboardException {
        try {
            List<String> remoteBranches = versionControlService.listBranches(getTenantId());
            List<BranchInfo> infos = new ArrayList<>();

            String defaultBranch = getSettings().getDefaultBranch();
            if (StringUtils.isNotEmpty(defaultBranch)) {
                remoteBranches.remove(defaultBranch);
                infos.add(new BranchInfo(defaultBranch, true));
            }

            remoteBranches.forEach(branch -> infos.add(new BranchInfo(branch, false)));
            return infos;
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiModelProperty(notes = "" +
            "```\n{\n" +
            "  \"repositoryUri\": \"https://github.com/User/repo.git\",\n" +
            "  \"username\": \"User\",\n" +
            "  \"password\": \"api_key\",\n" +
            "  \"defaultBranch\": \"master\"\n" +
            "}\n```")
    @GetMapping("/settings")
    public EntitiesVersionControlSettings getSettings() throws ThingsboardException {
        try {
            return versionControlService.getSettings(getTenantId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiModelProperty(notes = "" +
            "```\n{\n" +
            "  \"repositoryUri\": \"https://github.com/User/repo.git\",\n" +
            "  \"username\": \"User\",\n" +
            "  \"password\": \"api_key\",\n" +
            "  \"defaultBranch\": \"master\"\n" +
            "}\n```")
    @PostMapping("/settings")
    public void saveSettings(@RequestBody EntitiesVersionControlSettings settings) throws ThingsboardException {
        try {
            versionControlService.saveSettings(getTenantId(), settings);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @Data
    public static class BranchInfo {
        private final String name;
        private final boolean isDefault;
    }

}
