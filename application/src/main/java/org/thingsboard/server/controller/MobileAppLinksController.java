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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.mobile.MobileAppSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
public class MobileAppLinksController extends BaseController {

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
            "                \"paths\": [ \"*\" ]\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}";


    private final MobileAppSettingsService mobileAppSettingsService;


    @ApiOperation(value = "Get associated android applications (getAssetLinks)")
    @RequestMapping(value = "/.well-known/assetlinks.json", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<JsonNode> getAssetLinks() {
        MobileAppSettings mobileAppSettings = mobileAppSettingsService.getMobileAppSettings(TenantId.SYS_TENANT_ID);
        if (mobileAppSettings != null && mobileAppSettings.getAppPackage() != null && mobileAppSettings.getSha256CertFingerprints() != null) {
            return ResponseEntity.ok(JacksonUtil.toJsonNode(String.format(ASSET_LINKS_PATTERN, mobileAppSettings.getAppPackage(), mobileAppSettings.getSha256CertFingerprints())));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "Get associated ios applications (getAppleAppSiteAssociation)")
    @RequestMapping(value = "/.well-known/apple-app-site-association", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<JsonNode> getAppleAppSiteAssociation() {
        MobileAppSettings mobileAppSettings = mobileAppSettingsService.getMobileAppSettings(TenantId.SYS_TENANT_ID);
        if (mobileAppSettings != null && mobileAppSettings.getAppId() != null) {
            return ResponseEntity.ok(JacksonUtil.toJsonNode(String.format(APPLE_APP_SITE_ASSOCIATION_PATTERN, mobileAppSettings.getAppId())));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
