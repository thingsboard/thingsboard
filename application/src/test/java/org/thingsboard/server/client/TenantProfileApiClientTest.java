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
import org.thingsboard.client.ApiException;
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
        PageDataTenantProfile initialProfiles = client.getTenantProfiles(100, 0, null, null, null);
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

                TenantProfile created = client.saveTenantProfile(profile);
                assertNotNull(created);
                assertNotNull(created.getId());
                assertEquals(profile.getName(), created.getName());
                assertEquals(profile.getDescription(), created.getDescription());
                assertFalse(created.getDefault());

                createdProfiles.add(created);
            }

            // find all, check count
            PageDataTenantProfile allProfiles = client.getTenantProfiles(100, 0, null, null, null);
            assertNotNull(allProfiles);
            assertEquals(initialSize + 5, allProfiles.getData().size());

            // find with text search
            PageDataTenantProfile filteredProfiles = client.getTenantProfiles(100, 0,
                    TEST_PREFIX + "TenantProfile_" + timestamp, null, null);
            assertEquals(5, filteredProfiles.getData().size());

            // get by id
            TenantProfile searchProfile = createdProfiles.get(2);
            TenantProfile fetchedProfile = client.getTenantProfileById(searchProfile.getId().getId().toString());
            assertEquals(searchProfile.getName(), fetchedProfile.getName());
            assertEquals(searchProfile.getDescription(), fetchedProfile.getDescription());

            // update tenant profile
            fetchedProfile.setDescription("Updated description");
            TenantProfile updatedProfile = client.saveTenantProfile(fetchedProfile);
            assertEquals("Updated description", updatedProfile.getDescription());
            assertEquals(fetchedProfile.getName(), updatedProfile.getName());

            // get tenant profile infos (paginated)
            PageDataEntityInfo profileInfos = client.getTenantProfileInfos(100, 0, null, null, null);
            assertNotNull(profileInfos);
            assertEquals(initialSize + 5, profileInfos.getData().size());

            // get profiles by list of ids
            List<String> idsToFetch = List.of(
                    createdProfiles.get(0).getId().getId().toString(),
                    createdProfiles.get(1).getId().getId().toString()
            );
            List<TenantProfile> profileList = client.getTenantProfileList(idsToFetch);
            assertEquals(2, profileList.size());

            // set a profile as default
            TenantProfile profileToSetDefault = createdProfiles.get(1);
            client.setDefaultTenantProfile(profileToSetDefault.getId().getId().toString());
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
            client.setDefaultTenantProfile(originalDefault.getId().getId().toString());

            // delete tenant profile (cannot delete the default one)
            UUID profileToDeleteId = createdProfiles.get(0).getId().getId();
            client.deleteTenantProfile(profileToDeleteId.toString());
            createdProfiles.remove(0);

            // verify deletion
            assertReturns404(() ->
                    client.getTenantProfileById(profileToDeleteId.toString())
            );

            PageDataTenantProfile profilesAfterDelete = client.getTenantProfiles(100, 0, null, null, null);
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
                client.setDefaultTenantProfile(originalDefault.getId().getId().toString());
            } catch (ApiException ignored) {
            }

            for (TenantProfile profile : createdProfiles) {
                try {
                    client.deleteTenantProfile(profile.getId().getId().toString());
                } catch (ApiException ignored) {
                }
            }
        }
    }

}
