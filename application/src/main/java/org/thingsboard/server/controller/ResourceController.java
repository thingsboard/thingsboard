/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.transport.resource.Resource;
import org.thingsboard.server.common.data.transport.resource.ResourceType;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class ResourceController extends BaseController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource", method = RequestMethod.POST)
    @ResponseBody
    public Resource saveResource(Resource resource) throws ThingsboardException {
        try {
            resource.setTenantId(getTenantId());
            Resource savedResource = checkNotNull(resourceService.saveResource(resource));
            tbClusterService.onResourceChange(savedResource, null);
            return savedResource;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource", method = RequestMethod.GET)
    @ResponseBody
    public List<Resource> getResources(@RequestParam(required = false) boolean system) throws ThingsboardException {
        try {
            return checkNotNull(resourceService.findResourcesByTenantId(system ? TenantId.SYS_TENANT_ID : getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/{resourceType}", method = RequestMethod.GET)
    @ResponseBody
    public List<Resource> getResources(@RequestParam(required = false) boolean system,
                                       @PathVariable("resourceType") ResourceType resourceType) throws ThingsboardException {
        try {
            return checkNotNull(resourceService.findResourcesByTenantIdResourceType(system ? TenantId.SYS_TENANT_ID : getTenantId(), resourceType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/{resourceType}/{resourceId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteResource(@PathVariable("resourceType") ResourceType resourceType,
                               @PathVariable("resourceId") String resourceId) throws ThingsboardException {
        try {
            Resource resource = checkNotNull(resourceService.getResource(getTenantId(), resourceType, resourceId));
            resourceService.deleteResource(getTenantId(), resourceType, resourceId);
            tbClusterService.onResourceDeleted(resource, null);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
