// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.DeleteTenantProfileArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantProfileByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantProfileInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantProfileListArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantProfilesArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveTenantProfileArgs;
import org.thingsboard.client.api.ThingsboardApi.SetDefaultTenantProfileArgs;
import org.thingsboard.client.model.DefaultTenantProfileConfiguration;
import org.thingsboard.client.model.EntityInfo;
import org.thingsboard.client.model.PageDataEntityInfo;
import org.thingsboard.client.model.PageDataTenantProfile;
import org.thingsboard.client.model.TenantProfile;
import org.thingsboard.client.model.TenantProfileData;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class TenantProfileApiClientTest extends AbstractApiClientTest {

    @Test
    public void testTenantProfileLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<TenantProfile> createdProfiles = new ArrayList<>();

        // authenticate as sysadmin for tenant profile management
        client.login("sysadmin@thingsboard.org", "sysadmin");

        // get initial count (there should be a default profile)
        PageDataTenantProfile initialProfiles = client.getTenantProfiles(GetTenantProfilesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(initialProfiles);
        int initialSize = initialProfiles.getData().size();
        assertTrue("Expected at least 1 default tenant profile", initialSize >= 1);

        // get default tenant profile info
        EntityInfo defaultProfileInfo = client.getDefaultTenantProfileInfo();
        assertNotNull(defaultProfileInfo);
        assertNotNull(defaultProfileInfo.getName());

        try {
            // create 5 tenant profiles
            for (int i = 0; i < 5; i++) {
                TenantProfile profile = new TenantProfile();
                profile.setName(TEST_PREFIX + "TenantProfile_" + timestamp + "_" + i);
                profile.setDescription("Test tenant profile " + i);
                profile.setIsolatedTbRuleEngine(false);

                TenantProfileData profileData = new TenantProfileData();
                DefaultTenantProfileConfiguration config = new DefaultTenantProfileConfiguration();
                config.setMaxDevices(100L);
                config.setMaxAssets(100L);
                config.setMaxCustomers(50L);
                config.setMaxUsers(50L);
                config.setMaxDashboards(50L);
                config.setMaxRuleChains(20L);
                config.setMaxDataPointsPerRollingArg(20L);
                config.setMaxRelatedEntitiesToReturnPerCfArgument(20);
                config.setMaxRelationLevelPerCfArgument(20);
                profileData.setConfiguration(config);
                profile.setProfileData(profileData);
                profile.setDefault(false);

                TenantProfile created = client.saveTenantProfile(SaveTenantProfileArgs.builder()
                        .tenantProfile(profile)
                        .build());
                assertNotNull(created);
                assertNotNull(created.getId());
                assertEquals(profile.getName(), created.getName());
                assertEquals(profile.getDescription(), created.getDescription());
                assertFalse(created.getDefault());

                createdProfiles.add(created);
            }

            // find all, check count
            PageDataTenantProfile allProfiles = client.getTenantProfiles(GetTenantProfilesArgs.builder()
                    .pageSize(100)
                    .page(0)
                    .build());
            assertNotNull(allProfiles);
            assertEquals(initialSize + 5, allProfiles.getData().size());

            // find with text search
            PageDataTenantProfile filteredProfiles = client.getTenantProfiles(GetTenantProfilesArgs.builder()
                    .pageSize(100)
                    .page(0)
                    .textSearch(TEST_PREFIX + "TenantProfile_" + timestamp)
                    .build());
            assertEquals(5, filteredProfiles.getData().size());

            // get by id
            TenantProfile searchProfile = createdProfiles.get(2);
            TenantProfile fetchedProfile = client.getTenantProfileById(GetTenantProfileByIdArgs.builder()
                    .tenantProfileId(searchProfile.getId().getId().toString())
                    .build());
            assertEquals(searchProfile.getName(), fetchedProfile.getName());
            assertEquals(searchProfile.getDescription(), fetchedProfile.getDescription());

            // update tenant profile
            fetchedProfile.setDescription("Updated description");
            TenantProfile updatedProfile = client.saveTenantProfile(SaveTenantProfileArgs.builder()
                    .tenantProfile(fetchedProfile)
                    .build());
            assertEquals("Updated description", updatedProfile.getDescription());
            assertEquals(fetchedProfile.getName(), updatedProfile.getName());

            // get tenant profile infos (paginated)
            PageDataEntityInfo profileInfos = client.getTenantProfileInfos(GetTenantProfileInfosArgs.builder()
                    .pageSize(100)
                    .page(0)
                    .build());
            assertNotNull(profileInfos);
            assertEquals(initialSize + 5, profileInfos.getData().size());

            // get profiles by list of ids
            List<String> idsToFetch = List.of(
                    createdProfiles.get(0).getId().getId().toString(),
                    createdProfiles.get(1).getId().getId().toString()
            );
            List<TenantProfile> profileList = client.getTenantProfileList(GetTenantProfileListArgs.builder()
                    .ids(idsToFetch)
                    .build());
            assertEquals(2, profileList.size());

            // set a profile as default
            TenantProfile profileToSetDefault = createdProfiles.get(1);
            client.setDefaultTenantProfile(SetDefaultTenantProfileArgs.builder()
                    .tenantProfileId(profileToSetDefault.getId().getId().toString())
                    .build());
            EntityInfo defaultTenantProfileInfo = client.getDefaultTenantProfileInfo();
            assertEquals(profileToSetDefault.getName(), defaultTenantProfileInfo.getName());

            // verify default profile info now points to the new default
            EntityInfo newDefaultInfo = client.getDefaultTenantProfileInfo();
            assertEquals(profileToSetDefault.getName(), newDefaultInfo.getName());

            // restore original default profile
            TenantProfile originalDefault = initialProfiles.getData().stream()
                    .filter(TenantProfile::getDefault)
                    .findFirst()
                    .orElseThrow();
            client.setDefaultTenantProfile(SetDefaultTenantProfileArgs.builder()
                    .tenantProfileId(originalDefault.getId().getId().toString())
                    .build());

            // delete tenant profile (cannot delete the default one)
            UUID profileToDeleteId = createdProfiles.get(0).getId().getId();
            client.deleteTenantProfile(DeleteTenantProfileArgs.builder()
                    .tenantProfileId(profileToDeleteId.toString())
                    .build());
            createdProfiles.remove(0);

            // verify deletion
            assertReturns404(() ->
                    client.getTenantProfileById(GetTenantProfileByIdArgs.builder()
                            .tenantProfileId(profileToDeleteId.toString())
                            .build())
            );

            PageDataTenantProfile profilesAfterDelete = client.getTenantProfiles(GetTenantProfilesArgs.builder()
                    .pageSize(100)
                    .page(0)
                    .build());
            assertEquals(initialSize + 4, profilesAfterDelete.getData().size());
        } finally {
            // clean up created profiles
            client.login("sysadmin@thingsboard.org", "sysadmin");

            // ensure original default is restored before deleting test profiles
            TenantProfile originalDefault = initialProfiles.getData().stream()
                    .filter(TenantProfile::getDefault)
                    .findFirst()
                    .orElseThrow();
            try {
                client.setDefaultTenantProfile(SetDefaultTenantProfileArgs.builder()
                        .tenantProfileId(originalDefault.getId().getId().toString())
                        .build());
            } catch (ApiException ignored) {
            }

            for (TenantProfile profile : createdProfiles) {
                try {
                    client.deleteTenantProfile(DeleteTenantProfileArgs.builder()
                            .tenantProfileId(profile.getId().getId().toString())
                            .build());
                } catch (ApiException ignored) {
                }
            }
        }
    }

}
