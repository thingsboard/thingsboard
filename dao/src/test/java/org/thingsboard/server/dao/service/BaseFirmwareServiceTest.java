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
    private static final String CONTENT_TYPE = "text/plain";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{0});

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
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
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

        savedFirmware.setTitle("My new firmware");
        firmwareService.saveFirmware(savedFirmware);

        Firmware foundFirmware = firmwareService.findFirmwareById(tenantId, savedFirmware.getId());
        Assert.assertEquals(foundFirmware.getTitle(), savedFirmware.getTitle());

        firmwareService.deleteFirmware(tenantId, savedFirmware.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyTenant() {
        Firmware firmware = new Firmware();
        firmware.setTitle(TITLE);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyTitle() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyFileName() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyContentType() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setFileName(FILE_NAME);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithEmptyData() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmwareService.saveFirmware(firmware);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveFirmwareWithInvalidTenant() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(new TenantId(Uuids.timeBased()));
        firmware.setTitle(TITLE);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setData(DATA);
        firmwareService.saveFirmware(firmware);
    }



    @Test(expected = DataValidationException.class)
    public void testDeleteFirmwareWithReferenceByDevice() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
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
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
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
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setData(DATA);
        Firmware savedFirmware = firmwareService.saveFirmware(firmware);

        Firmware foundFirmware = firmwareService.findFirmwareById(tenantId, savedFirmware.getId());
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, foundFirmware);
        firmwareService.deleteFirmware(tenantId, savedFirmware.getId());
    }

    @Test
    public void testDeleteFirmware() {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setTitle(TITLE);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
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
            firmware.setFileName(FILE_NAME);
            firmware.setContentType(CONTENT_TYPE);
            firmware.setData(DATA);
            firmwares.add(new FirmwareInfo(firmwareService.saveFirmware(firmware)));
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

}
