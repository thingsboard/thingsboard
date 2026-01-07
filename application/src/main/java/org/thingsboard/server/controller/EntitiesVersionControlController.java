/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.BRANCH_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.VC_REQUEST_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.VERSION_ID_PARAM_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api/entities/vc")
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
@RequiredArgsConstructor
public class EntitiesVersionControlController extends BaseController {

    private final EntitiesVersionControlService versionControlService;

    @Value("${queue.vc.request-timeout:180000}")
    private int vcRequestTimeout;

    @ApiOperation(value = "Save entities version (saveEntitiesVersion)", notes = "" +
            "Creates a new version of entities (or a single entity) by request.\n" +
            "Supported entity types: CUSTOMER, ASSET, RULE_CHAIN, DASHBOARD, DEVICE_PROFILE, DEVICE, ENTITY_VIEW, WIDGETS_BUNDLE." + NEW_LINE +
            "There are two available types of request: `SINGLE_ENTITY` and `COMPLEX`. " +
            "Each of them contains version name (`versionName`) and name of a branch (`branch`) to create version (commit) in. " +
            "If specified branch does not exists in a remote repo, then new empty branch will be created. " +
            "Request of the `SINGLE_ENTITY` type has id of an entity (`entityId`) and additional configuration (`config`) " +
            "which has following options: \n" +
            "- `saveRelations` - whether to add inbound and outbound relations of type COMMON to created entity version;\n" +
            "- `saveAttributes` - to save attributes of server scope (and also shared scope for devices);\n" +
            "- `saveCredentials` - when saving a version of a device, to add its credentials to the version." + NEW_LINE +
            "An example of a `SINGLE_ENTITY` version create request:\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
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
            "    \"saveRelations\": true,\n" +
            "    \"saveAttributes\": true,\n" +
            "    \"saveCredentials\": false\n" +
            "  }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "Second request type (`COMPLEX`), additionally to `branch` and `versionName`, contains following properties:\n" +
            "- `entityTypes` - a structure with entity types to export and configuration for each entity type; " +
            "   this configuration has all the options available for `SINGLE_ENTITY` and additionally has these ones: \n" +
            "     - `allEntities` and `entityIds` - if you want to save the version of all entities of the entity type " +
            "        then set `allEntities` param to true, otherwise set it to false and specify the list of specific entities (`entityIds`);\n" +
            "     - `syncStrategy` - synchronization strategy to use for this entity type: when set to `OVERWRITE` " +
            "        then the list of remote entities of this type will be overwritten by newly added entities. If set to " +
            "        `MERGE` - existing remote entities of this entity type will not be removed, new entities will just " +
            "        be added on top (or existing remote entities will be updated).\n" +
            "- `syncStrategy` - default synchronization strategy to use when it is not specified for an entity type." + NEW_LINE +
            "Example for this type of request:\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
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
            "      \"saveRelations\": true,\n" +
            "      \"saveAttributes\": true,\n" +
            "      \"saveCredentials\": true\n" +
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
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "Response wil contain generated request UUID, that can be then used to retrieve " +
            "status of operation via `getVersionCreateRequestStatus`.\n" +
            TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/version")
    public DeferredResult<UUID> saveEntitiesVersion(@RequestBody VersionCreateRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        return wrapFuture(versionControlService.saveEntitiesVersion(user, request));
    }

    @ApiOperation(value = "Get version create request status (getVersionCreateRequestStatus)", notes = "" +
            "Returns the status of previously made version create request. " + NEW_LINE +
            "This status contains following properties:\n" +
            "- `done` - whether request processing is finished;\n" +
            "- `version` - created version info: timestamp, version id (commit hash), commit name and commit author;\n" +
            "- `added` - count of items that were created in the remote repo;\n" +
            "- `modified` - modified items count;\n" +
            "- `removed` - removed items count;\n" +
            "- `error` - error message, if an error occurred while handling the request." + NEW_LINE +
            "An example of successful status:\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"done\": true,\n" +
            "  \"added\": 10,\n" +
            "  \"modified\": 2,\n" +
            "  \"removed\": 5,\n" +
            "  \"version\": {\n" +
            "    \"timestamp\": 1655198528000,\n" +
            "    \"id\":\"8a834dd389ed80e0759ba8ee338b3f1fd160a114\",\n" +
            "    \"name\": \"My devices v2.0\",\n" +
            "    \"author\": \"John Doe\"\n" +
            "  },\n" +
            "  \"error\": null\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping(value = "/version/{requestId}/status")
    public VersionCreationResult getVersionCreateRequestStatus(@Parameter(description = VC_REQUEST_ID_PARAM_DESCRIPTION, required = true)
                                                               @PathVariable UUID requestId) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        return versionControlService.getVersionCreateStatus(getCurrentUser(), requestId);
    }

    @ApiOperation(value = "List entity versions (listEntityVersions)", notes = "" +
            "Returns list of versions for a specific entity in a concrete branch. \n" +
            "You need to specify external id of an entity to list versions for. This is `externalId` property of an entity, " +
            "or otherwise if not set - simply id of this entity. \n" +
            "If specified branch does not exist - empty page data will be returned. " + NEW_LINE +
            "Each version info item has timestamp, id, name and author. Version id can then be used to restore the version. " +
            PAGE_DATA_PARAMETERS + NEW_LINE +
            "Response example: \n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"timestamp\": 1655198593000,\n" +
            "      \"id\": \"fd82625bdd7d6131cf8027b44ee967012ecaf990\",\n" +
            "      \"name\": \"Devices and assets - v2.0\",\n" +
            "      \"author\": \"John Doe <johndoe@gmail.com>\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"timestamp\": 1655198528000,\n" +
            "      \"id\": \"682adcffa9c8a2f863af6f00c4850323acbd4219\",\n" +
            "      \"name\": \"Update my device\",\n" +
            "      \"author\": \"John Doe <johndoe@gmail.com>\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"timestamp\": 1655198280000,\n" +
            "      \"id\": \"d2a6087c2b30e18cc55e7cdda345a8d0dfb959a4\",\n" +
            "      \"name\": \"Devices and assets - v1.0\",\n" +
            "      \"author\": \"John Doe <johndoe@gmail.com>\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"totalPages\": 1,\n" +
            "  \"totalElements\": 3,\n" +
            "  \"hasNext\": false\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping(value = "/version/{entityType}/{externalEntityUuid}", params = {"branch", "pageSize", "page"})
    public DeferredResult<PageData<EntityVersion>> listEntityVersions(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
                                                                      @PathVariable EntityType entityType,
                                                                      @Parameter(description = "A string value representing external entity id. This is `externalId` property of an entity, or otherwise if not set - simply id of this entity.")
                                                                      @PathVariable UUID externalEntityUuid,
                                                                      @Parameter(description = BRANCH_PARAM_DESCRIPTION)
                                                                      @RequestParam String branch,
                                                                      @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                                      @RequestParam int pageSize,
                                                                      @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                      @RequestParam int page,
                                                                      @Parameter(description = ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION)
                                                                      @RequestParam(required = false) String textSearch,
                                                                      @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = "timestamp"))
                                                                      @RequestParam(required = false) String sortProperty,
                                                                      @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                                                      @RequestParam(required = false) String sortOrder) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        EntityId externalEntityId = EntityIdFactory.getByTypeAndUuid(entityType, externalEntityUuid);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return wrapFuture(versionControlService.listEntityVersions(getTenantId(), branch, externalEntityId, pageLink));
    }

    @ApiOperation(value = "List entity type versions (listEntityTypeVersions)", notes = "" +
            "Returns list of versions of an entity type in a branch. This is a collected list of versions that were created " +
            "for entities of this type in a remote branch. \n" +
            "If specified branch does not exist - empty page data will be returned. " +
            "The response structure is the same as for `listEntityVersions` API method." +
            TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping(value = "/version/{entityType}", params = {"branch", "pageSize", "page"})
    public DeferredResult<PageData<EntityVersion>> listEntityTypeVersions(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
                                                                          @PathVariable EntityType entityType,
                                                                          @Parameter(description = BRANCH_PARAM_DESCRIPTION, required = true)
                                                                          @RequestParam String branch,
                                                                          @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                                          @RequestParam int pageSize,
                                                                          @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                          @RequestParam int page,
                                                                          @Parameter(description = ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION)
                                                                          @RequestParam(required = false) String textSearch,
                                                                          @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = "timestamp"))
                                                                          @RequestParam(required = false) String sortProperty,
                                                                          @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                                                          @RequestParam(required = false) String sortOrder) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return wrapFuture(versionControlService.listEntityTypeVersions(getTenantId(), branch, entityType, pageLink));
    }

    @ApiOperation(value = "List all versions (listVersions)", notes = "" +
            "Lists all available versions in a branch for all entity types. \n" +
            "If specified branch does not exist - empty page data will be returned. " +
            "The response format is the same as for `listEntityVersions` API method." +
            TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping(value = "/version", params = {"branch", "pageSize", "page"})
    public DeferredResult<PageData<EntityVersion>> listVersions(@Parameter(description = BRANCH_PARAM_DESCRIPTION, required = true)
                                                                @RequestParam String branch,
                                                                @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                                @RequestParam int pageSize,
                                                                @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                @RequestParam int page,
                                                                @Parameter(description = ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION)
                                                                @RequestParam(required = false) String textSearch,
                                                                @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = "timestamp"))
                                                                @RequestParam(required = false) String sortProperty,
                                                                @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                                                @RequestParam(required = false) String sortOrder) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return wrapFuture(versionControlService.listVersions(getTenantId(), branch, pageLink));
    }


    @ApiOperation(value = "List entities at version (listEntitiesAtVersion)", notes = "" +
            "Returns a list of remote entities of a specific entity type that are available at a concrete version. \n" +
            "Each entity item in the result has `externalId` property. " +
            "Entities order will be the same as in the repository." +
            TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping(value = "/entity/{entityType}/{versionId}")
    public DeferredResult<List<VersionedEntityInfo>> listEntitiesAtVersion(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
                                                                           @PathVariable EntityType entityType,
                                                                           @Parameter(description = VERSION_ID_PARAM_DESCRIPTION, required = true)
                                                                           @PathVariable String versionId) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return wrapFuture(versionControlService.listEntitiesAtVersion(getTenantId(), versionId, entityType));
    }

    @ApiOperation(value = "List all entities at version (listAllEntitiesAtVersion)", notes = "" +
            "Returns a list of all remote entities available in a specific version. " +
            "Response type is the same as for listAllEntitiesAtVersion API method. \n" +
            "Returned entities order will be the same as in the repository." +
            TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping(value = "/entity/{versionId}")
    public DeferredResult<List<VersionedEntityInfo>> listAllEntitiesAtVersion(@Parameter(description = VERSION_ID_PARAM_DESCRIPTION, required = true)
                                                                              @PathVariable String versionId) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return wrapFuture(versionControlService.listAllEntitiesAtVersion(getTenantId(), versionId));
    }

    @ApiOperation(value = "Get entity data info (getEntityDataInfo)", notes = "" +
            "Retrieves short info about the remote entity by external id at a concrete version. \n" +
            "Returned entity data info contains following properties: " +
            "`hasRelations` (whether stored entity data contains relations), `hasAttributes` (contains attributes) and " +
            "`hasCredentials` (whether stored device data has credentials)." +
            TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/info/{versionId}/{entityType}/{externalEntityUuid}")
    public DeferredResult<EntityDataInfo> getEntityDataInfo(@Parameter(description = VERSION_ID_PARAM_DESCRIPTION, required = true)
                                                            @PathVariable String versionId,
                                                            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
                                                            @PathVariable EntityType entityType,
                                                            @Parameter(description = "A string value representing external entity id", required = true)
                                                            @PathVariable UUID externalEntityUuid) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, externalEntityUuid);
        return wrapFuture(versionControlService.getEntityDataInfo(getCurrentUser(), entityId, versionId));
    }

    @ApiOperation(value = "Compare entity data to version (compareEntityDataToVersion)", notes = "" +
            "Returns an object with current entity data and the one at a specific version. " +
            "Entity data structure is the same as stored in a repository. " +
            TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping(value = "/diff/{entityType}/{internalEntityUuid}", params = {"versionId"})
    public DeferredResult<EntityDataDiff> compareEntityDataToVersion(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
                                                                     @PathVariable EntityType entityType,
                                                                     @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
                                                                     @PathVariable UUID internalEntityUuid,
                                                                     @Parameter(description = VERSION_ID_PARAM_DESCRIPTION, required = true)
                                                                     @RequestParam String versionId) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, internalEntityUuid);
        return wrapFuture(versionControlService.compareEntityDataToVersion(getCurrentUser(), entityId, versionId));
    }

    @ApiOperation(value = "Load entities version (loadEntitiesVersion)", notes = "" +
            "Loads specific version of remote entities (or single entity) by request. " +
            "Supported entity types: CUSTOMER, ASSET, RULE_CHAIN, DASHBOARD, DEVICE_PROFILE, DEVICE, ENTITY_VIEW, WIDGETS_BUNDLE." + NEW_LINE +
            "There are multiple types of request. Each of them requires branch name (`branch`) and version id (`versionId`). " +
            "Request of type `SINGLE_ENTITY` is needed to restore a concrete version of a specific entity. It contains " +
            "id of a remote entity (`externalEntityId`) and additional configuration (`config`):\n" +
            "- `loadRelations` - to update relations list (in case `saveRelations` option was enabled during version creation);\n" +
            "- `loadAttributes` - to load entity attributes (if `saveAttributes` config option was enabled);\n" +
            "- `loadCredentials` - to update device credentials (if `saveCredentials` option was enabled during version creation)." + NEW_LINE +
            "An example of such request:\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
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
            "    \"loadAttributes\": true,\n" +
            "    \"loadCredentials\": true\n" +
            "  }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "Another request type (`ENTITY_TYPE`) is needed to load specific version of the whole entity types. " +
            "It contains a structure with entity types to load and configs for each entity type (`entityTypes`). " +
            "For each specified entity type, the method will load all remote entities of this type that are present " +
            "at the version. A config for each entity type contains the same options as in `SINGLE_ENTITY` request type, and " +
            "additionally contains following options:\n" +
            "- `removeOtherEntities` - to remove local entities that are not present on the remote - basically to " +
            "   overwrite local entity type with the remote one;\n" +
            "- `findExistingEntityByName` - when you are loading some remote entities that are not yet present at this tenant, " +
            "   try to find existing entity by name and update it rather than create new." + NEW_LINE +
            "Here is an example of the request to completely restore version of the whole device entity type:\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"ENTITY_TYPE\",\n" +
            "\n" +
            "  \"branch\": \"dev\",\n" +
            "  \"versionId\": \"b3c28d722d328324c7c15b0b30047b0c40011cf7\",\n" +
            "\n" +
            "  \"entityTypes\": {\n" +
            "    \"DEVICE\": {\n" +
            "      \"removeOtherEntities\": true,\n" +
            "      \"findExistingEntityByName\": false,\n" +
            "      \"loadRelations\": true,\n" +
            "      \"loadAttributes\": true,\n" +
            "      \"loadCredentials\": true\n" +
            "    }\n" +
            "  }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "The response will contain generated request UUID that is to be used to check the status of operation " +
            "via `getVersionLoadRequestStatus`." +
            TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/entity")
    public UUID loadEntitiesVersion(@RequestBody VersionLoadRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.VERSION_CONTROL, Operation.WRITE);
        return versionControlService.loadEntitiesVersion(user, request);
    }

    @ApiOperation(value = "Get version load request status (getVersionLoadRequestStatus)", notes = "" +
            "Returns the status of previously made version load request. " +
            "The structure contains following parameters:\n" +
            "- `done` - if the request was successfully processed;\n" +
            "- `result` - a list of load results for each entity type:\n" +
            "     - `created` - created entities count;\n" +
            "     - `updated` - updated entities count;\n" +
            "     - `deleted` - removed entities count.\n" +
            "- `error` - if an error occurred during processing, error info:\n" +
            "     - `type` - error type;\n" +
            "     - `source` - an external id of remote entity;\n" +
            "     - `target` - if failed to find referenced entity by external id - this external id;\n" +
            "     - `message` - error message." + NEW_LINE +
            "An example of successfully processed request status:\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"done\": true,\n" +
            "  \"result\": [\n" +
            "    {\n" +
            "      \"entityType\": \"DEVICE\",\n" +
            "      \"created\": 10,\n" +
            "      \"updated\": 5,\n" +
            "      \"deleted\": 5\n" +
            "    },\n" +
            "     {\n" +
            "      \"entityType\": \"ASSET\",\n" +
            "      \"created\": 4,\n" +
            "      \"updated\": 0,\n" +
            "      \"deleted\": 8\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            TENANT_AUTHORITY_PARAGRAPH
    )
    @GetMapping(value = "/entity/{requestId}/status")
    public VersionLoadResult getVersionLoadRequestStatus(@Parameter(description = VC_REQUEST_ID_PARAM_DESCRIPTION, required = true)
                                                         @PathVariable UUID requestId) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        return versionControlService.getVersionLoadStatus(getCurrentUser(), requestId);
    }


    @ApiOperation(value = "List branches (listBranches)", notes = "" +
            "Lists branches available in the remote repository. \n\n" +
            "Response example: \n" +
            MARKDOWN_CODE_BLOCK_START +
            "[\n" +
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
            "]" +
            MARKDOWN_CODE_BLOCK_END)
    @GetMapping("/branches")
    public DeferredResult<List<BranchInfo>> listBranches() throws Exception {
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
    }

    @Override
    protected <T> DeferredResult<T> wrapFuture(ListenableFuture<T> future) {
        return wrapFuture(future, vcRequestTimeout);
    }

}
