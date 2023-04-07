/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.ota.TbMultipartFile;
import org.thingsboard.server.dao.ota.util.ChecksumUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

public abstract class BaseOtaPackageServiceTest extends AbstractServiceTest {

    public static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    private static final ChecksumAlgorithm CHECKSUM_ALGORITHM = ChecksumAlgorithm.SHA256;
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final long DATA_SIZE = 1L;
    private static final byte[] DATA = new byte[]{1};
    private static final String URL = "http://firmware.test.org";

    private final IdComparator<OtaPackageInfo> idComparator = new IdComparator<>();

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

    @SuppressWarnings("deprecation")
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantProfileService.deleteTenantProfiles(tenantId);
    }

    @Test
    public void testSaveOtaPackageWithMaxSumDataSizeOutOfLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxOtaPackagesInBytes(DATA_SIZE).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);

        Assert.assertEquals(0, otaPackageService.sumDataSizeByTenantId(tenantId));

        createAndSaveFirmware(tenantId, "1");
        Assert.assertEquals(1, otaPackageService.sumDataSizeByTenantId(tenantId));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage(String.format("Failed to create the ota package, files size limit is exhausted %d bytes!", DATA_SIZE));
        createAndSaveFirmware(tenantId, "2");
    }

    @Test
    public void sumDataSizeByTenantId() {
        Assert.assertEquals(0, otaPackageService.sumDataSizeByTenantId(tenantId));

        createAndSaveFirmware(tenantId, "0.1");
        Assert.assertEquals(1, otaPackageService.sumDataSizeByTenantId(tenantId));

        int maxSumDataSize = 8;
        List<OtaPackage> packages = new ArrayList<>(maxSumDataSize);

        for (int i = 2; i <= maxSumDataSize; i++) {
            packages.add(createAndSaveFirmware(tenantId, "0." + i));
            Assert.assertEquals(i, otaPackageService.sumDataSizeByTenantId(tenantId));
        }

        Assert.assertEquals(maxSumDataSize, otaPackageService.sumDataSizeByTenantId(tenantId));
    }

    @Test
    @Transactional
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
        firmware.setData(new ByteArrayInputStream(DATA));
        firmware.setDataSize(DATA_SIZE);
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));

        Assert.assertNotNull(savedFirmware);
        Assert.assertNotNull(savedFirmware.getId());
        Assert.assertTrue(savedFirmware.getCreatedTime() > 0);
        Assert.assertEquals(firmware.getTenantId(), savedFirmware.getTenantId());
        Assert.assertEquals(firmware.getTitle(), savedFirmware.getTitle());
        Assert.assertEquals(firmware.getFileName(), savedFirmware.getFileName());
        Assert.assertEquals(firmware.getContentType(), savedFirmware.getContentType());
        Assert.assertEquals(firmware.getData(), savedFirmware.getData());

