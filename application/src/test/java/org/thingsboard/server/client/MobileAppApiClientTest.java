// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteMobileAppArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteMobileAppBundleArgs;
import org.thingsboard.client.api.ThingsboardApi.GetMobileAppBundleInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetMobileAppByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantMobileAppBundleInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantMobileAppsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveMobileAppArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveMobileAppBundleArgs;
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

            MobileApp created = client.saveMobileApp(SaveMobileAppArgs.builder()
                    .mobileApp(app)
                    .build());
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

            MobileApp created = client.saveMobileApp(SaveMobileAppArgs.builder()
                    .mobileApp(app)
                    .build());
            assertNotNull(created);
            createdApps.add(created);
        }

        // list all tenant mobile apps
        PageDataMobileApp allApps = client.getTenantMobileApps(GetTenantMobileAppsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allApps);
        assertEquals(5, allApps.getData().size());

        // list with platform type filter
        PageDataMobileApp androidApps = client.getTenantMobileApps(GetTenantMobileAppsArgs.builder()
                .pageSize(100)
                .page(0)
                .platformType(PlatformType.ANDROID)
                .build());
        assertEquals(3, androidApps.getData().size());

        PageDataMobileApp iosApps = client.getTenantMobileApps(GetTenantMobileAppsArgs.builder()
                .pageSize(100)
                .page(0)
                .platformType(PlatformType.IOS)
                .build());
        assertEquals(2, iosApps.getData().size());

        // get mobile app by id
        MobileApp searchApp = createdApps.get(1);
        MobileApp fetchedApp = client.getMobileAppById(GetMobileAppByIdArgs.builder()
                .id(searchApp.getId().getId())
                .build());
        assertEquals(searchApp.getPkgName(), fetchedApp.getPkgName());
        assertEquals(searchApp.getTitle(), fetchedApp.getTitle());
        assertEquals(searchApp.getPlatformType(), fetchedApp.getPlatformType());

        // update mobile app
        MobileApp appToUpdate = createdApps.get(2);
        appToUpdate.setTitle(appToUpdate.getTitle() + "_updated");
        MobileApp updatedApp = client.saveMobileApp(SaveMobileAppArgs.builder()
                .mobileApp(appToUpdate)
                .build());
        assertEquals(appToUpdate.getTitle(), updatedApp.getTitle());

        // create mobile app bundle with android and ios apps
        MobileAppBundle bundle = new MobileAppBundle();
        bundle.setTitle(TEST_PREFIX + "Bundle_" + timestamp);
        bundle.setDescription("Test bundle");
        bundle.setAndroidAppId(createdApps.get(0).getId());
        bundle.setIosAppId(createdApps.get(3).getId());
        bundle.setOauth2Enabled(false);

        MobileAppBundle savedBundle = client.saveMobileAppBundle(SaveMobileAppBundleArgs.builder()
                .mobileAppBundle(bundle)
                .build());
        assertNotNull(savedBundle);
        assertNotNull(savedBundle.getId());
        assertEquals(bundle.getTitle(), savedBundle.getTitle());

        // get bundle info by id
        MobileAppBundleInfo bundleInfo = client.getMobileAppBundleInfoById(GetMobileAppBundleInfoByIdArgs.builder()
                .id(savedBundle.getId().getId())
                .build());
        assertEquals(savedBundle.getTitle(), bundleInfo.getTitle());
        assertEquals("Test bundle", bundleInfo.getDescription());
        assertNotNull(bundleInfo.getAndroidPkgName());
        assertNotNull(bundleInfo.getIosPkgName());

        // list tenant bundles
        PageDataMobileAppBundleInfo bundles = client.getTenantMobileAppBundleInfos(GetTenantMobileAppBundleInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + "Bundle_" + timestamp)
                .build());
        assertEquals(1, bundles.getData().size());

        // update bundle
        savedBundle.setDescription("Updated description");
        MobileAppBundle updatedBundle = client.saveMobileAppBundle(SaveMobileAppBundleArgs.builder()
                .mobileAppBundle(savedBundle)
                .build());
        assertEquals("Updated description", updatedBundle.getDescription());

        // delete bundle
        client.deleteMobileAppBundle(DeleteMobileAppBundleArgs.builder()
                .id(savedBundle.getId().getId())
                .build());

        // verify bundle deletion
        assertReturns404(() ->
                client.getMobileAppBundleInfoById(GetMobileAppBundleInfoByIdArgs.builder()
                        .id(savedBundle.getId().getId())
                        .build())
        );

        // delete mobile app
        UUID appToDeleteId = createdApps.get(0).getId().getId();
        client.deleteMobileApp(DeleteMobileAppArgs.builder()
                .id(appToDeleteId)
                .build());

        // verify app deletion
        assertReturns404(() ->
                client.getMobileAppById(GetMobileAppByIdArgs.builder()
                        .id(appToDeleteId)
                        .build())
        );

        PageDataMobileApp appsAfterDelete = client.getTenantMobileApps(GetTenantMobileAppsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(4, appsAfterDelete.getData().size());
    }

}
