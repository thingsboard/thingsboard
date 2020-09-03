/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BaseDeviceProfileServiceTest extends AbstractServiceTest {

    private IdComparator<DeviceProfile> idComparator = new IdComparator<>();
    private IdComparator<DeviceProfileInfo> deviceProfileInfoIdComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveDeviceProfile() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedDeviceProfile);
        Assert.assertNotNull(savedDeviceProfile.getId());
        Assert.assertTrue(savedDeviceProfile.getCreatedTime() > 0);
        Assert.assertEquals(deviceProfile.getName(), savedDeviceProfile.getName());
        Assert.assertEquals(deviceProfile.getDescription(), savedDeviceProfile.getDescription());
        Assert.assertEquals(deviceProfile.getProfileData(), savedDeviceProfile.getProfileData());
        Assert.assertEquals(deviceProfile.isDefault(), savedDeviceProfile.isDefault());
        Assert.assertEquals(deviceProfile.getDefaultRuleChainId(), savedDeviceProfile.getDefaultRuleChainId());
        savedDeviceProfile.setName("New device profile");
        deviceProfileService.saveDeviceProfile(savedDeviceProfile);
        DeviceProfile foundDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, savedDeviceProfile.getId());
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfile.getName());
    }

    @Test
    public void testFindDeviceProfileById() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId,"Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfile foundDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, savedDeviceProfile.getId());
        Assert.assertNotNull(foundDeviceProfile);
        Assert.assertEquals(savedDeviceProfile, foundDeviceProfile);
   }

    @Test
    public void testFindDeviceProfileInfoById() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId,"Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfileInfo foundDeviceProfileInfo = deviceProfileService.findDeviceProfileInfoById(tenantId, savedDeviceProfile.getId());
        Assert.assertNotNull(foundDeviceProfileInfo);
        Assert.assertEquals(savedDeviceProfile.getId(), foundDeviceProfileInfo.getId());
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfileInfo.getName());
        Assert.assertEquals(savedDeviceProfile.getType(), foundDeviceProfileInfo.getType());
    }

    @Test
    public void testFindDefaultDeviceProfile() {
        DeviceProfile foundDefaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        Assert.assertNotNull(foundDefaultDeviceProfile);
        Assert.assertNotNull(foundDefaultDeviceProfile.getId());
        Assert.assertNotNull(foundDefaultDeviceProfile.getName());
    }

    @Test
    public void testFindDefaultDeviceProfileInfo() {
        DeviceProfileInfo foundDefaultDeviceProfileInfo = deviceProfileService.findDefaultDeviceProfileInfo(tenantId);
        Assert.assertNotNull(foundDefaultDeviceProfileInfo);
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getId());
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getName());
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getType());
    }

    @Test
    public void testSetDefaultDeviceProfile() {
        DeviceProfile deviceProfile1 = this.createDeviceProfile(tenantId,"Device Profile 1");
        DeviceProfile deviceProfile2 = this.createDeviceProfile(tenantId,"Device Profile 2");

        DeviceProfile savedDeviceProfile1 = deviceProfileService.saveDeviceProfile(deviceProfile1);
        DeviceProfile savedDeviceProfile2 = deviceProfileService.saveDeviceProfile(deviceProfile2);

        boolean result = deviceProfileService.setDefaultDeviceProfile(tenantId, savedDeviceProfile1.getId());
        Assert.assertTrue(result);
        DeviceProfile defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        Assert.assertNotNull(defaultDeviceProfile);
        Assert.assertEquals(savedDeviceProfile1.getId(), defaultDeviceProfile.getId());
        result = deviceProfileService.setDefaultDeviceProfile(tenantId, savedDeviceProfile2.getId());
        Assert.assertTrue(result);
        defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        Assert.assertNotNull(defaultDeviceProfile);
        Assert.assertEquals(savedDeviceProfile2.getId(), defaultDeviceProfile.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDeviceProfileWithEmptyName() {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDeviceProfileWithSameName() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId,"Device Profile");
        deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfile deviceProfile2 = this.createDeviceProfile(tenantId,"Device Profile");
        deviceProfileService.saveDeviceProfile(deviceProfile2);
    }

    @Ignore
    @Test(expected = DataValidationException.class)
    public void testSaveSameDeviceProfileWithDifferentType() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId,"Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        //TODO: once we have mode profile types, we should test that we can not change profile type in runtime and uncomment the @Ignore.
//        savedDeviceProfile.setType(DeviceProfileType.LWM2M);
        deviceProfileService.saveDeviceProfile(savedDeviceProfile);
    }

    @Test(expected = DataValidationException.class)
    public void testDeleteDeviceProfileWithExistingDevice() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId,"Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setType("default");
        device.setDeviceProfileId(savedDeviceProfile.getId());
        deviceService.saveDevice(device);
        deviceProfileService.deleteDeviceProfile(tenantId, savedDeviceProfile.getId());
    }

    @Test
    public void testDeleteDeviceProfile() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId,"Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        deviceProfileService.deleteDeviceProfile(tenantId, savedDeviceProfile.getId());
        DeviceProfile foundDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, savedDeviceProfile.getId());
        Assert.assertNull(foundDeviceProfile);
    }

    @Test
    public void testFindDeviceProfiles() {

        List<DeviceProfile> deviceProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<DeviceProfile> pageData = deviceProfileService.findDeviceProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        deviceProfiles.addAll(pageData.getData());

        for (int i=0;i<28;i++) {
            DeviceProfile deviceProfile = this.createDeviceProfile(tenantId,"Device Profile"+i);
            deviceProfiles.add(deviceProfileService.saveDeviceProfile(deviceProfile));
        }

        List<DeviceProfile> loadedDeviceProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = deviceProfileService.findDeviceProfiles(tenantId, pageLink);
            loadedDeviceProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(deviceProfiles, idComparator);
        Collections.sort(loadedDeviceProfiles, idComparator);

        Assert.assertEquals(deviceProfiles, loadedDeviceProfiles);

        for (DeviceProfile deviceProfile : loadedDeviceProfiles) {
            if (!deviceProfile.isDefault()) {
                deviceProfileService.deleteDeviceProfile(tenantId, deviceProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = deviceProfileService.findDeviceProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testFindDeviceProfileInfos() {

        List<DeviceProfile> deviceProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<DeviceProfile> deviceProfilePageData = deviceProfileService.findDeviceProfiles(tenantId, pageLink);
        Assert.assertFalse(deviceProfilePageData.hasNext());
        Assert.assertEquals(1, deviceProfilePageData.getTotalElements());
        deviceProfiles.addAll(deviceProfilePageData.getData());

        for (int i=0;i<28;i++) {
            DeviceProfile deviceProfile = this.createDeviceProfile(tenantId,"Device Profile"+i);
            deviceProfiles.add(deviceProfileService.saveDeviceProfile(deviceProfile));
        }

        List<DeviceProfileInfo> loadedDeviceProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<DeviceProfileInfo> pageData;
        do {
            pageData = deviceProfileService.findDeviceProfileInfos(tenantId, pageLink);
            loadedDeviceProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());


        Collections.sort(deviceProfiles, idComparator);
        Collections.sort(loadedDeviceProfileInfos, deviceProfileInfoIdComparator);

        List<DeviceProfileInfo> deviceProfileInfos = deviceProfiles.stream()
                .map(deviceProfile -> new DeviceProfileInfo(deviceProfile.getId(),
                deviceProfile.getName(), deviceProfile.getType(), deviceProfile.getTransportType())).collect(Collectors.toList());

        Assert.assertEquals(deviceProfileInfos, loadedDeviceProfileInfos);

        for (DeviceProfile deviceProfile : deviceProfiles) {
            if (!deviceProfile.isDefault()) {
                deviceProfileService.deleteDeviceProfile(tenantId, deviceProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = deviceProfileService.findDeviceProfileInfos(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }
}
