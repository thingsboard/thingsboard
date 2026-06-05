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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.model.MobileApp;
import org.thingsboard.client.model.MobileAppBundle;
import org.thingsboard.client.model.MobileAppBundleInfo;
import org.thingsboard.client.model.MobileAppStatus;
import org.thingsboard.client.model.PageDataMobileApp;
import org.thingsboard.client.model.PageDataMobileAppBundleInfo;
import org.thingsboard.client.model.PlatformType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class MobileAppApiClientTest extends AbstractApiClientTest {

    @Test
    public void testMobileAppLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<MobileApp> createdApps = new ArrayList<>();

        // create 3 Android apps
        for (int i = 0; i < 3; i++) {
            MobileApp app = new MobileApp();
            app.setPkgName("com.test.android." + timestamp + "." + i);
            app.setTitle(TEST_PREFIX + "AndroidApp_" + timestamp + "_" + i);
            app.setAppSecret("secret_android_" + timestamp + "_" + i);
            app.setPlatformType(PlatformType.ANDROID);
            app.setStatus(MobileAppStatus.DRAFT);

            MobileApp created = client.saveMobileApp(app);
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(app.getPkgName(), created.getPkgName());
            assertEquals(PlatformType.ANDROID, created.getPlatformType());
            assertEquals(MobileAppStatus.DRAFT, created.getStatus());

            createdApps.add(created);
        }

        // create 2 iOS apps
        for (int i = 0; i < 2; i++) {
            MobileApp app = new MobileApp();
            app.setPkgName("com.test.ios." + timestamp + "." + i);
            app.setTitle(TEST_PREFIX + "IosApp_" + timestamp + "_" + i);
            app.setAppSecret("secret_ios_" + timestamp + "_" + i);
            app.setPlatformType(PlatformType.IOS);
            app.setStatus(MobileAppStatus.DRAFT);

            MobileApp created = client.saveMobileApp(app);
            assertNotNull(created);
            createdApps.add(created);
        }

        // list all tenant mobile apps
        PageDataMobileApp allApps = client.getTenantMobileApps(100, 0, null,
                null, null, null);
        assertNotNull(allApps);
        assertEquals(5, allApps.getData().size());

        // list with platform type filter
        PageDataMobileApp androidApps = client.getTenantMobileApps(100, 0, PlatformType.ANDROID,
                null, null, null);
        assertEquals(3, androidApps.getData().size());

        PageDataMobileApp iosApps = client.getTenantMobileApps(100, 0, PlatformType.IOS,
                null, null, null);
        assertEquals(2, iosApps.getData().size());

        // get mobile app by id
        MobileApp searchApp = createdApps.get(1);
        MobileApp fetchedApp = client.getMobileAppById(searchApp.getId().getId());
        assertEquals(searchApp.getPkgName(), fetchedApp.getPkgName());
        assertEquals(searchApp.getTitle(), fetchedApp.getTitle());
        assertEquals(searchApp.getPlatformType(), fetchedApp.getPlatformType());

        // update mobile app
        MobileApp appToUpdate = createdApps.get(2);
        appToUpdate.setTitle(appToUpdate.getTitle() + "_updated");
        MobileApp updatedApp = client.saveMobileApp(appToUpdate);
        assertEquals(appToUpdate.getTitle(), updatedApp.getTitle());

        // create mobile app bundle with android and ios apps
        MobileAppBundle bundle = new MobileAppBundle();
        bundle.setTitle(TEST_PREFIX + "Bundle_" + timestamp);
        bundle.setDescription("Test bundle");
        bundle.setAndroidAppId(createdApps.get(0).getId());
        bundle.setIosAppId(createdApps.get(3).getId());
        bundle.setOauth2Enabled(false);

        MobileAppBundle savedBundle = client.saveMobileAppBundle(bundle, null);
        assertNotNull(savedBundle);
        assertNotNull(savedBundle.getId());
        assertEquals(bundle.getTitle(), savedBundle.getTitle());

        // get bundle info by id
        MobileAppBundleInfo bundleInfo = client.getMobileAppBundleInfoById(savedBundle.getId().getId());
        assertEquals(savedBundle.getTitle(), bundleInfo.getTitle());
        assertEquals("Test bundle", bundleInfo.getDescription());
        assertNotNull(bundleInfo.getAndroidPkgName());
        assertNotNull(bundleInfo.getIosPkgName());

        // list tenant bundles
        PageDataMobileAppBundleInfo bundles = client.getTenantMobileAppBundleInfos(100, 0,
                TEST_PREFIX + "Bundle_" + timestamp, null, null);
        assertEquals(1, bundles.getData().size());

        // update bundle
        savedBundle.setDescription("Updated description");
        MobileAppBundle updatedBundle = client.saveMobileAppBundle(savedBundle, null);
        assertEquals("Updated description", updatedBundle.getDescription());

        // delete bundle
        client.deleteMobileAppBundle(savedBundle.getId().getId());

        // verify bundle deletion
        assertReturns404(() ->
                client.getMobileAppBundleInfoById(savedBundle.getId().getId())
        );

        // delete mobile app
        UUID appToDeleteId = createdApps.get(0).getId().getId();
        client.deleteMobileApp(appToDeleteId);

        // verify app deletion
        assertReturns404(() ->
                client.getMobileAppById(appToDeleteId)
        );

        PageDataMobileApp appsAfterDelete = client.getTenantMobileApps(100, 0, null,
                null, null, null);
        assertEquals(4, appsAfterDelete.getData().size());
    }

}
