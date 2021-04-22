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
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Firmware;
import org.thingsboard.server.common.data.FirmwareInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseFirmwareServiceTest extends AbstractServiceTest {

    public static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    private static final String CHECKSUM_ALGORITHM = "sha256";
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{1});

    private IdComparator<FirmwareInfo> idComparator = new IdComparator<>();

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
    public void testSaveFirmware() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        Firmware savedFirmware = firmwareService.saveFirmware(firmware);

        Assert.assertNotNull(savedFirmware);
        Assert.assertNotNull(savedFirmware.getId());
        Assert.assertTrue(savedFirmware.getCreatedTime() > 0);
        Assert.assertEquals(firmware.getTenantId(), savedFirmware.getTenantId());
        Assert.assertEquals(firmware.getTitle(), savedFirmware.getTitle());
        Assert.assertEquals(firmware.getFileName(), savedFirmware.getFileName());
        Assert.assertEquals(firmware.getContentType(), savedFirmware.getContentType());
        Assert.assertEquals(firmware.getData(), savedFirmware.getData());

        savedFirmware.setAdditionalInfo(JacksonUtil.newObjectNode());
        firmwareService.saveFirmware(savedFirmware);

        Firmware foundFirmware = firmwareService.findFirmwareById(tenantId, savedFirmware.getId());
        Assert.assertEquals(foundFirmware.getTitle(), savedFirmware.getTitle());

        firmwareService.deleteFirmware(tenantId, savedFirmware.getId());
    }

    @Test
    public void testSaveFirmwareInfoAndUpdateWithData() {
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        firmwareInfo.setTenantId(tenantId);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        FirmwareInfo savedFirmwareInfo = firmwareService.saveFirmwareInfo(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(firmwareInfo.getTenantId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());

        Firmware firmware = new Firmware(savedFirmwareInfo.getId());
        firmware.setCreatedTime(firmwareInfo.getCreatedTime());
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);

        firmwareService.saveFirmware(firmware);

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());
        firmwareService.saveFirmwareInfo(savedFirmwareInfo);

        Firmware foundFirmware = firmwareService.findFirmwareById(tenantId, firmware.getId());
        firmware.setAdditionalInfo(JacksonUtil.newObjectNode());

        Assert.assertEquals(foundFirmware.getTitle(), firmware.getTitle());
        Assert.assertTrue(foundFirmware.isHasData());

        firmwareService.deleteFirmware(tenantId, savedFirmwareInfo.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyTenant() {
        Firmware firmware = new Firmware();
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyTitle() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyFileName() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyContentType() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyData() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithInvalidTenant() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(new TenantId(Uuids.timeBased()));
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyChecksum() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(new TenantId(Uuids.timeBased()));
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareInfoWithExistingTitleAndVersion() {
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        firmwareInfo.setTenantId(tenantId);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareService.saveFirmwareInfo(firmwareInfo);

        FirmwareInfo newFirmwareInfo = new FirmwareInfo();
        newFirmwareInfo.setTenantId(tenantId);
        newFirmwareInfo.setTitle(TITLE);
        newFirmwareInfo.setVersion(VERSION);
        firmwareService.saveFirmwareInfo(newFirmwareInfo);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithExistingTitleAndVersion() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);

        Firmware newFirmware = new Firmware();
        newFirmware.setTenantId(tenantId);
        newFirmware.setTitle(TITLE);
        newFirmware.setVersion(VERSION);
        newFirmware.setFileName(FILE_NAME);
        newFirmware.setContentType(CONTENT_TYPE);
        newFirmware.setData(DATA);
        firmwareService.saveFirmware(newFirmware);
    }

    @Test(expected = DataValidationException.class)
    public void testDeleteFirmwareWithReferenceByDevice() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        Firmware savedFirmware = firmwareService.saveFirmware(firmware);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        device.setFirmwareId(savedFirmware.getId());
        Device savedDevice = deviceService.saveDevice(device);

        try {
            firmwareService.deleteFirmware(tenantId, savedFirmware.getId());
        } finally {
            deviceService.deleteDevice(tenantId, savedDevice.getId());
            firmwareService.deleteFirmware(tenantId, savedFirmware.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testDeleteFirmwareWithReferenceByDeviceProfile() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        Firmware savedFirmware = firmwareService.saveFirmware(firmware);

        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        deviceProfile.setFirmwareId(savedFirmware.getId());
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        try {
            firmwareService.deleteFirmware(tenantId, savedFirmware.getId());
        } finally {
            deviceProfileService.deleteDeviceProfile(tenantId, savedDeviceProfile.getId());
            firmwareService.deleteFirmware(tenantId, savedFirmware.getId());
        }
    }

    @Test
    public void testFindFirmwareById() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        Firmware savedFirmware = firmwareService.saveFirmware(firmware);

        Firmware foundFirmware = firmwareService.findFirmwareById(tenantId, savedFirmware.getId());
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, foundFirmware);
        firmwareService.deleteFirmware(tenantId, savedFirmware.getId());
    }

    @Test
    public void testFindFirmwareInfoById() {
        FirmwareInfo firmware = new FirmwareInfo();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        FirmwareInfo savedFirmware = firmwareService.saveFirmwareInfo(firmware);

        FirmwareInfo foundFirmware = firmwareService.findFirmwareInfoById(tenantId, savedFirmware.getId());
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, foundFirmware);
        firmwareService.deleteFirmware(tenantId, savedFirmware.getId());
    }

    @Test
    public void testDeleteFirmware() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        Firmware savedFirmware = firmwareService.saveFirmware(firmware);

        Firmware foundFirmware = firmwareService.findFirmwareById(tenantId, savedFirmware.getId());
        Assert.assertNotNull(foundFirmware);
        firmwareService.deleteFirmware(tenantId, savedFirmware.getId());
        foundFirmware = firmwareService.findFirmwareById(tenantId, savedFirmware.getId());
        Assert.assertNull(foundFirmware);
    }

    @Test
    public void testFindTenantFirmwaresByTenantId() {
        List<FirmwareInfo> firmwares = new ArrayList<>();
        for (int i = 0; i < 165; i++) {
            Firmware firmware = new Firmware();
            firmware.setTenantId(tenantId);
            firmware.setTitle(TITLE);
            firmware.setVersion(VERSION + i);
            firmware.setFileName(FILE_NAME);
            firmware.setContentType(CONTENT_TYPE);
            firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
            firmware.setChecksum(CHECKSUM);
            firmware.setData(DATA);

            FirmwareInfo info = new FirmwareInfo(firmwareService.saveFirmware(firmware));
            info.setHasData(true);
            firmwares.add(info);
        }

        List<FirmwareInfo> loadedFirmwares = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        PageData<FirmwareInfo> pageData;
        do {
            pageData = firmwareService.findTenantFirmwaresByTenantId(tenantId, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(firmwares, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(firmwares, loadedFirmwares);

        firmwareService.deleteFirmwaresByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = firmwareService.findTenantFirmwaresByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindTenantFirmwaresByTenantIdAndHasData() {
        List<FirmwareInfo> firmwares = new ArrayList<>();
        for (int i = 0; i < 165; i++) {
            FirmwareInfo firmwareInfo = new FirmwareInfo();
            firmwareInfo.setTenantId(tenantId);
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);
            firmwares.add(firmwareService.saveFirmwareInfo(firmwareInfo));
        }

        List<FirmwareInfo> loadedFirmwares = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        PageData<FirmwareInfo> pageData;
        do {
            pageData = firmwareService.findTenantFirmwaresByTenantIdAndHasData(tenantId, false, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(firmwares, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(firmwares, loadedFirmwares);

        firmwares.forEach(f -> {
            Firmware firmware = new Firmware(f.getId());
            firmware.setCreatedTime(f.getCreatedTime());
            firmware.setTenantId(f.getTenantId());
            firmware.setTitle(f.getTitle());
            firmware.setVersion(f.getVersion());
            firmware.setFileName(FILE_NAME);
            firmware.setContentType(CONTENT_TYPE);
            firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
            firmware.setChecksum(CHECKSUM);
            firmware.setData(DATA);
            firmwareService.saveFirmware(firmware);
            f.setHasData(true);
        });

        loadedFirmwares = new ArrayList<>();
        pageLink = new PageLink(16);
        do {
            pageData = firmwareService.findTenantFirmwaresByTenantIdAndHasData(tenantId, true, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(firmwares, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(firmwares, loadedFirmwares);

        firmwareService.deleteFirmwaresByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = firmwareService.findTenantFirmwaresByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

}