//        savedFirmware.setAdditionalInfo(JacksonUtil.newObjectNode());
//        savedFirmware.setData(new ByteArrayInputStream(DATA));
//        otaPackageService.saveOtaPackage(savedFirmware, new TestTbMultipartFile(file));

        OtaPackage foundFirmware = otaPackageService.findOtaPackageById(tenantId, savedFirmware.getId());
        Assert.assertEquals(foundFirmware.getTitle(), savedFirmware.getTitle());

        otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
    }

    @Test
    public void testSaveFirmwareWithUrl() {
        OtaPackageInfo firmware = new OtaPackageInfo();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setUrl(URL);
        firmware.setDataSize(0L);
        OtaPackageInfo savedFirmware = otaPackageService.saveOtaPackageInfo(firmware, true);

        Assert.assertNotNull(savedFirmware);
        Assert.assertNotNull(savedFirmware.getId());
        Assert.assertTrue(savedFirmware.getCreatedTime() > 0);
        Assert.assertEquals(firmware.getTenantId(), savedFirmware.getTenantId());
        Assert.assertEquals(firmware.getTitle(), savedFirmware.getTitle());
        Assert.assertEquals(firmware.getFileName(), savedFirmware.getFileName());
        Assert.assertEquals(firmware.getContentType(), savedFirmware.getContentType());

        savedFirmware.setAdditionalInfo(JacksonUtil.newObjectNode());
        otaPackageService.saveOtaPackageInfo(savedFirmware, true);
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
        OtaPackageInfo savedFirmwareInfo = otaPackageService.saveOtaPackageInfo(firmwareInfo, false);

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
        firmware.setData(new ByteArrayInputStream(DATA));
        firmware.setDataSize(DATA_SIZE);

        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));

        savedFirmwareInfo = otaPackageService.findOtaPackageInfoById(tenantId, savedFirmwareInfo.getId());
        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());
        otaPackageService.saveOtaPackageInfo(savedFirmwareInfo, false);

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
        firmware.setData(new ByteArrayInputStream(DATA));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage should be assigned to tenant!");
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));
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
        firmware.setData(new ByteArrayInputStream(DATA));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("Type should be specified!");
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));
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
        firmware.setData(new ByteArrayInputStream(DATA));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage title should be specified!");
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));
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
        firmware.setData(new ByteArrayInputStream(DATA));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage file name should be specified!");
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));    }

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
        firmware.setData(new ByteArrayInputStream(DATA));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage content type should be specified!");
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));    }

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
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));    }

    @Test
    public void testSaveFirmwareWithInvalidTenant() {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(new ByteArrayInputStream(DATA));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage is referencing to non-existent tenant!");
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));    }

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
        firmware.setData(new ByteArrayInputStream(DATA));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage is referencing to non-existent device profile!");
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));
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
        firmware.setData(new ByteArrayInputStream(DATA));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage checksum should be specified!");
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));
    }

    @Test
    public void testSaveFirmwareInfoWithExistingTitleAndVersion() {
        OtaPackageInfo firmwareInfo = new OtaPackageInfo();
        firmwareInfo.setTenantId(tenantId);
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        otaPackageService.saveOtaPackageInfo(firmwareInfo, false);

        OtaPackageInfo newFirmwareInfo = new OtaPackageInfo();
        newFirmwareInfo.setTenantId(tenantId);
        newFirmwareInfo.setDeviceProfileId(deviceProfileId);
        newFirmwareInfo.setType(FIRMWARE);
        newFirmwareInfo.setTitle(TITLE);
        newFirmwareInfo.setVersion(VERSION);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage with such title and version already exists!");
        otaPackageService.saveOtaPackageInfo(newFirmwareInfo, false);
    }

    @Test
    public void testSaveFirmwareWithExistingTitleAndVersion() {
        createAndSaveFirmware(tenantId, VERSION);
        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage with such title and version already exists!");
        createAndSaveFirmware(tenantId, VERSION);
    }

    @Test
    public void testDeleteFirmwareWithReferenceByDevice() {
        OtaPackage savedFirmware = createAndSaveFirmware(tenantId, VERSION);

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
    @Transactional
    public void testUpdateDeviceProfileId() {
        OtaPackage savedFirmware = createAndSaveFirmware(tenantId, VERSION);

        try {
            thrown.expect(DataValidationException.class);
            thrown.expectMessage("Updating otaPackage deviceProfile is prohibited!");
            savedFirmware.setDeviceProfileId(null);
            savedFirmware.setData(new ByteArrayInputStream(DATA));
            MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
            otaPackageService.saveOtaPackage(savedFirmware, new TestTbMultipartFile(file));
        } finally {
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
        firmware.setData(new ByteArrayInputStream(DATA));
        firmware.setDataSize(DATA_SIZE);
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(file));

        savedDeviceProfile.setFirmwareId(savedFirmware.getId());
        deviceProfileService.saveDeviceProfile(savedDeviceProfile);

        try {
            thrown.expect(DataValidationException.class);
            thrown.expectMessage("The otaPackage referenced by the device profile cannot be deleted!");
            otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
        } finally {
            deviceProfileService.deleteDeviceProfile(tenantId, savedDeviceProfile.getId());
        }
    }

    @Test
    @Transactional
    public void testFindFirmwareById() {
        OtaPackage savedFirmware = createAndSaveFirmware(tenantId, VERSION);

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
        OtaPackageInfo savedFirmware = otaPackageService.saveOtaPackageInfo(firmware, false);

        OtaPackageInfo foundFirmware = otaPackageService.findOtaPackageInfoById(tenantId, savedFirmware.getId());
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, foundFirmware);
        otaPackageService.deleteOtaPackage(tenantId, savedFirmware.getId());
    }

    @Test
    @Transactional
    public void testDeleteFirmware() {
        OtaPackage savedFirmware = createAndSaveFirmware(tenantId, VERSION);

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
            OtaPackageInfo info = new OtaPackageInfo(createAndSaveFirmware(tenantId, VERSION + i));
            info.setHasData(true);
            firmwares.add(info);
        }

        OtaPackageInfo firmwareWithUrl = new OtaPackageInfo();
        firmwareWithUrl.setTenantId(tenantId);
        firmwareWithUrl.setDeviceProfileId(deviceProfileId);
        firmwareWithUrl.setType(FIRMWARE);
        firmwareWithUrl.setTitle(TITLE);
        firmwareWithUrl.setVersion(VERSION);
        firmwareWithUrl.setUrl(URL);
        firmwareWithUrl.setDataSize(0L);

        OtaPackageInfo savedFwWithUrl = otaPackageService.saveOtaPackageInfo(firmwareWithUrl, true);
        savedFwWithUrl.setHasData(true);

        firmwares.add(savedFwWithUrl);

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

        assertThat(firmwares).isEqualTo(loadedFirmwares);

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
            firmwares.add(new OtaPackageInfo(otaPackageService.saveOtaPackage(createFirmware(tenantId, VERSION + i, deviceProfileId), new TestTbMultipartFile(new MockMultipartFile(FILE_NAME, new byte[]{1})))));
        }

        OtaPackageInfo firmwareWithUrl = new OtaPackageInfo();
        firmwareWithUrl.setTenantId(tenantId);
        firmwareWithUrl.setDeviceProfileId(deviceProfileId);
        firmwareWithUrl.setType(FIRMWARE);
        firmwareWithUrl.setTitle(TITLE);
        firmwareWithUrl.setVersion(VERSION);
        firmwareWithUrl.setUrl(URL);
        firmwareWithUrl.setDataSize(0L);

        OtaPackageInfo savedFwWithUrl = otaPackageService.saveOtaPackageInfo(firmwareWithUrl, true);
        savedFwWithUrl.setHasData(true);

        firmwares.add(savedFwWithUrl);

        List<OtaPackageInfo> loadedFirmwares = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = otaPackageService.findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(tenantId, deviceProfileId, FIRMWARE, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        loadedFirmwares = new ArrayList<>();
        pageLink = new PageLink(16);
        do {
            pageData = otaPackageService.findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(tenantId, deviceProfileId, FIRMWARE, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(firmwares, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        assertThat(firmwares).isEqualTo(loadedFirmwares);

        otaPackageService.deleteOtaPackagesByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = otaPackageService.findTenantOtaPackagesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testSaveOtaPackageInfoWithBlankAndEmptyUrl() {
        OtaPackageInfo firmwareInfo = new OtaPackageInfo();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUrl("   ");
        thrown.expect(DataValidationException.class);
        thrown.expectMessage("Ota package URL should be specified!");
        otaPackageService.saveOtaPackageInfo(firmwareInfo, true);
        firmwareInfo.setUrl("");
        otaPackageService.saveOtaPackageInfo(firmwareInfo, true);
    }

    @Test
    public void testSaveOtaPackageUrlCantBeUpdated() {
        OtaPackageInfo firmwareInfo = new OtaPackageInfo();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUrl(URL);
        firmwareInfo.setTenantId(tenantId);

        OtaPackageInfo savedFirmwareInfo = otaPackageService.saveOtaPackageInfo(firmwareInfo, true);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("Updating otaPackage URL is prohibited!");

        savedFirmwareInfo.setUrl("https://newurl.com");
        otaPackageService.saveOtaPackageInfo(savedFirmwareInfo, true);
    }

    @Test
    public void testSaveOtaPackageCantViolateSizeOfTitle() {
        OtaPackageInfo firmwareInfo = new OtaPackageInfo();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(RandomStringUtils.random(257));
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUrl(URL);
        firmwareInfo.setTenantId(tenantId);

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("length of title must be equal or less than 255");

        otaPackageService.saveOtaPackageInfo(firmwareInfo, true);
    }

    @Test
    public void testSaveOtaPackageCantViolateSizeOfVersion() {
        OtaPackageInfo firmwareInfo = new OtaPackageInfo();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setUrl(URL);
        firmwareInfo.setTenantId(tenantId);
        firmwareInfo.setTitle(TITLE);

        firmwareInfo.setVersion(RandomStringUtils.random(257));
        thrown.expectMessage("length of version must be equal or less than 255");

        otaPackageService.saveOtaPackageInfo(firmwareInfo, true);
    }

    @Test
    @SneakyThrows
    public void testGettingCorrectFileWithOtaData() {
        OtaPackage firmware = createFirmware(tenantId, "24687846", deviceProfileId);
        firmware = otaPackageService.saveOtaPackage(firmware, new TestTbMultipartFile(new MockMultipartFile(FILE_NAME, new byte[]{1})));
        File file = otaPackageService.getOtaDataFile(tenantId, firmware.getId());
        try {
            assertEquals(firmware.getChecksum(), ChecksumUtil.generateChecksum(CHECKSUM_ALGORITHM, new FileInputStream(file)));
        } catch (FileNotFoundException e){
            throw new RuntimeException(e);
        }
    }

    private OtaPackage createAndSaveFirmware(TenantId tenantId, String version) {
        MockMultipartFile file = new MockMultipartFile(FILE_NAME, new byte[]{1});
        return otaPackageService.saveOtaPackage(createFirmware(tenantId, version, deviceProfileId), new TestTbMultipartFile(file));
    }

    public static OtaPackage createFirmware(
            TenantId tenantId,
            String version,
            DeviceProfileId deviceProfileId
    ) {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(version);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(new ByteArrayInputStream(DATA));
        firmware.setDataSize(DATA_SIZE);
        return firmware;
    }

    private class TestTbMultipartFile implements TbMultipartFile {
        private final MockMultipartFile file;

        private TestTbMultipartFile(MockMultipartFile file) {
            this.file = file;
        }

        @Override
        public Optional<InputStream> getInputStream() {
            try {
                return Optional.of(file.getInputStream());
            } catch (IOException e) {
                return Optional.empty();
            }
        }

        @Override
        public String getFileName() {
            return file.getName();
        }

        @Override
        public long getFileSize() {
            return file.getSize();

        }

        @Override
        public String getContentType() {
            return file.getContentType();
        }
    }
}