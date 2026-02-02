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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QRCodeConfig;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.common.data.mobile.app.StoreInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class QrCodeSettingsControllerTest extends AbstractControllerTest {

    @Value("${cache.specs.mobileSecretKey.timeToLiveInMinutes:2}")
    private int mobileSecretKeyTtl;

    static final TypeReference<PageData<MobileAppBundleInfo>> PAGE_DATA_MOBILE_APP_BUNDLE_TYPE_REF = new TypeReference<>() {
    };
    static final TypeReference<PageData<MobileApp>> PAGE_DATA_MOBILE_APP_TYPE_REF = new TypeReference<>() {
    };
    private static final String ANDROID_PACKAGE_NAME = "testAppPackage";
    private static final String ANDROID_APP_SHA256 = "DF:28:32:66:8B:A7:D3:EC:7D:73:CF:CC";
    private static final String ANDROID_STORE_LINK = "https://store.link.com";
    private static final String APPLE_APP_ID = "testId";
    private static final String TEST_LABEL = "Test label";
    private static final String IOS_STORE_LINK = "https://store.link.com";

    private MobileAppBundle mobileAppBundle;

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();

        MobileApp androidApp = validMobileApp( "my.android.package", PlatformType.ANDROID);
        StoreInfo androidStoreInfo = StoreInfo.builder()
                .sha256CertFingerprints(ANDROID_APP_SHA256)
                .storeLink(ANDROID_STORE_LINK)
                .build();
        androidApp.setStoreInfo(androidStoreInfo);
        MobileApp savedAndroidApp = doPost("/api/mobile/app", androidApp, MobileApp.class);

        MobileApp iosApp = validMobileApp( "my.ios.package", PlatformType.IOS);
        StoreInfo iosQrCodeConfig = StoreInfo.builder()
                .appId(APPLE_APP_ID)
                .storeLink(IOS_STORE_LINK)
                .build();
        iosApp.setStoreInfo(iosQrCodeConfig);
        MobileApp savedIosApp = doPost("/api/mobile/app", iosApp, MobileApp.class);

        mobileAppBundle = new MobileAppBundle();
        mobileAppBundle.setTitle("Test bundle");
        mobileAppBundle.setAndroidAppId(savedAndroidApp.getId());
        mobileAppBundle.setIosAppId(savedIosApp.getId());

        mobileAppBundle = doPost("/api/mobile/bundle", mobileAppBundle, MobileAppBundle.class);

        QrCodeSettings qrCodeSettings = doGet("/api/mobile/qr/settings", QrCodeSettings.class);
        QRCodeConfig qrCodeConfig = new QRCodeConfig();
        qrCodeConfig.setQrCodeLabel(TEST_LABEL);
        qrCodeSettings.setUseDefaultApp(true);
        qrCodeSettings.setMobileAppBundleId(null);
        qrCodeSettings.setQrCodeConfig(qrCodeConfig);

        doPost("/api/mobile/qr/settings", qrCodeSettings)
                .andExpect(status().isOk());
    }

    @After
    public void tearDown() throws Exception {
        loginSysAdmin();
        PageData<MobileAppBundleInfo> pageData2 = doGetTypedWithPageLink("/api/mobile/bundle/infos?", PAGE_DATA_MOBILE_APP_BUNDLE_TYPE_REF, new PageLink(10, 0));
        for (MobileAppBundleInfo appBundleInfo : pageData2.getData()) {
            doDelete("/api/mobile/bundle/" + appBundleInfo.getId().getId())
                    .andExpect(status().isOk());
        }
        PageData<MobileApp> pageData = doGetTypedWithPageLink("/api/mobile/app?", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        for (MobileApp mobileApp : pageData.getData()) {
            doDelete("/api/mobile/app/" + mobileApp.getId().getId())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveQrCodeSettings() throws Exception {
        loginSysAdmin();
        QrCodeSettings qrCodeSettings = doGet("/api/mobile/qr/settings", QrCodeSettings.class);
        assertThat(qrCodeSettings.getQrCodeConfig().getQrCodeLabel()).isEqualTo(TEST_LABEL);
        assertThat(qrCodeSettings.isUseDefaultApp()).isTrue();

        qrCodeSettings.setUseDefaultApp(false);
        qrCodeSettings.setMobileAppBundleId(mobileAppBundle.getId());

        doPost("/api/mobile/qr/settings", qrCodeSettings)
                .andExpect(status().isOk());

        QrCodeSettings updatedQrCodeSettings = doGet("/api/mobile/qr/settings", QrCodeSettings.class);
        assertThat(updatedQrCodeSettings.isUseDefaultApp()).isFalse();
    }

    @Test
    public void testShouldNotSaveQrCodeSettingsWithoutRequiredConfig() throws Exception {
        loginSysAdmin();
        QrCodeSettings qrCodeSettings = doGet("/api/mobile/qr/settings", QrCodeSettings.class);

        qrCodeSettings.setUseDefaultApp(false);
        qrCodeSettings.setQrCodeConfig(null);
        qrCodeSettings.setMobileAppBundleId(null);

        doPost("/api/mobile/qr/settings", qrCodeSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Validation error: qrCodeConfig must not be null")));

        qrCodeSettings.setQrCodeConfig(QRCodeConfig.builder().showOnHomePage(false).build());
        doPost("/api/mobile/qr/settings", qrCodeSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Mobile app bundle is required to use custom application!")));

        qrCodeSettings.setMobileAppBundleId(mobileAppBundle.getId());
        doPost("/api/mobile/qr/settings", qrCodeSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testShouldSaveQrCodeSettingsForDefaultApp() throws Exception {
        loginSysAdmin();
        QrCodeSettings qrCodeSettings = doGet("/api/mobile/qr/settings", QrCodeSettings.class);
        qrCodeSettings.setUseDefaultApp(true);
        qrCodeSettings.setMobileAppBundleId(null);

        doPost("/api/mobile/qr/settings", qrCodeSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testGetApplicationAssociations() throws Exception {
        loginSysAdmin();
        QrCodeSettings qrCodeSettings = doGet("/api/mobile/qr/settings", QrCodeSettings.class);
        qrCodeSettings.setUseDefaultApp(true);
        qrCodeSettings.setMobileAppBundleId(mobileAppBundle.getId());
        doPost("/api/mobile/qr/settings", qrCodeSettings)
                .andExpect(status().isOk());

        JsonNode assetLinks = doGet("/.well-known/assetlinks.json", JsonNode.class);
        assertThat(assetLinks.get(0).get("target").get("package_name").asText()).isEqualTo("my.android.package");
        assertThat(assetLinks.get(0).get("target").get("sha256_cert_fingerprints").get(0).asText()).isEqualTo(ANDROID_APP_SHA256);

        JsonNode appleAssociation = doGet("/.well-known/apple-app-site-association", JsonNode.class);
        assertThat(appleAssociation.get("applinks").get("details").get(0).get("appID").asText()).isEqualTo(APPLE_APP_ID);
    }

    @Test
    public void testGetMobileDeepLink() throws Exception {
        loginSysAdmin();
        String deepLink = doGet("/api/mobile/qr/deepLink", String.class);

        Pattern expectedPattern = Pattern.compile("\"https://([^/]+)/api/noauth/qr\\?secret=([^&]+)&ttl=([^&]+)&host=([^&]+)\"");
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
        String tenantDeepLink = doGet("/api/mobile/qr/deepLink", String.class);
        Matcher tenantParsedDeepLink = expectedPattern.matcher(tenantDeepLink);
        assertThat(tenantParsedDeepLink.matches()).isTrue();
        String tenantSecret = tenantParsedDeepLink.group(2);

        JwtPair tenantJwtPair =  doGet("/api/noauth/qr/" + tenantSecret, JwtPair.class);
        assertThat(tenantJwtPair).isNotNull();

        loginCustomerUser();
        String customerDeepLink = doGet("/api/mobile/qr/deepLink", String.class);
        Matcher customerParsedDeepLink = expectedPattern.matcher(customerDeepLink);
        assertThat(customerParsedDeepLink.matches()).isTrue();
        String customerSecret = customerParsedDeepLink.group(2);

        JwtPair customerJwtPair = doGet("/api/noauth/qr/" + customerSecret, JwtPair.class);
        assertThat(customerJwtPair).isNotNull();

        // update mobile setting to use custom one
        loginSysAdmin();
        QrCodeSettings qrCodeSettings = doGet("/api/mobile/qr/settings", QrCodeSettings.class);
        qrCodeSettings.setUseDefaultApp(false);
        qrCodeSettings.setMobileAppBundleId(mobileAppBundle.getId());
        doPost("/api/mobile/qr/settings", qrCodeSettings);

        String customAppDeepLink = doGet("/api/mobile/qr/deepLink", String.class);
        Pattern customAppExpectedPattern = Pattern.compile("\"https://([^/]+)/api/noauth/qr\\?secret=([^&]+)&ttl=([^&]+)\"");
        Matcher customAppParsedDeepLink = customAppExpectedPattern.matcher(customAppDeepLink);
        assertThat(customAppParsedDeepLink.matches()).isTrue();
        assertThat(customAppParsedDeepLink.group(1)).isEqualTo("localhost");

        loginTenantAdmin();
        String tenantCustomAppDeepLink = doGet("/api/mobile/qr/deepLink", String.class);
        Matcher tenantCustomAppParsedDeepLink = customAppExpectedPattern.matcher(tenantCustomAppDeepLink);
        assertThat(tenantCustomAppParsedDeepLink.matches()).isTrue();
        assertThat(tenantCustomAppParsedDeepLink.group(1)).isEqualTo("localhost");

        loginCustomerUser();
        String customerCustomAppDeepLink = doGet("/api/mobile/qr/deepLink", String.class);
        Matcher customerCustomAppParsedDeepLink = customAppExpectedPattern.matcher(customerCustomAppDeepLink);
        assertThat(customerCustomAppParsedDeepLink.matches()).isTrue();
        assertThat(customerCustomAppParsedDeepLink.group(1)).isEqualTo("localhost");
    }

    private MobileApp validMobileApp(String mobileAppName, PlatformType platformType) {
        MobileApp mobileApp = new MobileApp();
        mobileApp.setTenantId(tenantId);
        mobileApp.setStatus(MobileAppStatus.PUBLISHED);
        mobileApp.setPkgName(mobileAppName);
        mobileApp.setPlatformType(platformType);
        mobileApp.setAppSecret(StringUtils.randomAlphanumeric(24));
        StoreInfo storeInfo = StoreInfo.builder()
                .storeLink("https://play.google/test")
                .sha256CertFingerprints(ANDROID_APP_SHA256)
                .appId("test.app.id").build();
        mobileApp.setStoreInfo(storeInfo);
        return mobileApp;
    }
}
