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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceDeleteResult;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.util.ThrowingSupplier;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.resource.TbResourceService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.thingsboard.server.controller.ControllerConstants.LWM2M_OBJECT_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_SUB_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class TbResourceController extends BaseController {

    private static final String DOWNLOAD_RESOURCE_IF_NOT_CHANGED = "Download Resource based on the provided Resource Id or return 304 status code if resource was not changed.";
    private final TbResourceService tbResourceService;

    public static final String RESOURCE_ID = "resourceId";

    @ApiOperation(value = "Download Resource (downloadResource)", notes = "Download Resource based on the provided Resource Id." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/{resourceId}/download")
    public ResponseEntity<ByteArrayResource> downloadResource(@Parameter(description = RESOURCE_ID_PARAM_DESCRIPTION)
                                                              @PathVariable(RESOURCE_ID) String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        TbResource tbResource = checkResourceId(resourceId, Operation.READ);

        ByteArrayResource resource = new ByteArrayResource(tbResource.getData());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + tbResource.getFileName())
                .header("x-filename", tbResource.getFileName())
                .contentLength(resource.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @ApiOperation(value = "Download resource (downloadResource)",
            notes = "Download resource with a given type and key for the given scope" + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/resource/{resourceType}/{scope}/{key}")
    public ResponseEntity<ByteArrayResource> downloadResourceIfChanged(@Parameter(description = "Type of the resource", schema = @Schema(allowableValues = {"lwm2m_model", "jks", "pkcs_12", "js_module", "dashboard"}))
                                                                       @PathVariable("resourceType") String resourceTypeStr,
                                                                       @Parameter(description = "Scope of the resource", schema = @Schema(allowableValues = {"system", "tenant"}))
                                                                       @PathVariable String scope,
                                                                       @Parameter(description = "Key of the resource, e.g. 'extension.js'")
                                                                       @PathVariable String key,
                                                                       @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws ThingsboardException {

        ResourceType resourceType = ResourceType.valueOf(resourceTypeStr.toUpperCase());
        return downloadResourceIfChanged(() -> checkResourceInfo(scope, resourceType, key, Operation.READ), etag);
    }

    @ApiOperation(value = "Download LWM2M Resource (downloadLwm2mResourceIfChanged)", notes = DOWNLOAD_RESOURCE_IF_NOT_CHANGED + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/lwm2m/{resourceId}/download", produces = "application/xml")
    public ResponseEntity<ByteArrayResource> downloadLwm2mResourceIfChanged(@Parameter(description = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                            @PathVariable(RESOURCE_ID) String strResourceId,
                                                                            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws ThingsboardException {
        return downloadResourceIfChanged(strResourceId, etag);
    }

    @ApiOperation(value = "Download PKCS_12 Resource (downloadPkcs12ResourceIfChanged)", notes = DOWNLOAD_RESOURCE_IF_NOT_CHANGED + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/pkcs12/{resourceId}/download", method = RequestMethod.GET, produces = "application/x-pkcs12")
    public ResponseEntity<ByteArrayResource> downloadPkcs12ResourceIfChanged(@Parameter(description = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                             @PathVariable(RESOURCE_ID) String strResourceId,
                                                                             @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws ThingsboardException {
        return downloadResourceIfChanged(strResourceId, etag);
    }

    @ApiOperation(value = "Download JKS Resource (downloadJksResourceIfChanged)",
            notes = DOWNLOAD_RESOURCE_IF_NOT_CHANGED + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/jks/{resourceId}/download", produces = "application/x-java-keystore")
    public ResponseEntity<ByteArrayResource> downloadJksResourceIfChanged(@Parameter(description = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                          @PathVariable(RESOURCE_ID) String strResourceId,
                                                                          @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws ThingsboardException {
        return downloadResourceIfChanged(strResourceId, etag);
    }

    @ApiOperation(value = "Download JS Resource (downloadJsResourceIfChanged)", notes = DOWNLOAD_RESOURCE_IF_NOT_CHANGED + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/resource/js/{resourceId}/download", produces = "application/javascript")
    public ResponseEntity<ByteArrayResource> downloadJsResourceIfChanged(@Parameter(description = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                         @PathVariable(RESOURCE_ID) String strResourceId,
                                                                         @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws ThingsboardException {
        return downloadResourceIfChanged(strResourceId, etag);
    }

    @ApiOperation(value = "Get Resource Info (getResourceInfoById)",
            notes = "Fetch the Resource Info object based on the provided Resource Id. " +
                    RESOURCE_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/info/{resourceId}")
    public TbResourceInfo getResourceInfoById(@Parameter(description = RESOURCE_ID_PARAM_DESCRIPTION)
                                              @PathVariable(RESOURCE_ID) String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        return checkResourceInfoId(resourceId, Operation.READ);
    }

    @ApiOperation(value = "Get resource info (getResourceInfo)",
            notes = "Get info for the resource with the given type, scope and key. " +
                    RESOURCE_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/{resourceType}/{scope}/{key}/info")
    public TbResourceInfo getResourceInfo(@Parameter(description = "Type of the resource", schema = @Schema(allowableValues = {"lwm2m_model", "jks", "pkcs_12", "js_module", "dashboard"}))
                                          @PathVariable("resourceType") String resourceTypeStr,
                                          @Parameter(description = "Scope of the resource", schema = @Schema(allowableValues = {"system", "tenant"}))
                                          @PathVariable String scope,
                                          @Parameter(description = "Key of the resource, e.g. 'extension.js'")
                                          @PathVariable String key) throws ThingsboardException {
        ResourceType resourceType = ResourceType.valueOf(resourceTypeStr.toUpperCase());
        return checkResourceInfo(scope, resourceType, key, Operation.READ);
    }

    @ApiOperation(value = "Get Resource (getResourceById)",
            notes = "Fetch the Resource object based on the provided Resource Id. " +
                    RESOURCE_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH, hidden = true)
    @Deprecated  // resource's data should be fetched with a download request
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/{resourceId}")
    public TbResource getResourceById(@Parameter(description = RESOURCE_ID_PARAM_DESCRIPTION)
                                      @PathVariable(RESOURCE_ID) String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        return checkResourceId(resourceId, Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Resource (saveResource)",
            notes = "Create or update the Resource. When creating the Resource, platform generates Resource id as " + UUID_WIKI_LINK +
                    "The newly created Resource id will be present in the response. " +
                    "Specify existing Resource id to update the Resource. " +
                    "Referencing non-existing Resource Id will cause 'Not Found' error. " +
                    "\n\nResource combination of the title with the key is unique in the scope of tenant. " +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Resource entity." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/resource")
    public TbResourceInfo saveResource(@Parameter(description = "A JSON value representing the Resource.")
                                       @RequestBody TbResource resource) throws Exception {
        resource.setTenantId(getTenantId());
        checkEntity(resource.getId(), resource, Resource.TB_RESOURCE);
        return tbResourceService.save(resource, getCurrentUser());
    }

    @ApiOperation(value = "Get Resource Infos (getResources)",
            notes = "Returns a page of Resource Info objects owned by tenant or sysadmin. " +
                    PAGE_DATA_PARAMETERS + RESOURCE_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource")
    public PageData<TbResourceInfo> getResources(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                 @RequestParam int pageSize,
                                                 @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                 @RequestParam int page,
                                                 @Parameter(description = RESOURCE_TYPE, schema = @Schema(allowableValues = {"LWM2M_MODEL", "JKS", "PKCS_12", "JS_MODULE"}))
                                                 @RequestParam(required = false) String resourceType,
                                                 @Parameter(description = RESOURCE_SUB_TYPE, schema = @Schema(allowableValues = {"EXTENSION", "MODULE"}))
                                                 @RequestParam(required = false) String resourceSubType,
                                                 @Parameter(description = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                                 @RequestParam(required = false) String textSearch,
                                                 @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "resourceType", "tenantId"}))
                                                 @RequestParam(required = false) String sortProperty,
                                                 @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                                 @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TbResourceInfoFilter.TbResourceInfoFilterBuilder filter = TbResourceInfoFilter.builder();
        filter.tenantId(getTenantId());
        Set<ResourceType> resourceTypes = new HashSet<>();
        if (StringUtils.isNotEmpty(resourceType)) {
            resourceTypes.add(ResourceType.valueOf(resourceType));
            if (StringUtils.isNotEmpty(resourceSubType)) {
                filter.resourceSubTypes(Set.of(ResourceSubType.valueOf(resourceSubType)));
            }
        } else {
            Collections.addAll(resourceTypes, ResourceType.values());
            resourceTypes.remove(ResourceType.JS_MODULE);
            resourceTypes.remove(ResourceType.IMAGE);
            resourceTypes.remove(ResourceType.DASHBOARD);
        }
        filter.resourceTypes(resourceTypes);
        if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
            return checkNotNull(resourceService.findTenantResourcesByTenantId(filter.build(), pageLink));
        } else {
            return checkNotNull(resourceService.findAllTenantResourcesByTenantId(filter.build(), pageLink));
        }
    }

    @ApiOperation(value = "Get Resource Infos by ids (getSystemOrTenantResourcesByIds)")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource", params = {"resourceIds"})
    public List<TbResourceInfo> getSystemOrTenantResourcesByIds(
            @Parameter(description = "A list of resource ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam("resourceIds") Set<UUID> resourceUuids) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        List<TbResourceId> resourceIds = new ArrayList<>();
        for (UUID resourceId : resourceUuids) {
            resourceIds.add(new TbResourceId(resourceId));
        }
        return resourceService.findSystemOrTenantResourcesByIds(user.getTenantId(), resourceIds);
    }

    @ApiOperation(value = "Get All Resource Infos (getAllResources)",
            notes = "Returns a page of Resource Info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + RESOURCE_INFO_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/resource/tenant")
    public PageData<TbResourceInfo> getTenantResources(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                       @RequestParam int pageSize,
                                                       @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                       @RequestParam int page,
                                                       @Parameter(description = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                                       @RequestParam(required = false) String textSearch,
                                                       @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "resourceType", "tenantId"}))
                                                       @RequestParam(required = false) String sortProperty,
                                                       @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                                       @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(getTenantId())
                .resourceTypes(EnumSet.allOf(ResourceType.class))
                .build();
        return checkNotNull(resourceService.findTenantResourcesByTenantId(filter, pageLink));
    }

    @ApiOperation(value = "Get LwM2M Objects (getLwm2mListObjectsPage)",
            notes = "Returns a page of LwM2M objects parsed from Resources with type 'LWM2M_MODEL' owned by tenant or sysadmin. " +
                    PAGE_DATA_PARAMETERS + LWM2M_OBJECT_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/resource/lwm2m/page")
    public List<LwM2mObject> getLwm2mListObjectsPage(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                     @RequestParam int pageSize,
                                                     @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                     @RequestParam int page,
                                                     @Parameter(description = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                                     @RequestParam(required = false) String textSearch,
                                                     @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"id", "name"}))
                                                     @RequestParam(required = false) String sortProperty,
                                                     @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                                     @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = new PageLink(pageSize, page, textSearch);
        return checkNotNull(tbResourceService.findLwM2mObjectPage(getTenantId(), sortProperty, sortOrder, pageLink));
    }

    @ApiOperation(value = "Get LwM2M Objects (getLwm2mListObjects)",
            notes = "Returns a page of LwM2M objects parsed from Resources with type 'LWM2M_MODEL' owned by tenant or sysadmin. " +
                    "You can specify parameters to filter the results. " + LWM2M_OBJECT_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/resource/lwm2m")
    public List<LwM2mObject> getLwm2mListObjects(@Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}, requiredMode = Schema.RequiredMode.REQUIRED))
                                                 @RequestParam String sortOrder,
                                                 @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"id", "name"}, requiredMode = Schema.RequiredMode.REQUIRED))
                                                 @RequestParam String sortProperty,
                                                 @Parameter(description = "LwM2M Object ids.", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
                                                 @RequestParam(required = false) String[] objectIds) throws ThingsboardException {
        return checkNotNull(tbResourceService.findLwM2mObject(getTenantId(), sortOrder, sortProperty, objectIds));
    }

    @ApiOperation(value = "Delete Resource (deleteResource)",
            notes = "Deletes the Resource. Referencing non-existing Resource Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(value = "/resource/{resourceId}")
    public ResponseEntity<TbResourceDeleteResult> deleteResource(@Parameter(description = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                 @PathVariable("resourceId") String strResourceId,
                                                                 @RequestParam(name = "force", required = false) boolean force) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        TbResource tbResource = checkResourceId(resourceId, Operation.DELETE);
        TbResourceDeleteResult tbResourceDeleteResult = tbResourceService.delete(tbResource, force, getCurrentUser());
        return (tbResourceDeleteResult.isSuccess() ? ResponseEntity.ok() : ResponseEntity.badRequest()).body(tbResourceDeleteResult);
    }

    private ResponseEntity<ByteArrayResource> downloadResourceIfChanged(String strResourceId, String etag) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        return downloadResourceIfChanged(() -> checkResourceInfoId(resourceId, Operation.READ), etag);
    }

    private ResponseEntity<ByteArrayResource> downloadResourceIfChanged(ThrowingSupplier<TbResourceInfo> resourceInfoProvider,
                                                                        String etag) throws ThingsboardException {
        TbResourceInfo resourceInfo = resourceInfoProvider.get();
        if (etag != null) {
            etag = StringUtils.remove(etag, '\"'); // etag is wrapped in double quotes due to HTTP specification
            if (etag.equals(resourceInfo.getEtag())) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(resourceInfo.getEtag())
                        .build();
            }
        }

        byte[] data = resourceService.getResourceData(resourceInfo.getTenantId(), resourceInfo.getId());
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + resourceInfo.getFileName())
                .header("x-filename", resourceInfo.getFileName())
                .contentLength(resource.contentLength())
                .header("Content-Type", resourceInfo.getResourceType().getMediaType())
                .cacheControl(CacheControl.noCache())
                .eTag(resourceInfo.getEtag())
                .body(resource);
    }

    private TbResourceInfo checkResourceInfo(String scope, ResourceType resourceType, String key, Operation operation) throws ThingsboardException {
        TenantId tenantId;
        if (scope.equals("tenant")) {
            tenantId = getTenantId();
        } else if (scope.equals("system")) {
            tenantId = TenantId.SYS_TENANT_ID;
        } else {
            throw new IllegalArgumentException("Invalid scope");
        }

        TbResourceInfo resourceInfo = resourceService.findResourceInfoByTenantIdAndKey(tenantId, resourceType, key);
        checkEntity(getCurrentUser(), checkNotNull(resourceInfo), operation);
        return resourceInfo;
    }

}
