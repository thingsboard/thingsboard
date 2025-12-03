/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.tenant.TbTenantService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class TenantController extends BaseController {

    private static final String TENANT_INFO_DESCRIPTION = "The Tenant Info object extends regular Tenant object and includes Tenant Profile name. ";

    private final TenantService tenantService;
    private final TbTenantService tbTenantService;

    @ApiOperation(value = "Get Tenant (getTenantById)",
            notes = "Fetch the Tenant object based on the provided Tenant Id. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/tenant/{tenantId}")
    public Tenant getTenantById(
            @Parameter(description = TENANT_ID_PARAM_DESCRIPTION)
            @PathVariable(TENANT_ID) String strTenantId) throws ThingsboardException {
        checkParameter(TENANT_ID, strTenantId);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
        Tenant tenant = checkTenantId(tenantId, Operation.READ);
        checkDashboardInfo(tenant.getAdditionalInfo(), HOME_DASHBOARD);
        return tenant;
    }

    @ApiOperation(value = "Get Tenant Info (getTenantInfoById)",
            notes = "Fetch the Tenant Info object based on the provided Tenant Id. " +
                    TENANT_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/tenant/info/{tenantId}")
    public TenantInfo getTenantInfoById(
            @Parameter(description = TENANT_ID_PARAM_DESCRIPTION)
            @PathVariable(TENANT_ID) String strTenantId) throws ThingsboardException {
        checkParameter(TENANT_ID, strTenantId);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
        return checkTenantInfoId(tenantId, Operation.READ);
    }

    @ApiOperation(value = "Create Or update Tenant (saveTenant)",
            notes = "Create or update the Tenant. When creating tenant, platform generates Tenant Id as " + UUID_WIKI_LINK +
                    "Default Rule Chain and Device profile are also generated for the new tenants automatically. " +
                    "The newly created Tenant Id will be present in the response. " +
                    "Specify existing Tenant Id id to update the Tenant. " +
                    "Referencing non-existing Tenant Id will cause 'Not Found' error." +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Tenant entity." +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping(value = "/tenant")
    public Tenant saveTenant(@Parameter(description = "A JSON value representing the tenant.")
                             @RequestBody Tenant tenant) throws Exception {
        checkEntity(tenant.getId(), tenant, Resource.TENANT);
        return tbTenantService.save(tenant);
    }

    @ApiOperation(value = "Delete Tenant (deleteTenant)",
            notes = "Deletes the tenant, it's customers, rule chains, devices and all other related entities. Referencing non-existing tenant Id will cause an error." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(value = "/tenant/{tenantId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteTenant(@Parameter(description = TENANT_ID_PARAM_DESCRIPTION)
                             @PathVariable(TENANT_ID) String strTenantId) throws Exception {
        checkParameter(TENANT_ID, strTenantId);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
        Tenant tenant = checkTenantId(tenantId, Operation.DELETE);
        tbTenantService.delete(tenant);
    }

    @ApiOperation(value = "Get Tenants (getTenants)", notes = "Returns a page of tenants registered in the platform. " + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/tenants", params = {"pageSize", "page"})
    public PageData<Tenant> getTenants(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = TENANT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "email", "country", "state", "city", "address", "address2", "zip", "phone", "email"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tenantService.findTenants(pageLink));
    }

    @ApiOperation(value = "Get Tenants Info (getTenants)", notes = "Returns a page of tenant info objects registered in the platform. "
            + TENANT_INFO_DESCRIPTION + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/tenantInfos", params = {"pageSize", "page"})
    public PageData<TenantInfo> getTenantInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = TENANT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "tenantProfileName", "title", "email", "country", "state", "city", "address", "address2", "zip", "phone", "email"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tenantService.findTenantInfos(pageLink));
    }

}
