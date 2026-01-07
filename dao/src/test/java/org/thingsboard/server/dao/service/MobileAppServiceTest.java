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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.mobile.MobileAppService;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class MobileAppServiceTest extends AbstractServiceTest {

    @Autowired
    protected MobileAppService mobileAppService;

    @Autowired
    protected OAuth2ClientService oAuth2ClientService;

    @After
    public void after() {
        mobileAppService.deleteByTenantId(TenantId.SYS_TENANT_ID);
        oAuth2ClientService.deleteByTenantId(TenantId.SYS_TENANT_ID);
    }

    @Test
    public void testSaveMobileApp() {
        MobileApp MobileApp = validMobileApp(SYSTEM_TENANT_ID, "mobileApp.ce", PlatformType.IOS);
        MobileApp savedMobileApp = mobileAppService.saveMobileApp(SYSTEM_TENANT_ID, MobileApp);

        MobileApp retrievedMobileApp = mobileAppService.findMobileAppById(savedMobileApp.getTenantId(), savedMobileApp.getId());
        assertThat(retrievedMobileApp).isEqualTo(savedMobileApp);

        // update MobileApp name
        savedMobileApp.setPkgName("mobileApp.pe");
        MobileApp updatedMobileApp = mobileAppService.saveMobileApp(SYSTEM_TENANT_ID, savedMobileApp);

        MobileApp retrievedMobileApp2 = mobileAppService.findMobileAppById(savedMobileApp.getTenantId(), savedMobileApp.getId());
        assertThat(retrievedMobileApp2).isEqualTo(updatedMobileApp);

        //delete MobileApp
        mobileAppService.deleteMobileAppById(SYSTEM_TENANT_ID, savedMobileApp.getId());
        assertThat(mobileAppService.findMobileAppById(SYSTEM_TENANT_ID, savedMobileApp.getId())).isNull();
    }

    @Test
    public void testGetTenantMobileApps() {
        List<MobileApp> mobileApps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MobileApp oAuth2Client = validMobileApp(SYSTEM_TENANT_ID, StringUtils.randomAlphabetic(5), PlatformType.ANDROID);
            MobileApp savedOauth2Client = mobileAppService.saveMobileApp(SYSTEM_TENANT_ID, oAuth2Client);
            mobileApps.add(savedOauth2Client);
        }
        PageData<MobileApp> retrieved = mobileAppService.findMobileAppsByTenantId(TenantId.SYS_TENANT_ID, null, new PageLink(10, 0));
        assertThat(retrieved.getData()).containsOnlyOnceElementsOf(mobileApps);
    }

    private MobileApp validMobileApp(TenantId tenantId, String mobileAppName, PlatformType platformType) {
        MobileApp MobileApp = new MobileApp();
        MobileApp.setTenantId(tenantId);
        MobileApp.setPkgName(mobileAppName);
        MobileApp.setStatus(MobileAppStatus.DRAFT);
        MobileApp.setAppSecret(StringUtils.randomAlphanumeric(24));
        MobileApp.setPlatformType(platformType);
        return MobileApp;
    }
}
