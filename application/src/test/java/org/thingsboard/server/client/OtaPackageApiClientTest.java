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
import org.thingsboard.client.model.ChecksumAlgorithm;
import org.thingsboard.client.model.DeviceProfileId;
import org.thingsboard.client.model.DeviceProfileInfo;
import org.thingsboard.client.model.OtaPackage;
import org.thingsboard.client.model.OtaPackageInfo;
import org.thingsboard.client.model.OtaPackageType;
import org.thingsboard.client.model.PageDataOtaPackageInfo;
import org.thingsboard.client.model.SaveOtaPackageInfoRequest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class OtaPackageApiClientTest extends AbstractApiClientTest {

    private static final String OTA_PREFIX = "OtaTest_";

    private DeviceProfileId getDefaultDeviceProfileId() throws Exception {
        DeviceProfileInfo profileInfo = client.getDefaultDeviceProfileInfo();
        return (DeviceProfileId) profileInfo.getId();
    }

    private SaveOtaPackageInfoRequest buildOtaPackageInfoRequest(
            String title, String version, OtaPackageType type,
            DeviceProfileId deviceProfileId, boolean usesUrl, String url) {
        SaveOtaPackageInfoRequest request = new SaveOtaPackageInfoRequest();
        request.setTitle(title);
        request.setType(type);
        request.setUrl(url);
        request.setVersion(version);
        request.setDeviceProfileId(deviceProfileId);
        return request;
    }

    private OtaPackageInfo createFirmwareInfo(String suffix) throws Exception {
        DeviceProfileId profileId = getDefaultDeviceProfileId();
        SaveOtaPackageInfoRequest request = buildOtaPackageInfoRequest(
                OTA_PREFIX + suffix, "1.0." + System.currentTimeMillis(),
                OtaPackageType.FIRMWARE, profileId, false, null);
        return client.saveOtaPackageInfo(request);
    }

    private OtaPackageInfo createFirmwareWithUrl(String suffix) throws Exception {
        DeviceProfileId profileId = getDefaultDeviceProfileId();
        SaveOtaPackageInfoRequest request = buildOtaPackageInfoRequest(
                OTA_PREFIX + suffix, "1.0." + System.currentTimeMillis(),
                OtaPackageType.FIRMWARE, profileId, true, "https://example.com/firmware.bin");
        return client.saveOtaPackageInfo(request);
    }

    @Test
    public void testSaveAndGetOtaPackageInfo() throws Exception {
        long ts = System.currentTimeMillis();
        DeviceProfileId profileId = getDefaultDeviceProfileId();
        String title = OTA_PREFIX + "save_" + ts;
        String version = "1.0." + ts;

        SaveOtaPackageInfoRequest request = buildOtaPackageInfoRequest(
                title, version, OtaPackageType.FIRMWARE, profileId, true, "https://example.com/fw.bin");

        OtaPackageInfo saved = client.saveOtaPackageInfo(request);
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(title, saved.getTitle());
        assertEquals(version, saved.getVersion());
        assertEquals(OtaPackageType.FIRMWARE, saved.getType());
        assertTrue(saved.getUrl().contains("example.com"));

        // get info by id
        String pkgId = saved.getId().getId().toString();
        OtaPackageInfo fetched = client.getOtaPackageInfoById(pkgId);
        assertNotNull(fetched);
        assertEquals(title, fetched.getTitle());
        assertEquals(version, fetched.getVersion());
    }

    @Test
    public void testGetOtaPackageById() throws Exception {
        long ts = System.currentTimeMillis();
        OtaPackageInfo saved = createFirmwareWithUrl("getbyid_" + ts);

        OtaPackage fullPkg = client.getOtaPackageById(saved.getId().getId().toString());
        assertNotNull(fullPkg);
        assertEquals(saved.getTitle(), fullPkg.getTitle());
        assertEquals(saved.getVersion(), fullPkg.getVersion());
    }

    @Test
    public void testSaveOtaPackageInfoForSoftware() throws Exception {
        long ts = System.currentTimeMillis();
        DeviceProfileId profileId = getDefaultDeviceProfileId();
        String title = OTA_PREFIX + "sw_" + ts;

        SaveOtaPackageInfoRequest request = buildOtaPackageInfoRequest(
                title, "2.0." + ts, OtaPackageType.SOFTWARE, profileId, true, "https://example.com/sw.bin");

        OtaPackageInfo saved = client.saveOtaPackageInfo(request);
        assertNotNull(saved);
        assertEquals(OtaPackageType.SOFTWARE, saved.getType());
        assertEquals(title, saved.getTitle());
    }

    @Test
    public void testSaveOtaPackageData() throws Exception {
        long ts = System.currentTimeMillis();
        OtaPackageInfo info = createFirmwareInfo("data_" + ts);

        File tempFile = Files.createTempFile("ota_test_", ".bin").toFile();
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("test firmware content " + ts);
        }

        OtaPackageInfo updated = client.saveOtaPackageData(
                info.getId().getId().toString(), "MD5", tempFile, null);
        assertNotNull(updated);
        assertTrue(updated.getHasData());
        assertNotNull(updated.getFileName());
        assertNotNull(updated.getDataSize());
        assertTrue(updated.getDataSize() > 0);
        assertEquals(ChecksumAlgorithm.MD5, updated.getChecksumAlgorithm());
    }

    @Test
    public void testDownloadOtaPackage() throws Exception {
        long ts = System.currentTimeMillis();
        OtaPackageInfo info = createFirmwareInfo("download_" + ts);

        String content = "downloadable firmware " + ts;
        File tempFile = Files.createTempFile("ota_dl_", ".bin").toFile();
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        client.saveOtaPackageData(info.getId().getId().toString(), "MD5", tempFile, null);

        File downloaded = client.downloadOtaPackage(info.getId().getId().toString());
        assertNotNull(downloaded);
        assertTrue(downloaded.length() > 0);
        String downloadedContent = Files.readString(downloaded.toPath());
        assertEquals(content, downloadedContent);
    }

    @Test
    public void testDeleteOtaPackage() throws Exception {
        long ts = System.currentTimeMillis();
        OtaPackageInfo saved = createFirmwareWithUrl("delete_" + ts);

        String pkgId = saved.getId().getId().toString();
        client.getOtaPackageInfoById(pkgId);

        client.deleteOtaPackage(pkgId);

        assertReturns404(() -> client.getOtaPackageInfoById(pkgId));
    }

    @Test
    public void testGetOtaPackages() throws Exception {
        long ts = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            createFirmwareWithUrl("list_" + ts + "_" + i);
        }

        PageDataOtaPackageInfo page = client.getOtaPackages(100, 0, OTA_PREFIX + "list_" + ts, null, null);
        assertNotNull(page);
        assertEquals(3, page.getTotalElements().intValue());
        for (OtaPackageInfo pkg : page.getData()) {
            assertTrue(pkg.getTitle().startsWith(OTA_PREFIX + "list_" + ts));
        }
    }

    @Test
    public void testGetOtaPackagesByDeviceProfileAndType() throws Exception {
        long ts = System.currentTimeMillis();
        DeviceProfileId profileId = getDefaultDeviceProfileId();

        createFirmwareWithUrl("byprofile_" + ts + "_0");
        createFirmwareWithUrl("byprofile_" + ts + "_1");

        PageDataOtaPackageInfo page = client.getOtaPackagesByDeviceProfileAndType(
                profileId.getId().toString(), "FIRMWARE", 100, 0,
                OTA_PREFIX + "byprofile_" + ts, null, null);
        assertNotNull(page);
        assertEquals(2, page.getTotalElements().intValue());
    }

    @Test
    public void testGetOtaPackageInfoById_notFound() {
        String nonExistentId = UUID.randomUUID().toString();
        assertReturns404(() -> client.getOtaPackageInfoById(nonExistentId));
    }

    @Test
    public void testGetOtaPackagesPagination() throws Exception {
        long ts = System.currentTimeMillis();

        for (int i = 0; i < 5; i++) {
            createFirmwareWithUrl("paged_" + ts + "_" + i);
        }

        PageDataOtaPackageInfo page1 = client.getOtaPackages(2, 0, OTA_PREFIX + "paged_" + ts, null, null);
        assertNotNull(page1);
        assertEquals(5, page1.getTotalElements().intValue());
        assertEquals(3, page1.getTotalPages().intValue());
        assertEquals(2, page1.getData().size());
        assertTrue(page1.getHasNext());

        PageDataOtaPackageInfo lastPage = client.getOtaPackages(2, 2, OTA_PREFIX + "paged_" + ts, null, null);
        assertEquals(1, lastPage.getData().size());
        assertFalse(lastPage.getHasNext());
    }

    @Test
    public void testUpdateOtaPackageInfo() throws Exception {
        long ts = System.currentTimeMillis();
        OtaPackageInfo saved = createFirmwareWithUrl("update_" + ts);

        SaveOtaPackageInfoRequest updateReq = new SaveOtaPackageInfoRequest();
        updateReq.setId(saved.getId());
        updateReq.setTitle(saved.getTitle());
        updateReq.setType(saved.getType());
        updateReq.setVersion(saved.getVersion());
        updateReq.setDeviceProfileId(saved.getDeviceProfileId());
        updateReq.setUrl(saved.getUrl());
        updateReq.setAdditionalInfo(OBJECT_MAPPER.createObjectNode().put("infoKey", "infoValue"));

        OtaPackageInfo updated = client.saveOtaPackageInfo(updateReq);
        assertNotNull(updated);
        assertEquals(saved.getId().getId(), updated.getId().getId());
        assertEquals("infoValue", updated.getAdditionalInfo().get("infoKey").asText());
    }

}
