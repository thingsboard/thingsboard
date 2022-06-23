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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityDataDiff;
import org.thingsboard.server.common.data.sync.vc.EntityDataInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.VC_REQUEST_ID_PARAM_DESCRIPTION;

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
    public DeferredResult<UUID> saveEntitiesVersion(@RequestBody VersionCreateRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        return wrapFuture(versionControlService.saveEntitiesVersion(user, request));
    }

    @ApiOperation(value = "", notes = "")
    @GetMapping(value = "/version/{requestId}/status")
    public VersionCreationResult getVersionCreateRequestStatus(@ApiParam(value = VC_REQUEST_ID_PARAM_DESCRIPTION, required = true)
                                                               @PathVariable UUID requestId) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return versionControlService.getVersionCreateStatus(getCurrentUser(), requestId);
    }

    @ApiOperation(value = "", notes = "" +
            "```\n[\n" +
            "  {\n" +
            "    \"id\": \"c30c8bcaed3f0813649f0dee51a89d04d0a12b28\",\n" +
            "    \"name\": \"Device profile 1 version 1.0\"\n" +
            "  }\n" +
            "]\n```")
    @GetMapping(value = "/version/{entityType}/{externalEntityUuid}", params = {"branch", "pageSize", "page"})
    public DeferredResult<PageData<EntityVersion>> listEntityVersions(@PathVariable EntityType entityType,
                                                                      @PathVariable UUID externalEntityUuid,
                                                                      @RequestParam String branch,
                                                                      @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                                      @RequestParam int pageSize,
                                                                      @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                      @RequestParam int page,
                                                                      @ApiParam(value = ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION)
                                                                      @RequestParam(required = false) String textSearch,
                                                                      @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = "timestamp")
                                                                      @RequestParam(required = false) String sortProperty,
                                                                      @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                                      @RequestParam(required = false) String sortOrder) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        EntityId externalEntityId = EntityIdFactory.getByTypeAndUuid(entityType, externalEntityUuid);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return wrapFuture(versionControlService.listEntityVersions(getTenantId(), branch, externalEntityId, pageLink));
    }

    @ApiOperation(value = "", notes = "" +
            "```\n[\n" +
            "  {\n" +
            "    \"id\": \"c30c8bcaed3f0813649f0dee51a89d04d0a12b28\",\n" +
            "    \"name\": \"Device profiles from dev\"\n" +
            "  }\n" +
            "]\n```")
    @GetMapping(value = "/version/{entityType}", params = {"branch", "pageSize", "page"})
    public DeferredResult<PageData<EntityVersion>> listEntityTypeVersions(@PathVariable EntityType entityType,
                                                                          @RequestParam String branch,
                                                                          @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                                          @RequestParam int pageSize,
                                                                          @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                          @RequestParam int page,
                                                                          @ApiParam(value = ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION)
                                                                          @RequestParam(required = false) String textSearch,
                                                                          @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = "timestamp")
                                                                          @RequestParam(required = false) String sortProperty,
                                                                          @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                                          @RequestParam(required = false) String sortOrder) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return wrapFuture(versionControlService.listEntityTypeVersions(getTenantId(), branch, entityType, pageLink));
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
    @GetMapping(value = "/version", params = {"branch", "pageSize", "page"})
    public DeferredResult<PageData<EntityVersion>> listVersions(@RequestParam String branch,
                                                                @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                                @RequestParam int pageSize,
                                                                @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                @RequestParam int page,
                                                                @ApiParam(value = ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION)
                                                                @RequestParam(required = false) String textSearch,
                                                                @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = "timestamp")
                                                                @RequestParam(required = false) String sortProperty,
                                                                @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                                @RequestParam(required = false) String sortOrder) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return wrapFuture(versionControlService.listVersions(getTenantId(), branch, pageLink));
    }


    @GetMapping(value = "/entity/{entityType}/{versionId}", params = {"branch"})
    public DeferredResult<List<VersionedEntityInfo>> listEntitiesAtVersion(@PathVariable EntityType entityType,
                                                                           @PathVariable String versionId,
                                                                           @RequestParam String branch) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return wrapFuture(versionControlService.listEntitiesAtVersion(getTenantId(), branch, versionId, entityType));
    }

    @GetMapping(value = "/entity/{versionId}", params = {"branch"})
    public DeferredResult<List<VersionedEntityInfo>> listAllEntitiesAtVersion(@PathVariable String versionId,
                                                                              @RequestParam String branch) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return wrapFuture(versionControlService.listAllEntitiesAtVersion(getTenantId(), branch, versionId));
    }

    @GetMapping("/info/{versionId}/{entityType}/{externalEntityUuid}")
    public DeferredResult<EntityDataInfo> getEntityDataInfo(@PathVariable String versionId,
                                                            @PathVariable EntityType entityType,
                                                            @PathVariable UUID externalEntityUuid) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, externalEntityUuid);
        return wrapFuture(versionControlService.getEntityDataInfo(getCurrentUser(), entityId, versionId));
    }

    @GetMapping(value = "/diff/{entityType}/{internalEntityUuid}", params = {"branch", "versionId"})
    public DeferredResult<EntityDataDiff> compareEntityDataToVersion(@PathVariable EntityType entityType,
                                                                     @PathVariable UUID internalEntityUuid,
                                                                     @RequestParam String branch,
                                                                     @RequestParam String versionId) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, internalEntityUuid);
        return wrapFuture(versionControlService.compareEntityDataToVersion(getCurrentUser(), branch, entityId, versionId));
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
    public UUID loadEntitiesVersion(@RequestBody VersionLoadRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.VERSION_CONTROL, Operation.WRITE);
        return versionControlService.loadEntitiesVersion(user, request);
    }

    @ApiOperation(value = "", notes = "")
    @GetMapping(value = "/entity/{requestId}/status")
    public VersionLoadResult getVersionLoadRequestStatus(@ApiParam(value = VC_REQUEST_ID_PARAM_DESCRIPTION, required = true)
                                                               @PathVariable UUID requestId) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return versionControlService.getVersionLoadStatus(getCurrentUser(), requestId);
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
    public DeferredResult<List<BranchInfo>> listBranches() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            final TenantId tenantId = getTenantId();
            ListenableFuture<List<BranchInfo>> branches = versionControlService.listBranches(tenantId);
            return wrapFuture(Futures.transform(branches, remoteBranches -> {
                List<BranchInfo> infos = new ArrayList<>();
                BranchInfo defaultBranch;
                String defaultBranchName = versionControlService.getVersionControlSettings(tenantId).getDefaultBranch();
                if (StringUtils.isNotEmpty(defaultBranchName)) {
                    defaultBranch = new BranchInfo(defaultBranchName, true);
                } else {
                    defaultBranch = remoteBranches.stream().filter(BranchInfo::isDefault).findFirst().orElse(null);
                }
                if (defaultBranch != null) {
                    infos.add(defaultBranch);
                }
                infos.addAll(remoteBranches.stream().filter(b -> !b.equals(defaultBranch))
                        .map(b -> new BranchInfo(b.getName(), false)).collect(Collectors.toList()));
                return infos;
            }, MoreExecutors.directExecutor()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
