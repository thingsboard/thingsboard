/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.common.data.mobile.MobileAppInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.mobile.TbMobileAppService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MobileAppController extends BaseController {

    private final TbMobileAppService tbMobileAppService;

    @ApiOperation(value = "Save Or update Mobile app (saveMobileApp)",
            notes = "Create or update the Mobile app. When creating mobile app, platform generates Mobile App Id as " + UUID_WIKI_LINK +
                    "The newly created Mobile App Id will be present in the response. " +
                    "Specify existing Mobile App Id to update the mobile app. " +
                    "Referencing non-existing Mobile App Id will cause 'Not Found' error." +
                    "\n\nMobile app package name is unique for entire platform setup.\n\n" + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PostMapping(value = "/mobileApp")
    public MobileApp saveMobileApp(
            @Parameter(description = "A JSON value representing the Mobile Application.", required = true)
            @RequestBody @Valid MobileApp mobileApp,
            @Parameter(description = "A list of entity oauth2 client ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam(name = "oauth2ClientIds", required = false) UUID[] ids) throws Exception {
        mobileApp.setTenantId(getTenantId());
        checkEntity(mobileApp.getId(), mobileApp, Resource.MOBILE_APP);
        return tbMobileAppService.save(mobileApp, getOAuth2ClientIds(ids), getCurrentUser());
    }

    @ApiOperation(value = "Update oauth2 clients (updateOauth2Clients)",
            notes = "Update oauth2 clients of the specified mobile app. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PutMapping(value = "/mobileApp/{id}/oauth2Clients")
    public void updateOauth2Clients(@PathVariable UUID id,
                                    @RequestBody UUID[] clientIds) throws ThingsboardException {
        MobileAppId mobileAppId = new MobileAppId(id);
        MobileApp mobileApp = checkMobileAppId(mobileAppId, Operation.WRITE);
        List<OAuth2ClientId> oAuth2ClientIds = getOAuth2ClientIds(clientIds);
        tbMobileAppService.updateOauth2Clients(mobileApp, oAuth2ClientIds, getCurrentUser());
    }

    @ApiOperation(value = "Get mobile app infos (getTenantMobileAppInfos)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @GetMapping(value = "/mobileApp/infos")
    public PageData<MobileAppInfo> getTenantMobileAppInfos(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                           @RequestParam int pageSize,
                                                           @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                           @RequestParam int page,
                                                           @Parameter(description = "Case-insensitive 'substring' filter based on app's name")
                                                           @RequestParam(required = false) String textSearch,
                                                           @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                                           @RequestParam(required = false) String sortProperty,
                                                           @Parameter(description = SORT_ORDER_DESCRIPTION)
                                                           @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.MOBILE_APP, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return mobileAppService.findMobileAppInfosByTenantId(getTenantId(), pageLink);
    }

    @ApiOperation(value = "Get mobile info by id (getMobileAppInfoById)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @GetMapping(value = "/mobileApp/info/{id}")
    public MobileAppInfo getMobileAppInfoById(@PathVariable UUID id) throws ThingsboardException {
        MobileAppId mobileAppId = new MobileAppId(id);
        return checkEntityId(mobileAppId, mobileAppService::findMobileAppInfoById, Operation.READ);
    }

    @ApiOperation(value = "Delete Mobile App by ID (deleteMobileApp)",
            notes = "Deletes Mobile App by ID. Referencing non-existing mobile app Id will cause an error." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @DeleteMapping(value = "/mobileApp/{id}")
    public void deleteMobileApp(@PathVariable UUID id) throws Exception {
        MobileAppId mobileAppId = new MobileAppId(id);
        MobileApp mobileApp = checkMobileAppId(mobileAppId, Operation.DELETE);
        tbMobileAppService.delete(mobileApp, getCurrentUser());
    }

}
