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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.Views;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.mobile.LoginMobileInfo;
import org.thingsboard.server.common.data.mobile.UserMobileInfo;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppVersionInfo;
import org.thingsboard.server.common.data.mobile.app.StoreInfo;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.layout.MobilePage;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.mobile.TbMobileAppService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
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
public class MobileAppController extends BaseController {

    private final TbMobileAppService tbMobileAppService;

    @ApiOperation(value = "Get mobile app login info (getLoginMobileInfo)")
    @GetMapping(value = "/noauth/mobile")
    public LoginMobileInfo getLoginMobileInfo(@Parameter(description = "Mobile application package name")
                                              @RequestParam String pkgName,
                                              @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
                                              @RequestParam PlatformType platform) {
        List<OAuth2ClientLoginInfo> oauth2Clients = oAuth2ClientService.findOAuth2ClientLoginInfosByMobilePkgNameAndPlatformType(pkgName, platform);
        MobileApp mobileApp = mobileAppService.findMobileAppByPkgNameAndPlatformType(pkgName, platform);
        StoreInfo storeInfo = Optional.ofNullable(mobileApp).map(MobileApp::getStoreInfo).orElse(null);
        MobileAppVersionInfo versionInfo = Optional.ofNullable(mobileApp).map(MobileApp::getVersionInfo).orElse(null);
        return new LoginMobileInfo(oauth2Clients, storeInfo, versionInfo);
    }

    @ApiOperation(value = "Get user mobile app basic info (getUserMobileInfo)", notes = AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/mobile")
    public UserMobileInfo getUserMobileInfo(@Parameter(description = "Mobile application package name")
                                            @RequestParam String pkgName,
                                            @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
                                            @RequestParam PlatformType platform) throws ThingsboardException, JsonProcessingException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        HomeDashboardInfo homeDashboardInfo = securityUser.isSystemAdmin() ? null : getHomeDashboardInfo(securityUser, user.getAdditionalInfo());
        MobileAppBundle mobileAppBundle = mobileAppBundleService.findMobileAppBundleByPkgNameAndPlatform(securityUser.getTenantId(), pkgName, platform);
        MobileApp mobileApp = mobileAppService.findMobileAppByPkgNameAndPlatformType(pkgName, platform);
        StoreInfo storeInfo = Optional.ofNullable(mobileApp).map(MobileApp::getStoreInfo).orElse(null);
        MobileAppVersionInfo versionInfo = Optional.ofNullable(mobileApp).map(MobileApp::getVersionInfo).orElse(null);
        return new UserMobileInfo(user, storeInfo, versionInfo, homeDashboardInfo, getVisiblePages(mobileAppBundle));
    }

    @ApiOperation(value = "Save Or update Mobile app (saveMobileApp)",
            notes = "Create or update the Mobile app. When creating mobile app, platform generates Mobile App Id as " + UUID_WIKI_LINK +
                    "The newly created Mobile App Id will be present in the response. " +
                    "Specify existing Mobile App Id to update the mobile app. " +
                    "Referencing non-existing Mobile App Id will cause 'Not Found' error." +
                    "\n\nThe pair of mobile app package name and platform type is unique for entire platform setup.\n\n" + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/mobile/app")
    public MobileApp saveMobileApp(
            @Parameter(description = "A JSON value representing the Mobile Application.", required = true)
            @RequestBody @Valid MobileApp mobileApp) throws Exception {
        mobileApp.setTenantId(getTenantId());
        checkEntity(mobileApp.getId(), mobileApp, Resource.MOBILE_APP);
        return tbMobileAppService.save(mobileApp, getCurrentUser());
    }

    @ApiOperation(value = "Get mobile app infos (getTenantMobileAppInfos)", notes = SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/mobile/app")
    public PageData<MobileApp> getTenantMobileApps(@Parameter(description = "Platform type: ANDROID or IOS")
                                                   @RequestParam(required = false) PlatformType platformType,
                                                   @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
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
        return mobileAppService.findMobileAppsByTenantId(getTenantId(), platformType, pageLink);
    }

    @ApiOperation(value = "Get mobile info by id (getMobileAppInfoById)", notes = SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/mobile/app/{id}")
    public MobileApp getMobileAppById(@PathVariable UUID id) throws ThingsboardException {
        MobileAppId mobileAppId = new MobileAppId(id);
        return checkEntityId(mobileAppId, mobileAppService::findMobileAppById, Operation.READ);
    }

    @ApiOperation(value = "Delete Mobile App by ID (deleteMobileApp)",
            notes = "Deletes Mobile App by ID. Referencing non-existing mobile app Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(value = "/mobile/app/{id}")
    public void deleteMobileApp(@PathVariable UUID id) throws Exception {
        MobileAppId mobileAppId = new MobileAppId(id);
        MobileApp mobileApp = checkMobileAppId(mobileAppId, Operation.DELETE);
        tbMobileAppService.delete(mobileApp, getCurrentUser());
    }

    private JsonNode getVisiblePages(MobileAppBundle mobileAppBundle) throws JsonProcessingException {
        if (mobileAppBundle != null && mobileAppBundle.getLayoutConfig() != null) {
            List<MobilePage> mobilePages = mobileAppBundle.getLayoutConfig().getPages()
                    .stream()
                    .filter(MobilePage::isVisible)
                    .collect(Collectors.toList());
            return JacksonUtil.toJsonNode(JacksonUtil.writeValueAsViewIgnoringNullFields(mobilePages, Views.Public.class));
        } else {
            return null;
        }
    }

}
