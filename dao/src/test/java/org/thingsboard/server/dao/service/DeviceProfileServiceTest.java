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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.ota.OtaPackageService;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

@DaoSqlTest
public class DeviceProfileServiceTest extends AbstractServiceTest {

    @Autowired
    DeviceProfileService deviceProfileService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    OtaPackageService otaPackageService;

    private IdComparator<DeviceProfile> idComparator = new IdComparator<>();
    private IdComparator<DeviceProfileInfo> deviceProfileInfoIdComparator = new IdComparator<>();

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
    public void testSaveDeviceProfileWithFirmware() {
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

        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(savedDeviceProfile.getId());
        firmware.setType(FIRMWARE);
        firmware.setTitle("my firmware");
        firmware.setVersion("v1.0");
        firmware.setFileName("test.txt");
        firmware.setContentType("text/plain");
        firmware.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        firmware.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        firmware.setData(ByteBuffer.wrap(new byte[]{1}));
        firmware.setDataSize(1L);
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        deviceProfile.setFirmwareId(savedFirmware.getId());

        deviceProfileService.saveDeviceProfile(savedDeviceProfile);
        DeviceProfile foundDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, savedDeviceProfile.getId());
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfile.getName());
    }

    @Test
    public void testFindDeviceProfileById() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfile foundDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, savedDeviceProfile.getId());
        Assert.assertNotNull(foundDeviceProfile);
        Assert.assertEquals(savedDeviceProfile, foundDeviceProfile);
    }

    @Test
    public void testFindDeviceProfileInfoById() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
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
    public void testFindOrCreateDeviceProfile() throws ExecutionException, InterruptedException {
        ListeningExecutorService testExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(100, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-test-scope")));
        try {
            List<ListenableFuture<DeviceProfile>> futures = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                futures.add(testExecutor.submit(() -> deviceProfileService.findOrCreateDeviceProfile(tenantId, "Device Profile 1")));
                futures.add(testExecutor.submit(() -> deviceProfileService.findOrCreateDeviceProfile(tenantId, "Device Profile 2")));
            }

            List<DeviceProfile> deviceProfiles = Futures.allAsList(futures).get();
            deviceProfiles.forEach(Assert::assertNotNull);
        } finally {
            testExecutor.shutdownNow();
        }
    }

    @Test
    public void testSetDefaultDeviceProfile() {
        DeviceProfile deviceProfile1 = this.createDeviceProfile(tenantId, "Device Profile 1");
        DeviceProfile deviceProfile2 = this.createDeviceProfile(tenantId, "Device Profile 2");

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

    @Test
    public void testSaveDeviceProfileWithEmptyName() {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceProfileService.saveDeviceProfile(deviceProfile);
        });
    }

    @Test
    public void testSaveDeviceProfileWithSameName() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfile deviceProfile2 = this.createDeviceProfile(tenantId, "Device Profile");
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceProfileService.saveDeviceProfile(deviceProfile2);
        });
    }

    @Test
    public void testSaveDeviceProfileWithNameDefaultAndProfileWithIsDefaultAndDifferentNameAlreadyExists() {
        DeviceProfile defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        Assert.assertNotNull(defaultDeviceProfile);
        defaultDeviceProfile.setName("Device Profile 1");
        deviceProfileService.saveDeviceProfile(defaultDeviceProfile);
        DeviceProfile createdDeviceProfile = deviceProfileService.findOrCreateDeviceProfile(tenantId, "default");
        Assert.assertNotNull(createdDeviceProfile);
        Assert.assertEquals("default", createdDeviceProfile.getName());
        Assert.assertFalse(createdDeviceProfile.isDefault());
    }

    @Ignore
    @Test
    public void testChangeDeviceProfileTypeWithExistingDevices() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setType("default");
        device.setDeviceProfileId(savedDeviceProfile.getId());
        deviceService.saveDevice(device);
        //TODO: once we have more profile types, we should test that we can not change profile type in runtime and uncomment the @Ignore.
