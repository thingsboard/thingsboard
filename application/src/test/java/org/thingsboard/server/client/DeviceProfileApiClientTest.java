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
        PageDataDeviceProfile initialProfiles = client.getDeviceProfiles(100, 0, null, null, null);
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

            DeviceProfile created = client.saveDeviceProfile(deviceProfile);
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
        PageDataDeviceProfile allProfiles = client.getDeviceProfiles(100, 0, null, null, null);
        assertNotNull(allProfiles);
        assertEquals(initialSize + 5, allProfiles.getData().size());

        // Find all with text search
        PageDataDeviceProfile filteredProfiles = client.getDeviceProfiles(100, 0, "Test Device Profile " + timestamp, null, null);
        assertEquals(5, filteredProfiles.getData().size());

        // Get by id
        DeviceProfile searchProfile = createdProfiles.get(2);
        DeviceProfile fetchedProfile = client.getDeviceProfileById(searchProfile.getId().getId().toString(), false);
        assertEquals(searchProfile.getName(), fetchedProfile.getName());
        assertEquals(searchProfile.getDescription(), fetchedProfile.getDescription());

        // Update device profile
        fetchedProfile.setDescription("Updated description");
        DeviceProfile updatedProfile = client.saveDeviceProfile(fetchedProfile);
        assertEquals("Updated description", updatedProfile.getDescription());
        assertEquals(fetchedProfile.getName(), updatedProfile.getName());

        // Get device profile info by id
        DeviceProfileInfo profileInfo = client.getDefaultDeviceProfileInfo();
        assertNotNull(profileInfo);
        assertEquals(searchProfile.getType().getValue().toLowerCase(), profileInfo.getName());
        assertEquals(DeviceTransportType.DEFAULT, profileInfo.getTransportType());

        // Get device profile infos (paginated)
        PageDataDeviceProfileInfo profileInfos = client.getDeviceProfileInfos(100, 0, null, null, null, null);
        assertNotNull(profileInfos);
        assertEquals(initialSize + 5, profileInfos.getData().size());

        // Set a profile as default
        DeviceProfile profileToSetDefault = createdProfiles.get(1);
        DeviceProfile newDefault = client.setDefaultDeviceProfile(profileToSetDefault.getId().getId().toString());
        assertNotNull(newDefault);
        assertTrue(newDefault.getDefault());

        // Verify default profile info now points to the new default
        DeviceProfileInfo newDefaultInfo = client.getDefaultDeviceProfileInfo();
        assertEquals(profileToSetDefault.getName(), newDefaultInfo.getName());

        // Get device profile names
        List<EntityInfo> profileNames = client.getDeviceProfileNames(false);
        assertNotNull(profileNames);
        assertEquals(createdProfiles.size() + 1, profileNames.size());

        // Delete device profile (cannot delete the default one, so delete a non-default one)
        UUID profileToDeleteId = createdProfiles.get(0).getId().getId();
        client.deleteDeviceProfile(profileToDeleteId.toString());

        // Verify the profile is deleted
        assertReturns404(() ->
                client.getDeviceProfileById(profileToDeleteId.toString(), false));

        // Verify count after deletion
        PageDataDeviceProfile profilesAfterDelete = client.getDeviceProfiles(100, 0, null, null, null);
        assertEquals(initialSize + 4, profilesAfterDelete.getData().size());

        // Restore original default profile
        DeviceProfile originalDefault = initialProfiles.getData().stream()
                .filter(DeviceProfile::getDefault)
                .findFirst()
                .orElseThrow();
        client.setDefaultDeviceProfile(originalDefault.getId().getId().toString());
    }

}
