// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteDeviceProfileArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDeviceProfileByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDeviceProfileInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDeviceProfileNamesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDeviceProfilesArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceProfileArgs;
import org.thingsboard.client.api.ThingsboardApi.SetDefaultDeviceProfileArgs;
import org.thingsboard.client.model.DefaultDeviceProfileConfiguration;
import org.thingsboard.client.model.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.client.model.DeviceProfile;
import org.thingsboard.client.model.DeviceProfileData;
import org.thingsboard.client.model.DeviceProfileInfo;
import org.thingsboard.client.model.DeviceProfileType;
import org.thingsboard.client.model.DeviceTransportType;
import org.thingsboard.client.model.EntityInfo;
import org.thingsboard.client.model.PageDataDeviceProfile;
import org.thingsboard.client.model.PageDataDeviceProfileInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class DeviceProfileApiClientTest extends AbstractApiClientTest {

    @Test
    public void testDeviceProfileLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<DeviceProfile> createdProfiles = new ArrayList<>();

        // Get initial count (there should be a default profile)
        PageDataDeviceProfile initialProfiles = client.getDeviceProfiles(GetDeviceProfilesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(initialProfiles);
        int initialSize = initialProfiles.getData().size();
        assertTrue("Expected at least 1 default device profile", initialSize >= 1);

        // Get default device profile info
        DeviceProfileInfo defaultProfileInfo = client.getDefaultDeviceProfileInfo();
        assertNotNull(defaultProfileInfo);
        assertNotNull(defaultProfileInfo.getName());

        // Create multiple device profiles
        for (int i = 0; i < 5; i++) {
            DeviceProfile deviceProfile = new DeviceProfile();
            deviceProfile.setName("Test Device Profile " + timestamp + "_" + i);
            deviceProfile.setDescription("Test description " + i);
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            deviceProfile.setTransportType(DeviceTransportType.DEFAULT);

            DeviceProfileData deviceProfileData = new DeviceProfileData();
            DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
            configuration.setType(DeviceProfileType.DEFAULT.getValue());
            deviceProfileData.setConfiguration(configuration);
            DefaultDeviceProfileTransportConfiguration transportConf = new DefaultDeviceProfileTransportConfiguration();
            transportConf.setType(DeviceTransportType.DEFAULT.getValue());
            deviceProfileData.setTransportConfiguration(transportConf);
            deviceProfile.setProfileData(deviceProfileData);
            deviceProfile.setDefault(false);
            deviceProfile.setDefaultRuleChainId(null);

            DeviceProfile created = client.saveDeviceProfile(SaveDeviceProfileArgs.builder()
                    .deviceProfile(deviceProfile)
                    .build());
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(deviceProfile.getName(), created.getName());
            assertEquals(deviceProfile.getDescription(), created.getDescription());
            assertEquals(DeviceProfileType.DEFAULT, created.getType());
            assertEquals(DeviceTransportType.DEFAULT, created.getTransportType());
            assertFalse(created.getDefault());

            createdProfiles.add(created);
        }

        // Find all, check count
        PageDataDeviceProfile allProfiles = client.getDeviceProfiles(GetDeviceProfilesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allProfiles);
        assertEquals(initialSize + 5, allProfiles.getData().size());

        // Find all with text search
        PageDataDeviceProfile filteredProfiles = client.getDeviceProfiles(GetDeviceProfilesArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch("Test Device Profile " + timestamp)
                .build());
        assertEquals(5, filteredProfiles.getData().size());

        // Get by id
        DeviceProfile searchProfile = createdProfiles.get(2);
        DeviceProfile fetchedProfile = client.getDeviceProfileById(GetDeviceProfileByIdArgs.builder()
                .deviceProfileId(searchProfile.getId().getId().toString())
                .inlineImages(false)
                .build());
        assertEquals(searchProfile.getName(), fetchedProfile.getName());
        assertEquals(searchProfile.getDescription(), fetchedProfile.getDescription());

        // Update device profile
        fetchedProfile.setDescription("Updated description");
        DeviceProfile updatedProfile = client.saveDeviceProfile(SaveDeviceProfileArgs.builder()
                .deviceProfile(fetchedProfile)
                .build());
        assertEquals("Updated description", updatedProfile.getDescription());
        assertEquals(fetchedProfile.getName(), updatedProfile.getName());

        // Get device profile info by id
        DeviceProfileInfo profileInfo = client.getDefaultDeviceProfileInfo();
        assertNotNull(profileInfo);
        assertEquals(searchProfile.getType().getValue().toLowerCase(), profileInfo.getName());
        assertEquals(DeviceTransportType.DEFAULT, profileInfo.getTransportType());

        // Get device profile infos (paginated)
        PageDataDeviceProfileInfo profileInfos = client.getDeviceProfileInfos(GetDeviceProfileInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(profileInfos);
        assertEquals(initialSize + 5, profileInfos.getData().size());

        // Set a profile as default
        DeviceProfile profileToSetDefault = createdProfiles.get(1);
        DeviceProfile newDefault = client.setDefaultDeviceProfile(SetDefaultDeviceProfileArgs.builder()
                .deviceProfileId(profileToSetDefault.getId().getId().toString())
                .build());
        assertNotNull(newDefault);
        assertTrue(newDefault.getDefault());

        // Verify default profile info now points to the new default
        DeviceProfileInfo newDefaultInfo = client.getDefaultDeviceProfileInfo();
        assertEquals(profileToSetDefault.getName(), newDefaultInfo.getName());

        // Get device profile names
        List<EntityInfo> profileNames = client.getDeviceProfileNames(GetDeviceProfileNamesArgs.builder()
                .activeOnly(false)
                .build());
        assertNotNull(profileNames);
        assertEquals(createdProfiles.size() + 1, profileNames.size());

        // Delete device profile (cannot delete the default one, so delete a non-default one)
        UUID profileToDeleteId = createdProfiles.get(0).getId().getId();
        client.deleteDeviceProfile(DeleteDeviceProfileArgs.builder()
                .deviceProfileId(profileToDeleteId.toString())
                .build());

        // Verify the profile is deleted
        assertReturns404(() ->
                client.getDeviceProfileById(GetDeviceProfileByIdArgs.builder()
                        .deviceProfileId(profileToDeleteId.toString())
                        .inlineImages(false)
                        .build()));

        // Verify count after deletion
        PageDataDeviceProfile profilesAfterDelete = client.getDeviceProfiles(GetDeviceProfilesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialSize + 4, profilesAfterDelete.getData().size());

        // Restore original default profile
        DeviceProfile originalDefault = initialProfiles.getData().stream()
                .filter(DeviceProfile::getDefault)
                .findFirst()
                .orElseThrow();
        client.setDefaultDeviceProfile(SetDefaultDeviceProfileArgs.builder()
                .deviceProfileId(originalDefault.getId().getId().toString())
                .build());
    }

}
