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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.mobile.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.MobileLoginInfo;
import org.thingsboard.server.common.data.mobile.MobileUserInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
public class MobileV2Controller extends BaseController {

    @GetMapping(value = "/api/noauth/mobile")
    public MobileLoginInfo getMobileUserLoginSettings(@Parameter(description = "Mobile application package name")
                                                      @RequestParam String pkgName,
                                                      @Parameter(description = "Platform type",
                                                              schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
                                                      @RequestParam PlatformType platform) {
        List<OAuth2ClientLoginInfo> oauth2Clients = oAuth2ClientService.findOAuth2ClientLoginInfosByMobilePkgNameAndPlatformType(pkgName, platform);
        return new MobileLoginInfo(oauth2Clients);
    }

    @GetMapping(value = "/api/auth/mobile")
    public MobileUserInfo getMobileUserSettings(@Parameter(description = "Mobile application package name")
                                                @RequestParam String pkgName,
                                                @Parameter(description = "Platform type",
                                                        schema = @Schema(allowableValues = {"ANDROID", "IOS"}))
                                                @RequestParam PlatformType platform) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        HomeDashboardInfo homeDashboardInfo = getHomeDashboardInfo(securityUser, user.getAdditionalInfo());
        MobileAppBundle mobileAppBundle = mobileAppBundleService.findMobileAppBundleByPkgNameAndPlatform(securityUser.getTenantId(), pkgName, platform);
        return new MobileUserInfo(user, homeDashboardInfo, mobileAppBundle != null ? mobileAppBundle.getLayoutConfig() : null);
    }

}
