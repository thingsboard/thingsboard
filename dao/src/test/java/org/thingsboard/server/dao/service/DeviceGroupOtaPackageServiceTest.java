// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import lombok.Getter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.exception.DataValidationException;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;

@DaoSqlTest
public class DeviceGroupOtaPackageServiceTest extends AbstractServiceTest {

    private static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    private static final ChecksumAlgorithm CHECKSUM_ALGORITHM = ChecksumAlgorithm.SHA256;
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{1});

    @Getter
    @Autowired
    EntityGroupService entityGroupService;
    @Autowired
    DeviceGroupOtaPackageService deviceGroupOtaPackageService;
    @Autowired
    DeviceProfileService deviceProfileService;
    @Getter
    @Autowired
    DeviceService deviceService;
    @Autowired
    OtaPackageService otaPackageService;

    private DeviceProfileId deviceProfileId;

    @Before
    public void before() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedDeviceProfile);
        deviceProfileId = savedDeviceProfile.getId();
    }

    private OtaPackageInfo createOtaPackage(String title, DeviceProfileId deviceProfileId) {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        firmware.setDataSize((long) DATA.capacity());
        OtaPackage savedOtaPackage = otaPackageService.saveOtaPackage(firmware);
        Assert.assertNotNull(savedOtaPackage);
        return savedOtaPackage;
    }

    @Test
    public void testSaveDeviceGroupOtaPackage() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        DeviceGroupOtaPackage savedDgf = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        Assert.assertNotNull(savedDgf);
        Assert.assertNotNull(savedDgf.getId());
        Assert.assertTrue(savedDgf.getOtaPackageUpdateTime() > 0);
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithEmptyDeviceGroup() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());

        assertThatThrownBy(() -> deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("DeviceGroupOtaPackage should be assigned to entity group!");
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithInvalidDeviceGroup() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setGroupId(new EntityGroupId(UUID.randomUUID()));
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());

        assertThatThrownBy(() -> deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("OtaPackage is referencing to non-existent entity group!");
    }

    @Test
    public void testSaveGroupOtaPackageWithInvalidEntityGroupType() {
        EntityGroup assetGroup = createEntityGroup(tenantId, EntityType.ASSET, "Test assets");

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(assetGroup.getId());

        assertThatThrownBy(() -> deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("DeviceGroupOtaPackage can be only assigned to the Device group!");
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithEmptyType() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());

        assertThatThrownBy(() -> deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Type should be specified!");
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithEmptyOtaPackage() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());

        assertThatThrownBy(() -> deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("DeviceGroupOtaPackage should be assigned to OtaPackage!");
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithInvalidOtaPackage() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setOtaPackageId(new OtaPackageId(UUID.randomUUID()));

        assertThatThrownBy(() -> deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("DeviceGroupOtaPackage is referencing to non-existent OtaPackage!");
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithInvalidOtaPackageType() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackage.setOtaPackageType(SOFTWARE);
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());

        assertThatThrownBy(() -> deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("DeviceGroupOtaPackage type should be the same as OtaPackage type!");
    }

    @Test
    public void testFindDeviceGroupOtaPackageById() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        DeviceGroupOtaPackage savedDgf = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        Assert.assertNotNull(savedDgf);

        DeviceGroupOtaPackage foundDfg = deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(savedDgf.getId());
        Assert.assertNotNull(foundDfg);
        Assert.assertEquals(savedDgf, foundDfg);
    }

    @Test
    public void testFindDeviceGroupOtaPackageByGroupIdAndOtaPackageType() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        DeviceGroupOtaPackage savedDgf = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        Assert.assertNotNull(savedDgf);

        DeviceGroupOtaPackage foundDfg = deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(deviceGroup.getId(), firmware.getType());
        Assert.assertNotNull(foundDfg);
        Assert.assertEquals(savedDgf, foundDfg);
    }

    @Test
    public void testDeleteDeviceGroupOtaPackage() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        DeviceGroupOtaPackage savedDgf = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        Assert.assertNotNull(savedDgf);

        deviceGroupOtaPackageService.deleteDeviceGroupOtaPackage(tenantId, savedDgf);

        DeviceGroupOtaPackage foundDfg = deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(savedDgf.getId());
        Assert.assertNull(foundDfg);

        Assert.assertTrue(entityGroupService.isEntityInGroup(tenantId, device.getId(), deviceGroup.getId()));
    }

    @Test
    public void testDeviceGroupOtaPackageDeletionOnDeleteOta() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        deviceGroupOtaPackage = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);

        assertThat(otaPackageService.findOtaPackageById(tenantId, firmware.getId())).isNotNull();
        assertThat(deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(deviceGroupOtaPackage.getId())).isNotNull();

        otaPackageService.deleteOtaPackage(tenantId, firmware.getId());

        assertThat(deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(deviceGroupOtaPackage.getId())).isNull();
    }

}
