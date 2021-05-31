/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

public abstract class BaseOtaPackageServiceTest extends AbstractServiceTest {

    public static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    private static final ChecksumAlgorithm CHECKSUM_ALGORITHM = ChecksumAlgorithm.SHA256;
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{1});

    private IdComparator<OtaPackageInfo> idComparator = new IdComparator<>();

    private TenantId tenantId;

    private DeviceProfileId deviceProfileId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();

        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedDeviceProfile);
        deviceProfileId = savedDeviceProfile.getId();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveFirmware() {
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
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        Assert.assertNotNull(savedFirmware);
        Assert.assertNotNull(savedFirmware.getId());
        Assert.assertTrue(savedFirmware.getCreatedTime() > 0);
        Assert.assertEquals(firmware.getTenantId(), savedFirmware.getTenantId());
        Assert.assertEquals(firmware.getTitle(), savedFirmware.getTitle());
        Assert.assertEquals(firmware.getFileName(), savedFirmware.getFileName());
        Assert.assertEquals(firmware.getContentType(), savedFirmware.getContentType());
        Assert.assertEquals(firmware.getData(), savedFirmware.getData());

        savedFirmware.setAdditionalInfo(JacksonUtil.newObjectNode());
        otaPackageService.saveOtaPackage(savedFirmware);

        OtaPackage foundFirmware = otaPackageService.findOtaPackageById(tenantId, savedFirmware.getId());
        Assert.assertEquals(foundFirmware.getTitle(), savedFirmware.getTitle());

        otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
    }

    @Test
    public void testSaveFirmwareInfoAndUpdateWithData() {
        OtaPackageInfo firmwareInfo = new OtaPackageInfo();
        firmwareInfo.setTenantId(tenantId);
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        OtaPackageInfo savedFirmwareInfo = otaPackageService.saveOtaPackageInfo(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(firmwareInfo.getTenantId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());

        OtaPackage firmware = new OtaPackage(savedFirmwareInfo.getId());
        firmware.setCreatedTime(firmwareInfo.getCreatedTime());
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

        otaPackageService.saveOtaPackage(firmware);

        savedFirmwareInfo = otaPackageService.findOtaPackageInfoById(tenantId, savedFirmwareInfo.getId());
        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());
        otaPackageService.saveOtaPackageInfo(savedFirmwareInfo);

        OtaPackage foundFirmware = otaPackageService.findOtaPackageById(tenantId, firmware.getId());
        firmware.setAdditionalInfo(JacksonUtil.newObjectNode());

        Assert.assertEquals(foundFirmware.getTitle(), firmware.getTitle());
        Assert.assertTrue(foundFirmware.isHasData());

        otaPackageService.deleteOtaPackage(tenantId, savedFirmwareInfo.getId());
    }

    @Test
    public void testSaveFirmwareWithEmptyTenant() {
        OtaPackage firmware = new OtaPackage();
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage should be assigned to tenant!");
        otaPackageService.saveOtaPackage(firmware);
    }

    @Test
    public void testSaveFirmwareWithEmptyType() {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("Type should be specified!");
        otaPackageService.saveOtaPackage(firmware);
    }

    @Test
    public void testSaveFirmwareWithEmptyTitle() {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage title should be specified!");
        otaPackageService.saveOtaPackage(firmware);
    }

    @Test
    public void testSaveFirmwareWithEmptyFileName() {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage file name should be specified!");
        otaPackageService.saveOtaPackage(firmware);
    }

    @Test
    public void testSaveFirmwareWithEmptyContentType() {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage content type should be specified!");
        otaPackageService.saveOtaPackage(firmware);
    }

    @Test
    public void testSaveFirmwareWithEmptyData() {
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

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage data should be specified!");
        otaPackageService.saveOtaPackage(firmware);
    }

    @Test
    public void testSaveFirmwareWithInvalidTenant() {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(new TenantId(Uuids.timeBased()));
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage is referencing to non-existent tenant!");
        otaPackageService.saveOtaPackage(firmware);
    }

    @Test
    public void testSaveFirmwareWithInvalidDeviceProfileId() {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(new DeviceProfileId(Uuids.timeBased()));
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage is referencing to non-existent device profile!");
        otaPackageService.saveOtaPackage(firmware);
    }

    @Test
    public void testSaveFirmwareWithEmptyChecksum() {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setData(DATA);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage checksum should be specified!");
        otaPackageService.saveOtaPackage(firmware);
    }

    @Test
    public void testSaveFirmwareInfoWithExistingTitleAndVersion() {
        OtaPackageInfo firmwareInfo = new OtaPackageInfo();
        firmwareInfo.setTenantId(tenantId);
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        otaPackageService.saveOtaPackageInfo(firmwareInfo);

        OtaPackageInfo newFirmwareInfo = new OtaPackageInfo();
        newFirmwareInfo.setTenantId(tenantId);
        newFirmwareInfo.setDeviceProfileId(deviceProfileId);
        newFirmwareInfo.setType(FIRMWARE);
        newFirmwareInfo.setTitle(TITLE);
        newFirmwareInfo.setVersion(VERSION);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage with such title and version already exists!");
        otaPackageService.saveOtaPackageInfo(newFirmwareInfo);
    }

    @Test
    public void testSaveFirmwareWithExistingTitleAndVersion() {
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
        otaPackageService.saveOtaPackage(firmware);

        OtaPackage newFirmware = new OtaPackage();
        newFirmware.setTenantId(tenantId);
        newFirmware.setDeviceProfileId(deviceProfileId);
        newFirmware.setType(FIRMWARE);
        newFirmware.setTitle(TITLE);
        newFirmware.setVersion(VERSION);
        newFirmware.setFileName(FILE_NAME);
        newFirmware.setContentType(CONTENT_TYPE);
        newFirmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        newFirmware.setChecksum(CHECKSUM);
        newFirmware.setData(DATA);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage with such title and version already exists!");
        otaPackageService.saveOtaPackage(newFirmware);
    }

    @Test
    public void testDeleteFirmwareWithReferenceByDevice() {
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
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setDeviceProfileId(deviceProfileId);
        device.setFirmwareId(savedFirmware.getId());
        Device savedDevice = deviceService.saveDevice(device);

        try {
            thrown.expect(DataValidationException.class);
            thrown.expectMessage("The otaPackage referenced by the devices cannot be deleted!");
            otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
        } finally {
            deviceService.deleteDevice(tenantId, savedDevice.getId());
            otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
        }
    }

    @Test
    public void testUpdateDeviceProfileIdWithReferenceByDevice() {
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
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setDeviceProfileId(deviceProfileId);
        device.setFirmwareId(savedFirmware.getId());
        Device savedDevice = deviceService.saveDevice(device);

        try {
            thrown.expect(DataValidationException.class);
            thrown.expectMessage("Can`t update deviceProfileId because otaPackage is already in use!");
            savedFirmware.setDeviceProfileId(null);
            otaPackageService.saveOtaPackage(savedFirmware);
        } finally {
            deviceService.deleteDevice(tenantId, savedDevice.getId());
            otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
        }
    }

    @Test
    public void testDeleteFirmwareWithReferenceByDeviceProfile() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Test Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(savedDeviceProfile.getId());
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        savedDeviceProfile.setFirmwareId(savedFirmware.getId());
        deviceProfileService.saveDeviceProfile(savedDeviceProfile);

        try {
            thrown.expect(DataValidationException.class);
            thrown.expectMessage("The otaPackage referenced by the device profile cannot be deleted!");
            otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
        } finally {
            deviceProfileService.deleteDeviceProfile(tenantId, savedDeviceProfile.getId());
            otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
        }
    }

    @Test
    public void testUpdateDeviceProfileIdWithReferenceByDeviceProfile() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Test Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(savedDeviceProfile.getId());
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        savedDeviceProfile.setFirmwareId(savedFirmware.getId());
        deviceProfileService.saveDeviceProfile(savedDeviceProfile);

        try {
            thrown.expect(DataValidationException.class);
            thrown.expectMessage("Can`t update deviceProfileId because otaPackage is already in use!");
            savedFirmware.setDeviceProfileId(null);
            otaPackageService.saveOtaPackage(savedFirmware);
        } finally {
            deviceProfileService.deleteDeviceProfile(tenantId, savedDeviceProfile.getId());
            otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
        }
    }

    @Test
    public void testFindFirmwareById() {
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
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        OtaPackage foundFirmware = otaPackageService.findOtaPackageById(tenantId, savedFirmware.getId());
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, foundFirmware);
        otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
    }

    @Test
    public void testFindFirmwareInfoById() {
        OtaPackageInfo firmware = new OtaPackageInfo();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        OtaPackageInfo savedFirmware = otaPackageService.saveOtaPackageInfo(firmware);

        OtaPackageInfo foundFirmware = otaPackageService.findOtaPackageInfoById(tenantId, savedFirmware.getId());
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, foundFirmware);
        otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
    }

    @Test
    public void testDeleteFirmware() {
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
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        OtaPackage foundFirmware = otaPackageService.findOtaPackageById(tenantId, savedFirmware.getId());
        Assert.assertNotNull(foundFirmware);
        otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
        foundFirmware = otaPackageService.findOtaPackageById(tenantId, savedFirmware.getId());
        Assert.assertNull(foundFirmware);
    }

    @Test
    public void testFindTenantFirmwaresByTenantId() {
        List<OtaPackageInfo> firmwares = new ArrayList<>();
        for (int i = 0; i < 165; i++) {
            OtaPackage firmware = new OtaPackage();
            firmware.setTenantId(tenantId);
            firmware.setDeviceProfileId(deviceProfileId);
            firmware.setType(FIRMWARE);
            firmware.setTitle(TITLE);
            firmware.setVersion(VERSION + i);
            firmware.setFileName(FILE_NAME);
            firmware.setContentType(CONTENT_TYPE);
            firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
            firmware.setChecksum(CHECKSUM);
            firmware.setData(DATA);

            OtaPackageInfo info = new OtaPackageInfo(otaPackageService.saveOtaPackage(firmware));
            info.setHasData(true);
            firmwares.add(info);
        }

        List<OtaPackageInfo> loadedFirmwares = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = otaPackageService.findTenantOtaPackagesByTenantId(tenantId, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(firmwares, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(firmwares, loadedFirmwares);

        otaPackageService.deleteOtaPackagesByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = otaPackageService.findTenantOtaPackagesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindTenantFirmwaresByTenantIdAndHasData() {
        List<OtaPackageInfo> firmwares = new ArrayList<>();
        for (int i = 0; i < 165; i++) {
            OtaPackageInfo firmwareInfo = new OtaPackageInfo();
            firmwareInfo.setTenantId(tenantId);
            firmwareInfo.setDeviceProfileId(deviceProfileId);
            firmwareInfo.setType(FIRMWARE);
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);
            firmwareInfo.setFileName(FILE_NAME);
            firmwareInfo.setContentType(CONTENT_TYPE);
            firmwareInfo.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
            firmwareInfo.setChecksum(CHECKSUM);
            firmwareInfo.setDataSize((long) DATA.array().length);
            firmwares.add(otaPackageService.saveOtaPackageInfo(firmwareInfo));
        }

        List<OtaPackageInfo> loadedFirmwares = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = otaPackageService.findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(tenantId, deviceProfileId, FIRMWARE, false, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(firmwares, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(firmwares, loadedFirmwares);

        firmwares.forEach(f -> {
            OtaPackage firmware = new OtaPackage(f.getId());
            firmware.setCreatedTime(f.getCreatedTime());
            firmware.setTenantId(f.getTenantId());
            firmware.setDeviceProfileId(deviceProfileId);
            firmware.setType(FIRMWARE);
            firmware.setTitle(f.getTitle());
            firmware.setVersion(f.getVersion());
            firmware.setFileName(FILE_NAME);
            firmware.setContentType(CONTENT_TYPE);
            firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
            firmware.setChecksum(CHECKSUM);
            firmware.setData(DATA);
            firmware.setDataSize((long) DATA.array().length);
            otaPackageService.saveOtaPackage(firmware);
            f.setHasData(true);
        });

        loadedFirmwares = new ArrayList<>();
        pageLink = new PageLink(16);
        do {
            pageData = otaPackageService.findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(tenantId, deviceProfileId, FIRMWARE, true, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(firmwares, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(firmwares, loadedFirmwares);

        otaPackageService.deleteOtaPackagesByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = otaPackageService.findTenantOtaPackagesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

}