//        savedDeviceProfile.setType(DeviceProfileType.LWM2M);
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceProfileService.saveDeviceProfile(savedDeviceProfile);
        });
    }

    @Test
    public void testChangeDeviceProfileTransportTypeWithExistingDevices() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setType("default");
        device.setDeviceProfileId(savedDeviceProfile.getId());
        deviceService.saveDevice(device);
        savedDeviceProfile.setTransportType(DeviceTransportType.MQTT);
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceProfileService.saveDeviceProfile(savedDeviceProfile);
        });
    }

    @Test
    public void testDeleteDeviceProfileWithExistingDevice() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setType("default");
        device.setDeviceProfileId(savedDeviceProfile.getId());
        deviceService.saveDevice(device);
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceProfileService.deleteDeviceProfile(tenantId, savedDeviceProfile.getId());
        });
    }

    @Test
    public void testDeleteDeviceProfileWithExistingOta_cascadeDelete() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        OtaPackage otaPackage = constructDefaultOtaPackage(tenantId, deviceProfile.getId());
        otaPackage = otaPackageService.saveOtaPackage(otaPackage);

        assertThat(deviceProfileService.findDeviceProfileById(tenantId, deviceProfile.getId())).isNotNull();
        assertThat(otaPackageService.findOtaPackageById(tenantId, otaPackage.getId())).isNotNull();

        deviceProfileService.deleteDeviceProfile(tenantId, deviceProfile.getId());

        assertThat(deviceProfileService.findDeviceProfileById(tenantId, deviceProfile.getId())).isNull();
        assertThat(otaPackageService.findOtaPackageById(tenantId, otaPackage.getId())).isNull();
    }

    @Test
    public void testDeleteDeviceProfile() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
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

        for (int i = 0; i < 28; i++) {
            DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile" + i);
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

        for (int i = 0; i < 28; i++) {
            DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile" + i);
            deviceProfiles.add(deviceProfileService.saveDeviceProfile(deviceProfile));
        }

        List<DeviceProfileInfo> loadedDeviceProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<DeviceProfileInfo> pageData;
        do {
            pageData = deviceProfileService.findDeviceProfileInfos(tenantId, pageLink, null);
            loadedDeviceProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());


        Collections.sort(deviceProfiles, idComparator);
        Collections.sort(loadedDeviceProfileInfos, deviceProfileInfoIdComparator);

        List<DeviceProfileInfo> deviceProfileInfos = deviceProfiles.stream()
                .map(deviceProfile -> new DeviceProfileInfo(deviceProfile.getId(), deviceProfile.getTenantId(),
                        deviceProfile.getName(), deviceProfile.getImage(), deviceProfile.getDefaultDashboardId(),
                        deviceProfile.getType(), deviceProfile.getTransportType())).collect(Collectors.toList());

        Assert.assertEquals(deviceProfileInfos, loadedDeviceProfileInfos);

        for (DeviceProfile deviceProfile : deviceProfiles) {
            if (!deviceProfile.isDefault()) {
                deviceProfileService.deleteDeviceProfile(tenantId, deviceProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = deviceProfileService.findDeviceProfileInfos(tenantId, pageLink, null);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testFindAllDeviceProfilesByTenantId() {
        int deviceProfilesCount = 4; // 3 created + default
        var deviceProfiles = new ArrayList<DeviceProfile>(4);

        var profileC = deviceProfileService.saveDeviceProfile(
                createDeviceProfile(tenantId, "profile C"));
        deviceProfiles.add(deviceProfileService.saveDeviceProfile(profileC));


        var profileA = deviceProfileService.saveDeviceProfile(
                    createDeviceProfile(tenantId, "profile A"));
        deviceProfiles.add(deviceProfileService.saveDeviceProfile(profileA));


        var profileB = deviceProfileService.saveDeviceProfile(
                createDeviceProfile(tenantId, "profile B"));
        deviceProfiles.add(deviceProfileService.saveDeviceProfile(profileB));


        deviceProfiles.add(deviceProfileService.findDefaultDeviceProfile(tenantId));

        List<EntityInfo> sortedProfileInfos = deviceProfiles.stream()
                .map(profile -> new EntityInfo(profile.getId(), profile.getName()))
                .sorted(Comparator.comparing(EntityInfo::getName))
                .collect(Collectors.toList());

        var deviceProfileInfos = deviceProfileService
                .findDeviceProfileNamesByTenantId(tenantId, false);

        assertThat(deviceProfileInfos).isNotNull();
        assertThat(deviceProfileInfos).hasSize(deviceProfilesCount);
        assertThat(deviceProfileInfos).isEqualTo(sortedProfileInfos);
    }

    @Test
    public void testFindActiveOnlyDeviceProfilesByTenantId() {

        String profileCName = "profile C";
        deviceProfileService.saveDeviceProfile(
                createDeviceProfile(tenantId, profileCName));

        String profileAName = "profile A";
        deviceProfileService.saveDeviceProfile(
                createDeviceProfile(tenantId, profileAName));

        String profileBName = "profile B";
        deviceProfileService.saveDeviceProfile(
                createDeviceProfile(tenantId, profileBName));


        var deviceProfileInfos = deviceProfileService
                .findDeviceProfileNamesByTenantId(tenantId, true);

        assertThat(deviceProfileInfos).isNotNull();
        assertThat(deviceProfileInfos).isEmpty();

        var deviceC = new Device();
        deviceC.setName("Test Device C");
        deviceC.setType(profileCName);
        deviceC.setTenantId(tenantId);

        deviceC = deviceService.saveDevice(deviceC);

        var deviceA = new Device();
        deviceA.setName("Test Device A");
        deviceA.setType(profileAName);
        deviceA.setTenantId(tenantId);

        deviceA = deviceService.saveDevice(deviceA);

        var deviceB = new Device();
        deviceB.setName("Test Device B");
        deviceB.setType(profileBName);
        deviceB.setTenantId(tenantId);

        deviceB = deviceService.saveDevice(deviceB);

        deviceProfileInfos = deviceProfileService
                .findDeviceProfileNamesByTenantId(tenantId, true);

        var expected = List.of(
                new EntityInfo(deviceA.getDeviceProfileId(), profileAName),
                new EntityInfo(deviceB.getDeviceProfileId(), profileBName),
                new EntityInfo(deviceC.getDeviceProfileId(), profileCName)
        );

        assertThat(deviceProfileInfos).isNotEmpty();
        assertThat(deviceProfileInfos).hasSize(3);
        assertThat(deviceProfileInfos).isEqualTo(expected);
    }

}
