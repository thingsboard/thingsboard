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
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.mobile.AndroidConfig;
import org.thingsboard.server.common.data.mobile.IosConfig;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.common.data.mobile.QRCodeConfig;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class MobileApplicationControllerTest extends AbstractControllerTest {

    @Value("${cache.specs.mobileSecretKey.timeToLiveInMinutes:2}")
    private int mobileSecretKeyTtl;
    private static final String ANDROID_PACKAGE_NAME = "testAppPackage";
    private static final String ANDROID_APP_SHA256 = "DF:28:32:66:8B:A7:D3:EC:7D:73:CF:CC";
    private static final String APPLE_APP_ID = "testId";
    private static final String TEST_LABEL = "Test label";

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();

        QRCodeConfig qrCodeConfig = new QRCodeConfig();
        qrCodeConfig.setQrCodeLabel(TEST_LABEL);

        MobileAppSettings mobileAppSettings = new MobileAppSettings();
        mobileAppSettings.setUseDefault(true);
        AndroidConfig androidConfig = AndroidConfig.builder()
                .appPackage(ANDROID_PACKAGE_NAME)
                .sha256CertFingerprints(ANDROID_APP_SHA256)
                .enabled(true)
                .build();

        IosConfig iosConfig = IosConfig.builder()
                .appId(APPLE_APP_ID)
                .enabled(true)
                .build();
        mobileAppSettings.setAndroidConfig(androidConfig);
        mobileAppSettings.setIosConfig(iosConfig);
        mobileAppSettings.setQrCodeConfig(qrCodeConfig);

        doPost("/api/mobile/app/settings", mobileAppSettings);
    }

    @Test
    public void testSaveMobileAppSettings() throws Exception {
        loginSysAdmin();
        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        assertThat(mobileAppSettings.getQrCodeConfig().getQrCodeLabel()).isEqualTo(TEST_LABEL);
        assertThat(mobileAppSettings.isUseDefault()).isTrue();

        mobileAppSettings.setUseDefault(false);

        doPost("/api/mobile/app/settings", mobileAppSettings)
                .andExpect(status().isOk());

        MobileAppSettings updatedMobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        assertThat(updatedMobileAppSettings.isUseDefault()).isFalse();
    }

    @Test
    public void testGetApplicationAssociations() throws Exception {
        JsonNode assetLinks = doGet("/.well-known/assetlinks.json", JsonNode.class);
        assertThat(assetLinks.get(0).get("target").get("package_name").asText()).isEqualTo(ANDROID_PACKAGE_NAME);
        assertThat(assetLinks.get(0).get("target").get("sha256_cert_fingerprints").get(0).asText()).isEqualTo(ANDROID_APP_SHA256);

        JsonNode appleAssociation = doGet("/.well-known/apple-app-site-association", JsonNode.class);
        assertThat(appleAssociation.get("applinks").get("details").get(0).get("appID").asText()).isEqualTo(APPLE_APP_ID);
    }

    @Test
    public void testGetMobileDeepLink() throws Exception {
        loginSysAdmin();
        String deepLink = doGet("/api/mobile/deepLink", String.class);

        Pattern expectedPattern = Pattern.compile("https://([^/]+)/api/noauth/qr\\?secret=([^&]+)&ttl=([^&]+)&host=([^&]+)");
        Matcher parsedDeepLink = expectedPattern.matcher(deepLink);
        assertThat(parsedDeepLink.matches()).isTrue();
        String appHost = parsedDeepLink.group(1);
        String secret = parsedDeepLink.group(2);
        String ttl = parsedDeepLink.group(3);
        assertThat(appHost).isEqualTo("demo.thingsboard.io");
        assertThat(ttl).isEqualTo(String.valueOf(mobileSecretKeyTtl));

        JwtPair jwtPair = doGet("/api/noauth/qr/" + secret, JwtPair.class);
        assertThat(jwtPair).isNotNull();

        loginTenantAdmin();
        String tenantDeepLink = doGet("/api/mobile/deepLink", String.class);
        Matcher tenantParsedDeepLink = expectedPattern.matcher(tenantDeepLink);
        assertThat(tenantParsedDeepLink.matches()).isTrue();
        String tenantSecret = tenantParsedDeepLink.group(2);

        JwtPair tenantJwtPair =  doGet("/api/noauth/qr/" + tenantSecret, JwtPair.class);
        assertThat(tenantJwtPair).isNotNull();

        loginCustomerUser();
        String customerDeepLink = doGet("/api/mobile/deepLink", String.class);
        Matcher customerParsedDeepLink = expectedPattern.matcher(customerDeepLink);
        assertThat(customerParsedDeepLink.matches()).isTrue();
        String customerSecret = customerParsedDeepLink.group(2);

        JwtPair customerJwtPair = doGet("/api/noauth/qr/" + customerSecret, JwtPair.class);
        assertThat(customerJwtPair).isNotNull();

        // update mobile setting to use custom one
        loginSysAdmin();
        MobileAppSettings mobileAppSettings = doGet("/api/mobile/app/settings", MobileAppSettings.class);
        mobileAppSettings.setUseDefault(false);
        doPost("/api/mobile/app/settings", mobileAppSettings);

        String customAppDeepLink = doGet("/api/mobile/deepLink", String.class);
        Pattern customAppExpectedPattern = Pattern.compile("https://([^/]+)/api/noauth/qr\\?secret=([^&]+)&ttl=([^&]+)");
        Matcher customAppParsedDeepLink = customAppExpectedPattern.matcher(customAppDeepLink);
        assertThat(customAppParsedDeepLink.matches()).isTrue();
        assertThat(customAppParsedDeepLink.group(1)).isEqualTo("localhost");

        loginTenantAdmin();
        String tenantCustomAppDeepLink = doGet("/api/mobile/deepLink", String.class);
        Matcher tenantCustomAppParsedDeepLink = customAppExpectedPattern.matcher(tenantCustomAppDeepLink);
        assertThat(tenantCustomAppParsedDeepLink.matches()).isTrue();
        assertThat(tenantCustomAppParsedDeepLink.group(1)).isEqualTo("localhost");

        loginCustomerUser();
        String customerCustomAppDeepLink = doGet("/api/mobile/deepLink", String.class);
        Matcher customerCustomAppParsedDeepLink = customAppExpectedPattern.matcher(customerCustomAppDeepLink);
        assertThat(customerCustomAppParsedDeepLink.matches()).isTrue();
        assertThat(customerCustomAppParsedDeepLink.group(1)).isEqualTo("localhost");


    }
}
