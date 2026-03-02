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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.StoreInfo;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.mobile.QrCodeSettingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.mobile.secret.MobileAppSecretService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.net.URI;
import java.net.URISyntaxException;

import static org.thingsboard.server.common.data.oauth2.PlatformType.ANDROID;
import static org.thingsboard.server.common.data.oauth2.PlatformType.IOS;
import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
public class QrCodeSettingsController extends BaseController {

    @Value("${cache.specs.mobileSecretKey.timeToLiveInMinutes:2}")
    private int mobileSecretKeyTtl;
    @Value("${mobileApp.domain:demo.thingsboard.io}")
    private String defaultAppDomain;

    public static final String ASSET_LINKS_PATTERN = "[{\n" +
            "  \"relation\": [\"delegate_permission/common.handle_all_urls\"],\n" +
            "  \"target\": {\n" +
            "    \"namespace\": \"android_app\",\n" +
            "    \"package_name\": \"%s\",\n" +
            "    \"sha256_cert_fingerprints\":\n" +
            "    [\"%s\"]\n" +
            "  }\n" +
            "}]";

    public static final String APPLE_APP_SITE_ASSOCIATION_PATTERN = "{\n" +
            "    \"applinks\": {\n" +
            "        \"apps\": [],\n" +
            "        \"details\": [\n" +
            "            {\n" +
            "                \"appID\": \"%s\",\n" +
            "                \"paths\": [ \"/api/noauth/qr\" ]\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}";

    public static final String SECRET = "secret";
    public static final String SECRET_PARAM_DESCRIPTION = "A string value representing short-lived secret key";
    public static final String DEEP_LINK_PATTERN = "https://%s/api/noauth/qr?secret=%s&ttl=%s";

    private final SystemSecurityService systemSecurityService;
    private final MobileAppSecretService mobileAppSecretService;
    private final QrCodeSettingService qrCodeSettingService;

    @ApiOperation(value = "Get associated android applications (getAssetLinks)")
    @GetMapping(value = "/.well-known/assetlinks.json")
    public ResponseEntity<JsonNode> getAssetLinks() {
        MobileApp mobileApp = qrCodeSettingService.findAppFromQrCodeSettings(TenantId.SYS_TENANT_ID, ANDROID);
        StoreInfo storeInfo = mobileApp != null ? mobileApp.getStoreInfo() : null;
        if (storeInfo != null && storeInfo.getSha256CertFingerprints() != null) {
            return ResponseEntity.ok(JacksonUtil.toJsonNode(String.format(ASSET_LINKS_PATTERN, mobileApp.getPkgName(), storeInfo.getSha256CertFingerprints())));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "Get associated ios applications (getAppleAppSiteAssociation)")
    @GetMapping(value = "/.well-known/apple-app-site-association")
    public ResponseEntity<JsonNode> getAppleAppSiteAssociation() {
        MobileApp mobileApp = qrCodeSettingService.findAppFromQrCodeSettings(TenantId.SYS_TENANT_ID, IOS);
        StoreInfo storeInfo = mobileApp != null ? mobileApp.getStoreInfo() : null;
        if (storeInfo != null && storeInfo.getAppId() != null) {
            return ResponseEntity.ok(JacksonUtil.toJsonNode(String.format(APPLE_APP_SITE_ASSOCIATION_PATTERN, storeInfo.getAppId())));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "Create Or Update the Mobile application settings (saveMobileAppSettings)",
            notes = "The request payload contains configuration for android/iOS applications and platform qr code widget settings." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PostMapping(value = "/api/mobile/qr/settings")
    public QrCodeSettings saveQrCodeSettings(@Parameter(description = "A JSON value representing the mobile apps configuration")
                                             @RequestBody QrCodeSettings qrCodeSettings) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.MOBILE_APP_SETTINGS, Operation.WRITE);
        qrCodeSettings.setTenantId(getTenantId());
        return qrCodeSettingService.saveQrCodeSettings(currentUser.getTenantId(), qrCodeSettings);
    }

    @ApiOperation(value = "Get Mobile application settings (getMobileAppSettings)",
            notes = "The response payload contains configuration for android/iOS applications and platform qr code widget settings." + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/api/mobile/qr/settings")
    public QrCodeSettings getQrCodeSettings() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.MOBILE_APP_SETTINGS, Operation.READ);
        return qrCodeSettingService.findQrCodeSettings(TenantId.SYS_TENANT_ID);
    }

    @ApiOperation(value = "Get the deep link to the associated mobile application (getMobileAppDeepLink)",
            notes = "Fetch the url that takes user to linked mobile application " + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/api/mobile/qr/deepLink", produces = "text/plain")
    public String getMobileAppDeepLink(HttpServletRequest request) throws ThingsboardException, URISyntaxException {
        String secret = mobileAppSecretService.generateMobileAppSecret(getCurrentUser());
        String baseUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, null, request);
        String platformDomain = new URI(baseUrl).getHost();
        QrCodeSettings qrCodeSettings = qrCodeSettingService.findQrCodeSettings(TenantId.SYS_TENANT_ID);
        String appDomain = qrCodeSettings.isUseDefaultApp() ? defaultAppDomain : platformDomain;
        String deepLink = String.format(DEEP_LINK_PATTERN, appDomain, secret, mobileSecretKeyTtl);
        if (!appDomain.equals(platformDomain)) {
            deepLink = deepLink + "&host=" + baseUrl;
        }
        return "\"" + deepLink + "\"";
    }

    @ApiOperation(value = "Get User Token (getUserTokenByMobileSecret)",
            notes = "Returns the token of the User based on the provided secret key.")
    @GetMapping(value = "/api/noauth/qr/{secret}")
    public JwtPair getUserTokenByMobileSecret(@Parameter(description = SECRET_PARAM_DESCRIPTION)
                                              @PathVariable(SECRET) String secret) throws ThingsboardException {
        checkParameter(SECRET, secret);
        return mobileAppSecretService.getJwtPair(secret);
    }

    @GetMapping(value = "/api/noauth/qr")
    public ResponseEntity<?> getApplicationRedirect(@RequestHeader(value = "User-Agent") String userAgent) {
        QrCodeSettings qrCodeSettings = qrCodeSettingService.findQrCodeSettings(TenantId.SYS_TENANT_ID);
        if (userAgent.contains("Android") && qrCodeSettings.isAndroidEnabled()) {
            String googlePlayLink = qrCodeSettings.getGooglePlayLink();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", googlePlayLink)
                    .build();
        } else if ((userAgent.contains("iPhone") || userAgent.contains("iPad")) && qrCodeSettings.isIosEnabled()) {
            String appStoreLink = qrCodeSettings.getAppStoreLink();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", appStoreLink)
                    .build();
        } else {
            return response(HttpStatus.NOT_FOUND);
        }
    }

}
