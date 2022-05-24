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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.*;

@RestController
@TbCoreComponent
@RequestMapping("/api/entities/vc")
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
@RequiredArgsConstructor
public class EntitiesVersionControlController extends BaseController {

    private final EntitiesVersionControlService versionControlService;


    @ApiOperation(value = "", notes = "" +
            "SINGLE_ENTITY:" + NEW_LINE +
            "```\n{\n" +
            "  \"type\": \"SINGLE_ENTITY\",\n" +
            "\n" +
            "  \"versionName\": \"Version 1.0\",\n" +
            "  \"branch\": \"dev\",\n" +
            "\n" +
            "  \"entityId\": {\n" +
            "    \"entityType\": \"DEVICE\",\n" +
            "    \"id\": \"b79448e0-d4f4-11ec-847b-0f432358ab48\"\n" +
            "  },\n" +
            "  \"config\": {\n" +
            "    \"saveRelations\": true\n" +
            "  }\n" +
            "}\n```" + NEW_LINE +
            "COMPLEX:" + NEW_LINE +
            "```\n{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "\n" +
            "  \"versionName\": \"Devices and profiles: release 2\",\n" +
            "  \"branch\": \"master\",\n" +
            "\n" +
            "  \"syncStrategy\": \"OVERWRITE\",\n" +
            "  \"entityTypes\": {\n" +
            "    \"DEVICE\": {\n" +
            "      \"syncStrategy\": null,\n" +
            "      \"allEntities\": true,\n" +
            "      \"saveRelations\": true\n" +
            "    },\n" +
            "    \"DEVICE_PROFILE\": {\n" +
            "      \"syncStrategy\": \"MERGE\",\n" +
            "      \"allEntities\": false,\n" +
            "      \"entityIds\": [\n" +
            "        \"b79448e0-d4f4-11ec-847b-0f432358ab48\"\n" +
            "      ],\n" +
            "      \"saveRelations\": true\n" +
            "    }\n" +
            "  }\n" +
            "}\n```")
    @PostMapping("/version")
    public VersionCreationResult saveEntitiesVersion(@RequestBody VersionCreateRequest request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            return versionControlService.saveEntitiesVersion(user, request);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "", notes = "" +
            "```\n[\n" +
            "  {\n" +
            "    \"id\": \"c30c8bcaed3f0813649f0dee51a89d04d0a12b28\",\n" +
            "    \"name\": \"Device profile 1 version 1.0\"\n" +
            "  }\n" +
            "]\n```")
    @GetMapping(value = "/version/{branch}/{entityType}/{externalEntityUuid}", params = {"pageSize", "page"})
    public PageData<EntityVersion> listEntityVersions(@PathVariable String branch,
                                                      @PathVariable EntityType entityType,
                                                      @PathVariable UUID externalEntityUuid,
                                                      @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                      @RequestParam int pageSize,
                                                      @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                      @RequestParam int page) throws ThingsboardException {
        try {
            EntityId externalEntityId = EntityIdFactory.getByTypeAndUuid(entityType, externalEntityUuid);
            PageLink pageLink = new PageLink(pageSize, page);
            return versionControlService.listEntityVersions(getTenantId(), branch, externalEntityId, pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "", notes = "" +
            "```\n[\n" +
            "  {\n" +
            "    \"id\": \"c30c8bcaed3f0813649f0dee51a89d04d0a12b28\",\n" +
            "    \"name\": \"Device profiles from dev\"\n" +
            "  }\n" +
            "]\n```")
    @GetMapping(value = "/version/{branch}/{entityType}", params = {"pageSize", "page"})
    public PageData<EntityVersion> listEntityTypeVersions(@PathVariable String branch,
                                                          @PathVariable EntityType entityType,
                                                          @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                          @RequestParam int pageSize,
                                                          @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                          @RequestParam int page) throws ThingsboardException {
        try {
            PageLink pageLink = new PageLink(pageSize, page);
            return versionControlService.listEntityTypeVersions(getTenantId(), branch, entityType, pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "", notes = "" +
            "```\n[\n" +
            "  {\n" +
            "    \"id\": \"ba9baaca1742b730e7331f31a6a51da5fc7da7f7\",\n" +
            "    \"name\": \"Device 1 removed\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"id\": \"b3c28d722d328324c7c15b0b30047b0c40011cf7\",\n" +
            "    \"name\": \"Device profiles added\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"id\": \"c30c8bcaed3f0813649f0dee51a89d04d0a12b28\",\n" +
            "    \"name\": \"Devices added\"\n" +
            "  }\n" +
            "]\n```")
    @GetMapping(value = "/version/{branch}", params = {"pageSize", "page"})
    public PageData<EntityVersion> listVersions(@PathVariable String branch,
                                                @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                @RequestParam int pageSize,
                                                @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                @RequestParam int page) throws ThingsboardException {
        try {
            PageLink pageLink = new PageLink(pageSize, page);
            return versionControlService.listVersions(getTenantId(), branch, pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @GetMapping("/entity/{branch}/{entityType}/{versionId}")
    public List<VersionedEntityInfo> listEntitiesAtVersion(@PathVariable String branch,
                                                           @PathVariable EntityType entityType,
                                                           @PathVariable String versionId) throws ThingsboardException {
        try {
            return versionControlService.listEntitiesAtVersion(getTenantId(), branch, versionId, entityType);
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


    @ApiOperation(value = "", notes = "" +
            "SINGLE_ENTITY:" + NEW_LINE +
            "```\n{\n" +
            "  \"type\": \"SINGLE_ENTITY\",\n" +
            "  \n" +
            "  \"branch\": \"dev\",\n" +
            "  \"versionId\": \"b3c28d722d328324c7c15b0b30047b0c40011cf7\",\n" +
            "  \n" +
            "  \"externalEntityId\": {\n" +
            "    \"entityType\": \"DEVICE\",\n" +
            "    \"id\": \"b7944123-d4f4-11ec-847b-0f432358ab48\"\n" +
            "  },\n" +
            "  \"config\": {\n" +
            "    \"loadRelations\": false,\n" +
            "    \"findExistingEntityByName\": false\n" +
            "  }\n" +
            "}\n```" + NEW_LINE +
            "ENTITY_TYPE:" + NEW_LINE +
            "```\n{\n" +
            "  \"type\": \"ENTITY_TYPE\",\n" +
            "\n" +
            "  \"branch\": \"dev\",\n" +
            "  \"versionId\": \"b3c28d722d328324c7c15b0b30047b0c40011cf7\",\n" +
            "\n" +
            "  \"entityTypes\": {\n" +
            "    \"DEVICE\": {\n" +
            "      \"loadRelations\": false,\n" +
            "      \"findExistingEntityByName\": false,\n" +
            "      \"removeOtherEntities\": true\n" +
            "    }\n" +
            "  }\n" +
            "}\n```")
    @PostMapping("/entity")
    public List<VersionLoadResult> loadEntitiesVersion(@RequestBody VersionLoadRequest request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            String versionId = request.getVersionId();
            if (versionId == null) {
                PageData<EntityVersion> versions = versionControlService.listVersions(user.getTenantId(), request.getBranch(), new PageLink(1));
                if (versions.getData().size() > 0) {
                    request.setVersionId(versions.getData().get(0).getId());
                } else {
                    throw new IllegalArgumentException("No versions available in branch");
                }
            }

            return versionControlService.loadEntitiesVersion(user, request);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "", notes = "" +
            "```\n[\n" +
            "  {\n" +
            "    \"name\": \"master\",\n" +
            "    \"default\": true\n" +
            "  },\n" +
            "  {\n" +
            "    \"name\": \"dev\",\n" +
            "    \"default\": false\n" +
            "  },\n" +
            "  {\n" +
            "    \"name\": \"dev-2\",\n" +
            "    \"default\": false\n" +
            "  }\n" +
            "]\n\n```")
    @GetMapping("/branches")
    public List<BranchInfo> listBranches() throws ThingsboardException {
        try {
            List<String> remoteBranches = versionControlService.listBranches(getTenantId());
            List<BranchInfo> infos = new ArrayList<>();

            String defaultBranch = versionControlService.getVersionControlSettings(getTenantId()).getDefaultBranch();
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

    @Data
    public static class BranchInfo {
        private final String name;
        private final boolean isDefault;
    }

}
