// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteOtaPackageArgs;
import org.thingsboard.client.api.ThingsboardApi.DownloadOtaPackageArgs;
import org.thingsboard.client.api.ThingsboardApi.GetOtaPackageByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetOtaPackageInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetOtaPackagesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetOtaPackagesByDeviceProfileAndTypeArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveOtaPackageDataArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveOtaPackageInfoArgs;
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
        return client.saveOtaPackageInfo(SaveOtaPackageInfoArgs.builder()
                .saveOtaPackageInfoRequest(request)
                .build());
    }

    private OtaPackageInfo createFirmwareWithUrl(String suffix) throws Exception {
        DeviceProfileId profileId = getDefaultDeviceProfileId();
        SaveOtaPackageInfoRequest request = buildOtaPackageInfoRequest(
                OTA_PREFIX + suffix, "1.0." + System.currentTimeMillis(),
                OtaPackageType.FIRMWARE, profileId, true, "https://example.com/firmware.bin");
        return client.saveOtaPackageInfo(SaveOtaPackageInfoArgs.builder()
                .saveOtaPackageInfoRequest(request)
                .build());
    }

    @Test
    public void testSaveAndGetOtaPackageInfo() throws Exception {
        long ts = System.currentTimeMillis();
        DeviceProfileId profileId = getDefaultDeviceProfileId();
        String title = OTA_PREFIX + "save_" + ts;
        String version = "1.0." + ts;

        SaveOtaPackageInfoRequest request = buildOtaPackageInfoRequest(
                title, version, OtaPackageType.FIRMWARE, profileId, true, "https://example.com/fw.bin");

        OtaPackageInfo saved = client.saveOtaPackageInfo(SaveOtaPackageInfoArgs.builder()
                .saveOtaPackageInfoRequest(request)
                .build());
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(title, saved.getTitle());
        assertEquals(version, saved.getVersion());
        assertEquals(OtaPackageType.FIRMWARE, saved.getType());
        assertTrue(saved.getUrl().contains("example.com"));

        // get info by id
        String pkgId = saved.getId().getId().toString();
        OtaPackageInfo fetched = client.getOtaPackageInfoById(GetOtaPackageInfoByIdArgs.builder()
                .otaPackageId(pkgId)
                .build());
        assertNotNull(fetched);
        assertEquals(title, fetched.getTitle());
        assertEquals(version, fetched.getVersion());
    }

    @Test
    public void testGetOtaPackageById() throws Exception {
        long ts = System.currentTimeMillis();
        OtaPackageInfo saved = createFirmwareWithUrl("getbyid_" + ts);

        OtaPackage fullPkg = client.getOtaPackageById(GetOtaPackageByIdArgs.builder()
                .otaPackageId(saved.getId().getId().toString())
                .build());
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

        OtaPackageInfo saved = client.saveOtaPackageInfo(SaveOtaPackageInfoArgs.builder()
                .saveOtaPackageInfoRequest(request)
                .build());
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

        OtaPackageInfo updated = client.saveOtaPackageData(SaveOtaPackageDataArgs.builder()
                .otaPackageId(info.getId().getId().toString())
                .checksumAlgorithm("MD5")
                ._file(tempFile)
                .build());
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

        client.saveOtaPackageData(SaveOtaPackageDataArgs.builder()
                .otaPackageId(info.getId().getId().toString())
                .checksumAlgorithm("MD5")
                ._file(tempFile)
                .build());

        File downloaded = client.downloadOtaPackage(DownloadOtaPackageArgs.builder()
                .otaPackageId(info.getId().getId().toString())
                .build());
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
        client.getOtaPackageInfoById(GetOtaPackageInfoByIdArgs.builder()
                .otaPackageId(pkgId)
                .build());

        client.deleteOtaPackage(DeleteOtaPackageArgs.builder()
                .otaPackageId(pkgId)
                .build());

        assertReturns404(() -> client.getOtaPackageInfoById(GetOtaPackageInfoByIdArgs.builder()
                .otaPackageId(pkgId)
                .build()));
    }

    @Test
    public void testGetOtaPackages() throws Exception {
        long ts = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            createFirmwareWithUrl("list_" + ts + "_" + i);
        }

        PageDataOtaPackageInfo page = client.getOtaPackages(GetOtaPackagesArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(OTA_PREFIX + "list_" + ts)
                .build());
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

        PageDataOtaPackageInfo page = client.getOtaPackagesByDeviceProfileAndType(GetOtaPackagesByDeviceProfileAndTypeArgs.builder()
                .deviceProfileId(profileId.getId().toString())
                .type("FIRMWARE")
                .pageSize(100)
                .page(0)
                .textSearch(OTA_PREFIX + "byprofile_" + ts)
                .build());
        assertNotNull(page);
        assertEquals(2, page.getTotalElements().intValue());
    }

    @Test
    public void testGetOtaPackageInfoById_notFound() {
        String nonExistentId = UUID.randomUUID().toString();
        assertReturns404(() -> client.getOtaPackageInfoById(GetOtaPackageInfoByIdArgs.builder()
                .otaPackageId(nonExistentId)
                .build()));
    }

    @Test
    public void testGetOtaPackagesPagination() throws Exception {
        long ts = System.currentTimeMillis();

        for (int i = 0; i < 5; i++) {
            createFirmwareWithUrl("paged_" + ts + "_" + i);
        }

        PageDataOtaPackageInfo page1 = client.getOtaPackages(GetOtaPackagesArgs.builder()
                .pageSize(2)
                .page(0)
                .textSearch(OTA_PREFIX + "paged_" + ts)
                .build());
        assertNotNull(page1);
        assertEquals(5, page1.getTotalElements().intValue());
        assertEquals(3, page1.getTotalPages().intValue());
        assertEquals(2, page1.getData().size());
        assertTrue(page1.getHasNext());

        PageDataOtaPackageInfo lastPage = client.getOtaPackages(GetOtaPackagesArgs.builder()
                .pageSize(2)
                .page(2)
                .textSearch(OTA_PREFIX + "paged_" + ts)
                .build());
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

        OtaPackageInfo updated = client.saveOtaPackageInfo(SaveOtaPackageInfoArgs.builder()
                .saveOtaPackageInfoRequest(updateReq)
                .build());
        assertNotNull(updated);
        assertEquals(saved.getId().getId(), updated.getId().getId());
        assertEquals("infoValue", updated.getAdditionalInfo().get("infoKey").asText());
    }

}
