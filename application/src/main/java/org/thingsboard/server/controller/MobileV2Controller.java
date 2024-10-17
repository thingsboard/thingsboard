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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.mobile.LoginMobileInfo;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.app.MobileAppVersionInfo;
import org.thingsboard.server.common.data.mobile.UserMobileInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
public class MobileV2Controller extends BaseController {

    @ApiOperation(value = "Get mobile app login info (getLoginMobileInfo)")
    @GetMapping(value = "/api/noauth/mobile")
    public LoginMobileInfo getLoginMobileInfo(@Parameter(description = "Mobile application package name")
                                              @RequestParam String pkgName,
                                              @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
                                              @RequestParam PlatformType platform) {
        List<OAuth2ClientLoginInfo> oauth2Clients = oAuth2ClientService.findOAuth2ClientLoginInfosByMobilePkgNameAndPlatformType(pkgName, platform);
        MobileApp mobileApp = mobileAppService.findMobileAppByPkgNameAndPlatformType(pkgName, platform);
        return new LoginMobileInfo(oauth2Clients, mobileApp != null ? mobileApp.getVersionInfo() : null);
    }

    @ApiOperation(value = "Get user mobile app basic info (getUserMobileInfo)", notes = AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/api/mobile")
    public UserMobileInfo getUserMobileInfo(@Parameter(description = "Mobile application package name")
                                            @RequestParam String pkgName,
                                            @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
                                            @RequestParam PlatformType platform) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        HomeDashboardInfo homeDashboardInfo = securityUser.isSystemAdmin() ? null : getHomeDashboardInfo(securityUser, user.getAdditionalInfo());
        MobileAppBundle mobileAppBundle = mobileAppBundleService.findMobileAppBundleByPkgNameAndPlatform(securityUser.getTenantId(), pkgName, platform);
        return new UserMobileInfo(user, homeDashboardInfo, mobileAppBundle != null ? mobileAppBundle.getLayoutConfig() : null);
    }

    @ApiOperation(value = "Get mobile app version info (getMobileVersionInfo)")
    @GetMapping(value = "/api/mobile/versionInfo")
    public MobileAppVersionInfo getMobileVersionInfo(@Parameter(description = "Mobile application package name")
                                                     @RequestParam String pkgName,
                                                     @Parameter(description = "Platform type", schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
                                                     @RequestParam PlatformType platform) {
        MobileApp mobileApp = mobileAppService.findMobileAppByPkgNameAndPlatformType(pkgName, platform);
        return mobileApp != null ? mobileApp.getVersionInfo() : null;
    }

}
