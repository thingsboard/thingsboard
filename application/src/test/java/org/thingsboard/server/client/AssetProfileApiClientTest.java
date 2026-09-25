// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteAssetProfileArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAssetProfileByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAssetProfileInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAssetProfileNamesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAssetProfilesArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveAssetProfileArgs;
import org.thingsboard.client.api.ThingsboardApi.SetDefaultAssetProfileArgs;
import org.thingsboard.client.model.AssetProfile;
import org.thingsboard.client.model.AssetProfileInfo;
import org.thingsboard.client.model.EntityInfo;
import org.thingsboard.client.model.PageDataAssetProfile;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class AssetProfileApiClientTest extends AbstractApiClientTest {

    @Test
    public void testAssetProfileLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<AssetProfile> createdProfiles = new ArrayList<>();

        // Get initial count (there should be a default profile)
        PageDataAssetProfile initialProfiles = client.getAssetProfiles(GetAssetProfilesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(initialProfiles);
        int initialSize = initialProfiles.getData().size();
        assertTrue("Expected at least 1 default asset profile", initialSize == 1);

        // Get default asset profile info
        AssetProfileInfo defaultProfileInfo = client.getDefaultAssetProfileInfo();
        assertNotNull(defaultProfileInfo);
        assertEquals(defaultProfileInfo.getName(), "default");

        // Create multiple asset profiles
        for (int i = 0; i < 5; i++) {
            AssetProfile profile = new AssetProfile();
            profile.setName("Test Asset Profile " + timestamp + "_" + i);
            profile.setDescription("Test description " + i);

            AssetProfile created = client.saveAssetProfile(SaveAssetProfileArgs.builder()
                    .assetProfile(profile)
                    .build());
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(profile.getName(), created.getName());
            assertEquals(profile.getDescription(), created.getDescription());
            assertFalse(created.getDefault());

            createdProfiles.add(created);
        }

        // Find all, check count
        PageDataAssetProfile allProfiles = client.getAssetProfiles(GetAssetProfilesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allProfiles);
        assertEquals(initialSize + 5, allProfiles.getData().size());

        // Find all with text search
        PageDataAssetProfile filteredProfiles = client.getAssetProfiles(GetAssetProfilesArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch("Test Asset Profile " + timestamp)
                .build());
        assertEquals(5, filteredProfiles.getData().size());

        // Get by id
        AssetProfile searchProfile = createdProfiles.get(2);
        AssetProfile fetchedProfile = client.getAssetProfileById(GetAssetProfileByIdArgs.builder()
                .assetProfileId(searchProfile.getId().getId().toString())
                .inlineImages(false)
                .build());
        assertEquals(searchProfile.getName(), fetchedProfile.getName());
        assertEquals(searchProfile.getDescription(), fetchedProfile.getDescription());

        // Update asset profile
        fetchedProfile.setDescription("Updated description");
        AssetProfile updatedProfile = client.saveAssetProfile(SaveAssetProfileArgs.builder()
                .assetProfile(fetchedProfile)
                .build());
        assertEquals("Updated description", updatedProfile.getDescription());
        assertEquals(fetchedProfile.getName(), updatedProfile.getName());

        // Get asset profile info by id
        AssetProfileInfo profileInfo = client.getAssetProfileInfoById(GetAssetProfileInfoByIdArgs.builder()
                .assetProfileId(searchProfile.getId().getId().toString())
                .build());
        assertNotNull(profileInfo);
        assertEquals(searchProfile.getName(), profileInfo.getName());

        // Get asset profile infos (paginated)
        PageDataAssetProfile profileInfos = client.getAssetProfiles(GetAssetProfilesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(profileInfos);
        assertEquals(initialSize + 5, profileInfos.getData().size());

        // Set a profile as default
        AssetProfile profileToSetDefault = createdProfiles.get(1);
        AssetProfile newDefault = client.setDefaultAssetProfile(SetDefaultAssetProfileArgs.builder()
                .assetProfileId(profileToSetDefault.getId().getId().toString())
                .build());
        assertNotNull(newDefault);
        assertTrue(newDefault.getDefault());

        // Verify default profile info now points to the new default
        AssetProfileInfo newDefaultInfo = client.getDefaultAssetProfileInfo();
        assertEquals(profileToSetDefault.getName(), newDefaultInfo.getName());

        // Get asset profile names
        List<EntityInfo> profileNames = client.getAssetProfileNames(GetAssetProfileNamesArgs.builder()
                .activeOnly(false)
                .build());
        assertNotNull(profileNames);
        assertEquals(createdProfiles.size() + 1, profileNames.size());

        // Delete asset profile (cannot delete the default one, so delete a non-default one)
        UUID profileToDeleteId = createdProfiles.get(0).getId().getId();
        client.deleteAssetProfile(DeleteAssetProfileArgs.builder()
                .assetProfileId(profileToDeleteId.toString())
                .build());

        // Verify the profile is deleted
        assertReturns404(() ->
                client.getAssetProfileById(GetAssetProfileByIdArgs.builder()
                        .assetProfileId(profileToDeleteId.toString())
                        .inlineImages(false)
                        .build()));

        // Verify count after deletion
        PageDataAssetProfile profilesAfterDelete = client.getAssetProfiles(GetAssetProfilesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialSize + 4, profilesAfterDelete.getData().size());

        // Restore original default profile
        AssetProfile originalDefault = initialProfiles.getData().stream()
                .filter(AssetProfile::getDefault)
                .findFirst()
                .orElseThrow();
        client.setDefaultAssetProfile(SetDefaultAssetProfileArgs.builder()
                .assetProfileId(originalDefault.getId().getId().toString())
                .build());
    }

}
