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
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.mobile.TbMobileAppBundleService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MobileAppBundleController extends BaseController {

    private final TbMobileAppBundleService tbMobileAppBundleService;

    @ApiOperation(value = "Save Or update Mobile app bundle (saveMobileAppBundle)",
            notes = "Create or update the Mobile app bundle that represents tha pair of ANDROID and IOS app and " +
                    "mobile settings like oauth2 clients, self-registration and layout configuration." +
                    "When creating mobile app bundle, platform generates Mobile App Bundle Id as " + UUID_WIKI_LINK +
                    "The newly created Mobile App Bundle Id will be present in the response. " +
                    "Referencing non-existing Mobile App Bundle Id will cause 'Not Found' error."  + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/mobile/bundle")
    public MobileAppBundle saveMobileAppBundle(
            @Parameter(description = "A JSON value representing the Mobile Application Bundle.", required = true)
            @RequestBody @Valid MobileAppBundle mobileAppBundle,
            @Parameter(description = "A list of oauth2 client ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam(name = "oauth2ClientIds", required = false) UUID[] ids) throws Exception {
        mobileAppBundle.setTenantId(getTenantId());
        checkEntity(mobileAppBundle.getId(), mobileAppBundle, Resource.MOBILE_APP_BUNDLE);
        return tbMobileAppBundleService.save(mobileAppBundle, getOAuth2ClientIds(ids), getCurrentUser());
    }

    @ApiOperation(value = "Update oauth2 clients (updateOauth2Clients)",
            notes = "Update oauth2 clients of the specified mobile app bundle." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(value = "/mobile/bundle/{id}/oauth2Clients")
    public void updateOauth2Clients(@PathVariable UUID id,
                                    @RequestBody UUID[] clientIds) throws ThingsboardException {
        MobileAppBundleId mobileAppBundleId = new MobileAppBundleId(id);
        MobileAppBundle mobileAppBundle = checkMobileAppBundleId(mobileAppBundleId, Operation.WRITE);
        List<OAuth2ClientId> oAuth2ClientIds = getOAuth2ClientIds(clientIds);
        tbMobileAppBundleService.updateOauth2Clients(mobileAppBundle, oAuth2ClientIds, getCurrentUser());
    }

    @ApiOperation(value = "Get mobile app bundle infos (getTenantMobileAppBundleInfos)", notes = SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/mobile/bundle/infos")
    public PageData<MobileAppBundleInfo> getTenantMobileAppBundleInfos(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                                       @RequestParam int pageSize,
                                                                       @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                       @RequestParam int page,
                                                                       @Parameter(description = "Case-insensitive 'substring' filter based on app's name")
                                                                       @RequestParam(required = false) String textSearch,
                                                                       @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                                                       @RequestParam(required = false) String sortProperty,
                                                                       @Parameter(description = SORT_ORDER_DESCRIPTION)
                                                                       @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return mobileAppBundleService.findMobileAppBundleInfosByTenantId(getTenantId(), pageLink);
    }

    @ApiOperation(value = "Get mobile app bundle info by id (getMobileAppBundleInfoById)", notes = SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/mobile/bundle/info/{id}")
    public MobileAppBundleInfo getMobileAppBundleInfoById(@PathVariable UUID id) throws ThingsboardException {
        MobileAppBundleId mobileAppBundleId = new MobileAppBundleId(id);
        return checkEntityId(mobileAppBundleId, mobileAppBundleService::findMobileAppBundleInfoById, Operation.READ);
    }

    @ApiOperation(value = "Delete Mobile App Bundle by ID (deleteMobileAppBundle)",
            notes = "Deletes Mobile App Bundle by ID. Referencing non-existing mobile app bundle Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(value = "/mobile/bundle/{id}")
    public void deleteMobileAppBundle(@PathVariable UUID id) throws Exception {
        MobileAppBundleId mobileAppBundleId = new MobileAppBundleId(id);
        MobileAppBundle mobileAppBundle = checkMobileAppBundleId(mobileAppBundleId, Operation.DELETE);
        tbMobileAppBundleService.delete(mobileAppBundle, getCurrentUser());
    }

}
