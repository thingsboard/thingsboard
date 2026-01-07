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
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class MobileAppBundleControllerTest extends AbstractControllerTest {

    static final TypeReference<PageData<MobileAppBundleInfo>> PAGE_DATA_MOBILE_APP_BUNDLE_TYPE_REF = new TypeReference<>() {
    };
    static final TypeReference<PageData<MobileApp>> PAGE_DATA_MOBILE_APP_TYPE_REF = new TypeReference<>() {
    };
    static final TypeReference<PageData<OAuth2ClientInfo>> PAGE_DATA_OAUTH2_CLIENT_TYPE_REF = new TypeReference<>() {
    };

    private MobileApp androidApp;
    private MobileApp iosApp;

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();

        androidApp = validMobileApp(TenantId.SYS_TENANT_ID, "my.android.package", PlatformType.ANDROID);
        androidApp = doPost("/api/mobile/app", androidApp, MobileApp.class);

        iosApp = validMobileApp(TenantId.SYS_TENANT_ID, "my.ios.package", PlatformType.IOS);
        iosApp = doPost("/api/mobile/app", iosApp, MobileApp.class);
    }

    @After
    public void tearDown() throws Exception {
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

        PageData<OAuth2ClientInfo> clients = doGetTypedWithPageLink("/api/oauth2/client/infos?", PAGE_DATA_OAUTH2_CLIENT_TYPE_REF, new PageLink(10, 0));
        for (OAuth2ClientInfo oAuth2ClientInfo : clients.getData()) {
            doDelete("/api/oauth2/client/" + oAuth2ClientInfo.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveMobileAppBundle() {
        MobileAppBundle mobileAppBundle = new MobileAppBundle();
        mobileAppBundle.setTitle("Test bundle");
        mobileAppBundle.setAndroidAppId(androidApp.getId());
        mobileAppBundle.setIosAppId(iosApp.getId());

        MobileAppBundle createdMobileAppBundle = doPost("/api/mobile/bundle", mobileAppBundle, MobileAppBundle.class);
        assertThat(createdMobileAppBundle.getAndroidAppId()).isEqualTo(androidApp.getId());
        assertThat(createdMobileAppBundle.getIosAppId()).isEqualTo(iosApp.getId());
    }

    @Test
    public void testSaveMobileAppBundleWithoutApps() throws Exception {
        MobileAppBundle mobileAppBundle = new MobileAppBundle();
        mobileAppBundle.setTitle("Test bundle");

        MobileAppBundle savedAppBundle = doPost("/api/mobile/bundle", mobileAppBundle, MobileAppBundle.class);
        MobileAppBundleInfo retrievedMobileAppBundleInfo = doGet("/api/mobile/bundle/info/{id}", MobileAppBundleInfo.class, savedAppBundle.getId().getId());
        assertThat(retrievedMobileAppBundleInfo).isEqualTo(new MobileAppBundleInfo(savedAppBundle, null, null, false,
                Collections.emptyList()));
    }


    @Test
    public void testUpdateMobileAppBundleOauth2Clients() throws Exception {
        MobileAppBundle mobileAppBundle = new MobileAppBundle();
        mobileAppBundle.setTitle("Test bundle");
        mobileAppBundle.setAndroidAppId(androidApp.getId());
        mobileAppBundle.setIosAppId(iosApp.getId());

        MobileAppBundle savedAppBundle = doPost("/api/mobile/bundle", mobileAppBundle, MobileAppBundle.class);

        OAuth2Client oAuth2Client = createOauth2Client(TenantId.SYS_TENANT_ID, "test google client");
        OAuth2Client savedOAuth2Client = doPost("/api/oauth2/client", oAuth2Client, OAuth2Client.class);

        OAuth2Client oAuth2Client2 = createOauth2Client(TenantId.SYS_TENANT_ID, "test facebook client");
        OAuth2Client savedOAuth2Client2 = doPost("/api/oauth2/client", oAuth2Client2, OAuth2Client.class);

        doPut("/api/mobile/bundle/" + savedAppBundle.getId() + "/oauth2Clients", List.of(savedOAuth2Client.getId().getId(), savedOAuth2Client2.getId().getId()));

        MobileAppBundleInfo retrievedMobileAppBundleInfo = doGet("/api/mobile/bundle/info/{id}", MobileAppBundleInfo.class, savedAppBundle.getId().getId());
        assertThat(retrievedMobileAppBundleInfo).isEqualTo(new MobileAppBundleInfo(savedAppBundle, androidApp.getPkgName(), iosApp.getPkgName(), false,
                Stream.of(new OAuth2ClientInfo(savedOAuth2Client), new OAuth2ClientInfo(savedOAuth2Client2))
                        .sorted(Comparator.comparing(OAuth2ClientInfo::getTitle)).collect(Collectors.toList())
        ));

        doPut("/api/mobile/bundle/" + savedAppBundle.getId() + "/oauth2Clients", List.of(savedOAuth2Client2.getId().getId()));
        MobileAppBundleInfo retrievedMobileAppInfo2 = doGet("/api/mobile/bundle/info/{id}", MobileAppBundleInfo.class, savedAppBundle.getId().getId());
        assertThat(retrievedMobileAppInfo2).isEqualTo(new MobileAppBundleInfo(savedAppBundle, androidApp.getPkgName(), iosApp.getPkgName(), false, List.of(new OAuth2ClientInfo(savedOAuth2Client2))));
    }

    @Test
    public void testCreateMobileAppBundleWithOauth2Clients() throws Exception {
        OAuth2Client oAuth2Client = createOauth2Client(TenantId.SYS_TENANT_ID, "test google client");
        OAuth2Client savedOAuth2Client = doPost("/api/oauth2/client", oAuth2Client, OAuth2Client.class);

        MobileAppBundle mobileAppBundle = new MobileAppBundle();
        mobileAppBundle.setTitle("Test bundle");
        mobileAppBundle.setAndroidAppId(androidApp.getId());
        mobileAppBundle.setIosAppId(iosApp.getId());

        MobileAppBundle savedMobileAppBundle = doPost("/api/mobile/bundle?oauth2ClientIds=" + savedOAuth2Client.getId().getId(), mobileAppBundle, MobileAppBundle.class);

        MobileAppBundleInfo retrievedMobileAppInfo = doGet("/api/mobile/bundle/info/{id}", MobileAppBundleInfo.class, savedMobileAppBundle.getId().getId());
        assertThat(retrievedMobileAppInfo).isEqualTo(new MobileAppBundleInfo(savedMobileAppBundle, androidApp.getPkgName(), iosApp.getPkgName(), false, List.of(new OAuth2ClientInfo(savedOAuth2Client))));
    }

    private MobileApp validMobileApp(TenantId tenantId, String mobileAppName, PlatformType platformType) {
        MobileApp mobileApp = new MobileApp();
        mobileApp.setTenantId(tenantId);
        mobileApp.setStatus(MobileAppStatus.DRAFT);
        mobileApp.setPkgName(mobileAppName);
        mobileApp.setPlatformType(platformType);
        mobileApp.setAppSecret(StringUtils.randomAlphanumeric(24));
        return mobileApp;
    }

}
