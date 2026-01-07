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
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class MobileAppControllerTest extends AbstractControllerTest {

    static final TypeReference<PageData<MobileApp>> PAGE_DATA_MOBILE_APP_TYPE_REF = new TypeReference<>() {
    };

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();
    }

    @After
    public void tearDown() throws Exception {
        PageData<MobileApp> pageData = doGetTypedWithPageLink("/api/mobile/app?", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        for (MobileApp mobileApp : pageData.getData()) {
            doDelete("/api/mobile/app/" + mobileApp.getId().getId())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveMobileApp() throws Exception {
        PageData<MobileApp> pageData = doGetTypedWithPageLink("/api/mobile/app?", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        MobileApp mobileApp = validMobileApp("my.test.package", PlatformType.ANDROID);
        MobileApp savedMobileApp = doPost("/api/mobile/app", mobileApp, MobileApp.class);

        PageData<MobileApp> pageData2 = doGetTypedWithPageLink("/api/mobile/app?", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData2.getData()).hasSize(1);
        assertThat(pageData2.getData().get(0)).isEqualTo(savedMobileApp);

        MobileApp retrievedMobileAppInfo = doGet("/api/mobile/app/{id}", MobileApp.class, savedMobileApp.getId().getId());
        assertThat(retrievedMobileAppInfo).isEqualTo(savedMobileApp);

        doDelete("/api/mobile/app/" + savedMobileApp.getId().getId());
        doGet("/api/mobile/app/{id}", savedMobileApp.getId().getId())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveMobileAppWithShortAppSecret() throws Exception {
        MobileApp mobileApp = validMobileApp( "mobileApp.ce", PlatformType.ANDROID);
        mobileApp.setAppSecret("short");
        doPost("/api/mobile/app", mobileApp)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("appSecret must be at least 16 and max 2048 characters")));
    }

    @Test
    public void testGetTenantAppsByPlatformTypeSaveMobileApp() throws Exception {
        MobileApp androidApp = doPost("/api/mobile/app", validMobileApp("android.1", PlatformType.ANDROID), MobileApp.class);
        MobileApp androidApp2 = doPost("/api/mobile/app", validMobileApp("android.2", PlatformType.ANDROID), MobileApp.class);
        MobileApp iosApp = doPost("/api/mobile/app", validMobileApp("ios.1", PlatformType.IOS), MobileApp.class);

        PageData<MobileApp> pageData = doGetTypedWithPageLink("/api/mobile/app?", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).hasSize(3);
        assertThat(pageData.getData()).containsExactlyInAnyOrder(androidApp, androidApp2, iosApp);

        PageData<MobileApp> androidPageData = doGetTypedWithPageLink("/api/mobile/app?platformType=ANDROID&", PAGE_DATA_MOBILE_APP_TYPE_REF, new PageLink(10, 0));
        assertThat(androidPageData.getData()).hasSize(2);
        assertThat(androidPageData.getData()).containsExactlyInAnyOrder(androidApp, androidApp2);
    }

    private MobileApp validMobileApp(String mobileAppName, PlatformType platformType) {
        MobileApp mobileApp = new MobileApp();
        mobileApp.setPkgName(mobileAppName);
        mobileApp.setAppSecret(StringUtils.randomAlphanumeric(24));
        mobileApp.setPlatformType(platformType);
        mobileApp.setStatus(MobileAppStatus.DRAFT);
        return mobileApp;
    }

}
