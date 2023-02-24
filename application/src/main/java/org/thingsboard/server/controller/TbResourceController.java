/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.resource.TbResourceService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Base64;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.LWM2M_OBJECT_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.LWM2M_OBJECT_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
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

    private final TbResourceService tbResourceService;

    public static final String RESOURCE_ID = "resourceId";

    @ApiOperation(value = "Download Resource (downloadResource)", notes = "Download Resource based on the provided Resource Id." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/{resourceId}/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> downloadResource(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                                 @PathVariable(RESOURCE_ID) String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        try {
            TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
            TbResource tbResource = checkResourceId(resourceId, Operation.READ);

            ByteArrayResource resource = new ByteArrayResource(Base64.getDecoder().decode(tbResource.getData().getBytes()));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + tbResource.getFileName())
                    .header("x-filename", tbResource.getFileName())
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Resource Info (getResourceInfoById)",
            notes = "Fetch the Resource Info object based on the provided Resource Id. " +
                    RESOURCE_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/info/{resourceId}", method = RequestMethod.GET)
    @ResponseBody
    public TbResourceInfo getResourceInfoById(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                              @PathVariable(RESOURCE_ID) String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        try {
            TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
            return checkResourceInfoId(resourceId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Resource (getResourceById)",
            notes = "Fetch the Resource object based on the provided Resource Id. " +
                    RESOURCE_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/{resourceId}", method = RequestMethod.GET)
    @ResponseBody
    public TbResource getResourceById(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                      @PathVariable(RESOURCE_ID) String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        try {
            TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
            return checkResourceId(resourceId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Resource (saveResource)",
            notes = "Create or update the Resource. When creating the Resource, platform generates Resource id as " + UUID_WIKI_LINK +
                    "The newly created Resource id will be present in the response. " +
                    "Specify existing Resource id to update the Resource. " +
                    "Referencing non-existing Resource Id will cause 'Not Found' error. " +
                    "\n\nResource combination of the title with the key is unique in the scope of tenant. " +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Resource entity." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json",
            consumes = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource", method = RequestMethod.POST)
    @ResponseBody
    public TbResource saveResource(@ApiParam(value = "A JSON value representing the Resource.")
                                   @RequestBody TbResource resource) throws Exception {
        resource.setTenantId(getTenantId());
        checkEntity(resource.getId(), resource, Resource.TB_RESOURCE);
        return tbResourceService.save(resource, getCurrentUser());
    }

    @ApiOperation(value = "Get Resource Infos (getResources)",
            notes = "Returns a page of Resource Info objects owned by tenant or sysadmin. " +
                    PAGE_DATA_PARAMETERS + RESOURCE_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource", method = RequestMethod.GET)
    @ResponseBody
    public PageData<TbResourceInfo> getResources(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                 @RequestParam int pageSize,
                                                 @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                 @RequestParam int page,
                                                 @ApiParam(value = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                                 @RequestParam(required = false) String textSearch,
                                                 @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = RESOURCE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                 @RequestParam(required = false) String sortProperty,
                                                 @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                 @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
                return checkNotNull(resourceService.findTenantResourcesByTenantId(getTenantId(), pageLink));
            } else {
                return checkNotNull(resourceService.findAllTenantResourcesByTenantId(getTenantId(), pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get LwM2M Objects (getLwm2mListObjectsPage)",
            notes = "Returns a page of LwM2M objects parsed from Resources with type 'LWM2M_MODEL' owned by tenant or sysadmin. " +
                    PAGE_DATA_PARAMETERS + LWM2M_OBJECT_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/resource/lwm2m/page", method = RequestMethod.GET)
    @ResponseBody
    public List<LwM2mObject> getLwm2mListObjectsPage(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                     @RequestParam int pageSize,
                                                     @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                     @RequestParam int page,
                                                     @ApiParam(value = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                                     @RequestParam(required = false) String textSearch,
                                                     @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = LWM2M_OBJECT_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                     @RequestParam(required = false) String sortProperty,
                                                     @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                     @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = new PageLink(pageSize, page, textSearch);
            return checkNotNull(resourceService.findLwM2mObjectPage(getTenantId(), sortProperty, sortOrder, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get LwM2M Objects (getLwm2mListObjects)",
            notes = "Returns a page of LwM2M objects parsed from Resources with type 'LWM2M_MODEL' owned by tenant or sysadmin. " +
                    "You can specify parameters to filter the results. " + LWM2M_OBJECT_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/resource/lwm2m", method = RequestMethod.GET)
    @ResponseBody
    public List<LwM2mObject> getLwm2mListObjects(@ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES, required = true)
                                                 @RequestParam String sortOrder,
                                                 @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = LWM2M_OBJECT_SORT_PROPERTY_ALLOWABLE_VALUES, required = true)
                                                 @RequestParam String sortProperty,
                                                 @ApiParam(value = "LwM2M Object ids.", required = true)
                                                 @RequestParam(required = false) String[] objectIds) throws ThingsboardException {
        try {
            return checkNotNull(resourceService.findLwM2mObject(getTenantId(), sortOrder, sortProperty, objectIds));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete Resource (deleteResource)",
            notes = "Deletes the Resource. Referencing non-existing Resource Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/{resourceId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteResource(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                               @PathVariable("resourceId") String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        TbResource tbResource = checkResourceId(resourceId, Operation.DELETE);
        tbResourceService.delete(tbResource, getCurrentUser());
    }
}