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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.housekeeper.OtaDataDeletionHousekeeperTask;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.msg.housekeeper.HousekeeperClient;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.sql.ota.OtaPackageRepository;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

@DaoSqlTest
public class OtaPackageDataCleanupTest extends AbstractServiceTest {

    private static final String TITLE = "Test Firmware";
    private static final String FILE_NAME = "firmware.bin";
    private static final String CONTENT_TYPE = "application/octet-stream";
    private static final ChecksumAlgorithm CHECKSUM_ALGORITHM = ChecksumAlgorithm.SHA256;
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final long DATA_SIZE = 1L;
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{(int) DATA_SIZE});

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private OtaPackageService otaPackageService;

    @Autowired
    private OtaPackageRepository otaPackageRepository;

    @MockitoBean
    private HousekeeperClient housekeeperClient;

    private DeviceProfileId deviceProfileId;

    @Before
    public void before() {
        DeviceProfile deviceProfile = createDeviceProfile(tenantId, "Test Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedDeviceProfile);
        deviceProfileId = savedDeviceProfile.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testDeleteOtaPackage_shouldQueueCleanupTask() {
        OtaPackage savedPackage = otaPackageService.saveOtaPackage(createOtaPackage("v1.0"));
        Assert.assertNotNull(savedPackage);
        Assert.assertNotNull(savedPackage.getId());

        Long oid = otaPackageRepository.getDataOidById(savedPackage.getId().getId());
        Assert.assertNotNull("OID should exist after saving OTA package", oid);
        Assert.assertTrue("OID should be positive", oid > 0);

        otaPackageService.deleteOtaPackage(tenantId, savedPackage.getId());

        ArgumentCaptor<OtaDataDeletionHousekeeperTask> taskCaptor =
                ArgumentCaptor.forClass(OtaDataDeletionHousekeeperTask.class);
        verify(housekeeperClient, times(1)).submitTask(taskCaptor.capture());

        OtaDataDeletionHousekeeperTask capturedTask = taskCaptor.getValue();
        assertThat(capturedTask.getTaskType()).isEqualTo(HousekeeperTaskType.DELETE_OTA_DATA);
        assertThat(capturedTask.getTenantId()).isEqualTo(tenantId);
        assertThat(capturedTask.getEntityId()).isEqualTo(savedPackage.getId());
        assertThat(capturedTask.getOid()).isEqualTo(oid);
        assertThat(capturedTask.getOid()).isGreaterThan(0L);
    }

    @Test
    public void testDeleteOtaPackageWithoutData_shouldNotQueueCleanupTask() {
        OtaPackageInfo savedInfo = otaPackageService.saveOtaPackageInfo(createOtaPackageWithUrl(), true);
        Assert.assertNotNull(savedInfo);

        Long oid = otaPackageRepository.getDataOidById(savedInfo.getId().getId());
        Assert.assertNull("OID should not exist for URL-based package", oid);

        otaPackageService.deleteOtaPackage(tenantId, savedInfo.getId());

        verify(housekeeperClient, times(0)).submitTask(any(OtaDataDeletionHousekeeperTask.class));
    }

    @Test
    public void testUpdateOtaPackageInfo_shouldNotQueueCleanupTask() {
        OtaPackage savedPackage = otaPackageService.saveOtaPackage(createOtaPackage("v3.0"));
        Assert.assertNotNull(savedPackage);

        Long originalOid = otaPackageRepository.getDataOidById(savedPackage.getId().getId());

        OtaPackageInfo info = otaPackageService.findOtaPackageInfoById(tenantId, savedPackage.getId());
        Assert.assertNotNull(info);

        info.setAdditionalInfo(org.thingsboard.common.util.JacksonUtil.newObjectNode().put("test", "value"));
        OtaPackageInfo updatedInfo = otaPackageService.saveOtaPackageInfo(info, false);
        Assert.assertNotNull(updatedInfo);
        assertThat(updatedInfo.getAdditionalInfo().get("test").asText()).isEqualTo("value");

        Long currentOid = otaPackageRepository.getDataOidById(updatedInfo.getId().getId());
        Assert.assertEquals("OID should remain the same when only metadata is updated", originalOid, currentOid);

        verify(housekeeperClient, times(0)).submitTask(any(OtaDataDeletionHousekeeperTask.class));
    }

    private OtaPackage createOtaPackage(String version) {
        OtaPackage otaPackage = new OtaPackage();
        otaPackage.setTenantId(tenantId);
        otaPackage.setDeviceProfileId(deviceProfileId);
        otaPackage.setType(FIRMWARE);
        otaPackage.setTitle(TITLE);
        otaPackage.setVersion(version);
        otaPackage.setFileName(FILE_NAME);
        otaPackage.setContentType(CONTENT_TYPE);
        otaPackage.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        otaPackage.setChecksum(CHECKSUM);
        otaPackage.setData(DATA.duplicate());
        otaPackage.setDataSize(DATA_SIZE);
        return otaPackage;
    }

    private OtaPackageInfo createOtaPackageWithUrl() {
        OtaPackageInfo otaPackage = new OtaPackageInfo();
        otaPackage.setTenantId(tenantId);
        otaPackage.setDeviceProfileId(deviceProfileId);
        otaPackage.setType(FIRMWARE);
        otaPackage.setTitle(TITLE);
        otaPackage.setVersion("v2.0");
        otaPackage.setUrl("http://example.com/firmware.bin");
        otaPackage.setFileName(FILE_NAME);
        otaPackage.setContentType(CONTENT_TYPE);
        return otaPackage;
    }

}
