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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.mobile.MobileAppSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.qr.QRService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.net.URI;
import java.net.URISyntaxException;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class QRCodeController extends BaseController {

    public static final String SECRET = "secret";
    public static final String SECRET_PARAM_DESCRIPTION = "A string value representing short-live secret key";
    public static final String DEFAULT_APP_DOMAIN = "demo.thingsboard.io";
    public static final String DEEP_LINK_PATTERN = "https://%s/api/noauth/qr?secret=%s";
    private final QRService qrService;
    private final SystemSecurityService systemSecurityService;

    private final MobileAppSettingsService mobileAppSettingsService;

    @ApiOperation(value = "Get the deep link to the associated mobile application (getQRCodeDeepLink)",
            notes = "Fetch the url that takes user to associated mobile application ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/qr/deepLink", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public String getQRCodeDeepLink(HttpServletRequest request) throws ThingsboardException, URISyntaxException {
        SecurityUser currentUser = getCurrentUser();
        String secret = qrService.generateSecret(currentUser);

        String baseUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
        String platformDomain = new URI(baseUrl).getHost();
        JsonNode mobileAppSettings = mobileAppSettingsService.getMobileAppSettings(TenantId.SYS_TENANT_ID).getSettings();
        String appDomain;
        if (mobileAppSettings != null && mobileAppSettings.get("useDefault") != null
                && !mobileAppSettings.get("useDefault").asBoolean()) {
            appDomain = platformDomain;
        } else {
            appDomain = DEFAULT_APP_DOMAIN;
        }
        String deepLink = String.format(DEEP_LINK_PATTERN, appDomain, secret);
        if (!appDomain.equals(platformDomain)) {
            deepLink = deepLink + "&host=" + baseUrl;
        }
        return deepLink;
    }

    @ApiOperation(value = "Get User Token (getUserToken)",
            notes = "Returns the token of the User based on the provided secret key.")
    @RequestMapping(value = "/noauth/qr/{secret}", method = RequestMethod.GET)
    @ResponseBody
    public JwtPair getUserToken(@Parameter(description = SECRET_PARAM_DESCRIPTION)
            @PathVariable(SECRET) String secret) throws ThingsboardException {
        checkParameter(SECRET, secret);
        return qrService.getJwtPair(secret);
    }

}
