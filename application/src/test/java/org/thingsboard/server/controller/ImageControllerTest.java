/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.hash.Hashing;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.SystemParams;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sql.resource.TbResourceRepository;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "server.http.max_payload_size=/api/image*/**=3000;/api/**=60000"
})
public class ImageControllerTest extends AbstractControllerTest {

    protected static final byte[] PNG_IMAGE = Base64.getDecoder().decode("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");
    private static final byte[] JPEG_IMAGE = Base64.getDecoder().decode("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");
    private static final byte[] SVG_IMAGE = Base64.getDecoder().decode("PHN2ZyB2ZXJzaW9uPSIxLjEiIGlkPSJzdmc0NDA4IiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHg9IjAiIHk9IjAiIHZpZXdCb3g9IjAgMCAxNTAgMTUwIiB4bWw6c3BhY2U9InByZXNlcnZlIj48c3R5bGU+LnN0MntmaWxsOiMyNzg2MjJ9PC9zdHlsZT48ZyBpZD0ibGF5ZXIxIj48ZyBpZD0icGF0aDY4ODEtMy01LTUtMS04LTQtNC03LTgiIHRyYW5zZm9ybT0idHJhbnNsYXRlKC0xNDYuNDM4IC0yNzYuMDI4KSIgb3BhY2l0eT0iLjg5MiI+PHJhZGlhbEdyYWRpZW50IGlkPSJTVkdJRF8xXyIgY3g9IjMwODUuMjE1IiBjeT0iMzE3OC40NTgiIHI9IjQ5LjkwMSIgZ3JhZGllbnRUcmFuc2Zvcm09Im1hdHJpeCguNjc5MyAuMDA3NiAtLjUwOSAuNTYxMiAtMjMyLjYyOSAtMTQxMS43MjUpIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHN0b3Agb2Zmc2V0PSIwIi8+PHN0b3Agb2Zmc2V0PSIxIiBzdG9wLW9wYWNpdHk9Ii4xODgiLz48L3JhZGlhbEdyYWRpZW50PjxwYXRoIGQ9Ik0yODUuNiAzODguNWMxMC4zLTEyLjQgNC40LTIyLjQtMTQuNC0yMi40LTE4LjkgMC00Mi40IDEwLTUzLjkgMjIuNC0xNi44IDE4IC40IDIzLjUtLjIgMzUtLjEgMS44IDMuOSAxLjggNyAwIDE5LjgtMTEuNSA0Ni41LTE3IDYxLjUtMzUiIGZpbGw9InVybCgjU1ZHSURfMV8pIi8+PC9nPjxwYXRoIGlkPSJwYXRoNjg4MS0zLTUtNS0xLTgtNC00IiBjbGFzcz0ic3QyIiBkPSJNMTI0LjcgNjkuMWMtLjktMjcuNS0yMi4zLTQ5LjgtNDkuOC00OS44cy00OSAyMi4zLTQ5LjggNDkuOGMtMS4zIDQwLjEgMzAuNyA1Mi4yIDQ0LjcgNzggMi4yIDQgOCA0IDEwLjEgMCAxNC4xLTI1LjggNDYuMS0zNy45IDQ0LjgtNzgiLz48L2c+PGcgaWQ9Imc0OTI4Ij48Y2lyY2xlIGlkPSJwYXRoNDk3OCIgY2xhc3M9InN0MiIgY3g9Ijc0LjkiIGN5PSI2OS4xIiByPSI0OS45Ii8+PGcgaWQ9Imc0OTE1Ij48cGF0aCBpZD0icGF0aDY4ODMtMi0zLTUtMi00LTktNC05IiBkPSJNNzQuOCAxMDYuNGMtMjAuNiAwLTM3LjQtMTYuNy0zNy40LTM3LjQgMC0yMC42IDE2LjctMzcuNCAzNy40LTM3LjQgMjAuNiAwIDM3LjQgMTYuNyAzNy40IDM3LjRzLTE2LjcgMzcuNC0zNy40IDM3LjQiIGZpbGw9IiNmZmYiLz48L2c+PC9nPjxwYXRoIGNsYXNzPSJzdDIiIGQ9Ik05NS45IDQ2LjZWNDloLTEwdi0yLjVsMTAgLjF6bS0yIDUuM2gtOHYyLjVoOHYtMi41em0tOCA3LjloNnYtMi41aC02djIuNXptNCAyLjloLTR2Mi41aDR2LTIuNXptLTQgNy44aDJWNjhoLTJ2Mi41em0xLjUgMTRjMCA2LjktNS41IDEyLjUtMTIuMyAxMi41cy0xMi4zLTUuNi0xMi4zLTEyLjVjMC00LjUgMi4zLTguNSA2LjEtMTAuN1Y0NS41YzAtMy41IDIuOC02LjMgNi4yLTYuM3M2LjIgMi44IDYuMiA2LjN2MjguM2MzLjggMi4yIDYuMSA2LjMgNi4xIDEwLjd6bS0yLjQgMGMwLTMuOC0yLjEtNy4yLTUuNC04LjlsLS43LS4zVjQ1LjVjMC0yLjEtMS43LTMuOC0zLjgtMy44LTIuMSAwLTMuOCAxLjctMy44IDMuOHYyOS44bC0uNy4zYy0zLjMgMS43LTUuNCA1LjEtNS40IDguOSAwIDUuNSA0LjQgMTAgOS45IDEwUzg1IDkwIDg1IDg0LjV6bS0yLjEgMGMwIDQuNC0zLjUgOC03LjggOHMtNy44LTMuNi03LjgtOGMwLTMuNiAyLjQtNi44IDUuOC03LjdsLjUtLjFWNjEuNWgzLjF2MTUuMmwuNS4xYzMuMyAxIDUuNyA0LjEgNS43IDcuN3ptLTcuNC01LjNjLS4yLS44LTEtMS40LTEuOS0xLjItMyAuNy01IDMuMy01IDYuNCAwIC45LjcgMS42IDEuNiAxLjZzMS42LS43IDEuNi0xLjZjMC0xLjYgMS4xLTMgMi42LTMuMy43LS4yIDEuMy0xIDEuMS0xLjl6Ii8+PC9zdmc+Cg==");
    private static final byte[] PNG_IMAGE_5KB = Base64.getDecoder().decode("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");

    @Autowired
    private TbResourceRepository resourceRepository;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void afterEach() {
        resourceRepository.deleteAll();
    }

    @Test
    public void testUploadPngImage() throws Exception {
        String filename = "my_png_image.png";
        TbResourceInfo imageInfo = uploadImage(HttpMethod.POST, "/api/image", filename, "image/png", PNG_IMAGE);

        assertThat(imageInfo.getResourceSubType()).isEqualTo(ResourceSubType.IMAGE);

        assertThat(imageInfo.getTitle()).isEqualTo(filename);
        assertThat(imageInfo.getResourceType()).isEqualTo(ResourceType.IMAGE);
        assertThat(imageInfo.getResourceKey()).isEqualTo(filename);
        assertThat(imageInfo.getFileName()).isEqualTo(filename);
        assertThat(imageInfo.getEtag()).isEqualTo(Hashing.sha256().hashBytes(PNG_IMAGE).toString());

        checkPngImageDescriptor(imageInfo.getDescriptor(ImageDescriptor.class));

        assertThat(downloadImage("tenant", filename)).containsExactly(PNG_IMAGE);
        assertThat(downloadImagePreview("tenant", filename)).containsExactly(PNG_IMAGE);

        assertThat(getImages(null, false, 10)).contains(imageInfo);
    }

    @Test
    public void testUploadJpegImage() throws Exception {
        String filename = "my_jpeg_image.jpg";
        TbResourceInfo imageInfo = uploadImage(HttpMethod.POST, "/api/image", filename, "image/jpeg", JPEG_IMAGE);

        assertThat(imageInfo.getResourceSubType()).isEqualTo(ResourceSubType.IMAGE);

        ImageDescriptor imageDescriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        checkJpegImageDescriptor(imageDescriptor);

        assertThat(downloadImage("tenant", filename)).containsExactly(JPEG_IMAGE);
        assertThat(downloadImagePreview("tenant", filename)).hasSize((int) imageDescriptor.getPreviewDescriptor().getSize());
    }

    @Test
    public void testUploadSvgImage() throws Exception {
        String filename = "my_svg_image.svg";
        TbResourceInfo imageInfo = uploadImage(HttpMethod.POST, "/api/image", filename, "image/svg+xml", SVG_IMAGE);

        assertThat(imageInfo.getResourceSubType()).isEqualTo(ResourceSubType.IMAGE);

        ImageDescriptor imageDescriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        checkSvgImageDescriptor(imageDescriptor);

        assertThat(downloadImage("tenant", filename)).containsExactly(SVG_IMAGE);
        assertThat(downloadImagePreview("tenant", filename)).hasSize((int) imageDescriptor.getPreviewDescriptor().getSize());
    }

    @Test
    public void testUploadScadaSymbolImage() throws Exception {
        String filename = "my_scada_symbol_image.svg";
        TbResourceInfo imageInfo = uploadImage(HttpMethod.POST, "/api/image", ResourceSubType.SCADA_SYMBOL.name(), filename, "image/svg+xml", SVG_IMAGE);

        assertThat(imageInfo.getResourceSubType()).isEqualTo(ResourceSubType.SCADA_SYMBOL);

        ImageDescriptor imageDescriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        checkSvgImageDescriptor(imageDescriptor);

        assertThat(downloadImage("tenant", filename)).containsExactly(SVG_IMAGE);
        assertThat(downloadImagePreview("tenant", filename)).hasSize((int) imageDescriptor.getPreviewDescriptor().getSize());
    }

    @Test
    public void testUploadImageWithSameFilename() throws Exception {
        String filename = "my_jpeg_image.jpg";
        TbResourceInfo imageInfo1 = uploadImage(HttpMethod.POST, "/api/image", filename, "image/jpeg", JPEG_IMAGE);
        assertThat(imageInfo1.getTitle()).isEqualTo(filename);
        assertThat(imageInfo1.getFileName()).isEqualTo(filename);
        assertThat(imageInfo1.getResourceKey()).isEqualTo(filename);

        TbResourceInfo imageInfo2 = uploadImage(HttpMethod.POST, "/api/image", filename, "image/jpeg", JPEG_IMAGE);
        assertThat(imageInfo2.getTitle()).isEqualTo(filename);
        assertThat(imageInfo2.getFileName()).isEqualTo(filename);
        assertThat(imageInfo2.getResourceKey()).isEqualTo("my_jpeg_image_(1).jpg");

        TbResourceInfo imageInfo3 = uploadImage(HttpMethod.POST, "/api/image", filename, "image/jpeg", JPEG_IMAGE);
        assertThat(imageInfo3.getTitle()).isEqualTo(filename);
        assertThat(imageInfo3.getFileName()).isEqualTo(filename);
        assertThat(imageInfo3.getResourceKey()).isEqualTo("my_jpeg_image_(2).jpg");
    }

    @Test
    public void testUpdateImage() throws Exception {
        String filename = "my_png_image.png";
        TbResourceInfo imageInfo = uploadImage(HttpMethod.POST, "/api/image", filename, "image/png", PNG_IMAGE);
        checkPngImageDescriptor(imageInfo.getDescriptor(ImageDescriptor.class));

        String newFilename = "my_jpeg_image.png";
        TbResourceInfo newImageInfo = uploadImage(HttpMethod.PUT, "/api/images/tenant/" + filename, newFilename, "image/jpeg", JPEG_IMAGE);

        assertThat(newImageInfo.getTitle()).isEqualTo(filename);
        assertThat(newImageInfo.getResourceKey()).isEqualTo(filename);
        assertThat(newImageInfo.getFileName()).isEqualTo(newFilename);
        assertThat(newImageInfo.getPublicResourceKey()).isEqualTo(imageInfo.getPublicResourceKey());

        ImageDescriptor imageDescriptor = newImageInfo.getDescriptor(ImageDescriptor.class);
        checkJpegImageDescriptor(imageDescriptor);

        assertThat(downloadImage("tenant", filename)).containsExactly(JPEG_IMAGE);
        assertThat(downloadImagePreview("tenant", filename)).hasSize((int) imageDescriptor.getPreviewDescriptor().getSize());
    }

    @Test
    public void testUpdateImageInfo() throws Exception {
        String filename = "my_png_image.png";
        TbResourceInfo imageInfo = uploadImage(HttpMethod.POST, "/api/image", filename, "image/png", PNG_IMAGE);
        ImageDescriptor imageDescriptor = imageInfo.getDescriptor(ImageDescriptor.class);

        assertThat(imageInfo.getTitle()).isEqualTo(filename);
        assertThat(imageInfo.getResourceKey()).isEqualTo(filename);
        assertThat(imageInfo.getFileName()).isEqualTo(filename);

        String newTitle = "My PNG image";
        TbResourceInfo newImageInfo = new TbResourceInfo(imageInfo);
        newImageInfo.setTitle(newTitle);
        newImageInfo.setDescriptor(JacksonUtil.newObjectNode());
        newImageInfo = doPut("/api/images/tenant/" + filename + "/info", newImageInfo, TbResourceInfo.class);

        assertThat(newImageInfo.getTitle()).isEqualTo(newTitle);
        assertThat(newImageInfo.getDescriptor(ImageDescriptor.class)).isEqualTo(imageDescriptor);
        assertThat(newImageInfo.getResourceKey()).isEqualTo(imageInfo.getResourceKey());
        assertThat(newImageInfo.getPublicResourceKey()).isEqualTo(imageInfo.getPublicResourceKey());
    }

    @Test
    public void testExportImportImage() throws Exception {
        String filename = "my_png_image.png";
        uploadImage(HttpMethod.POST, "/api/image", filename, "image/png", PNG_IMAGE);

        ResourceExportData exportData = doGet("/api/images/tenant/" + filename + "/export", ResourceExportData.class);
        assertThat(exportData.getMediaType()).isEqualTo("image/png");
        assertThat(exportData.getFileName()).isEqualTo(filename);
        assertThat(exportData.getTitle()).isEqualTo(filename);
        assertThat(exportData.getSubType()).isEqualTo(ResourceSubType.IMAGE);
        assertThat(exportData.getResourceKey()).isEqualTo(filename);
        assertThat(exportData.getData()).isEqualTo(Base64.getEncoder().encodeToString(PNG_IMAGE));
        assertThat(exportData.isPublic()).isTrue();
        assertThat(exportData.getPublicResourceKey()).isNotEmpty();

        doDelete("/api/images/tenant/" + filename).andExpect(status().isOk());

        TbResourceInfo importedImageInfo = doPut("/api/image/import", exportData, TbResourceInfo.class);
        assertThat(importedImageInfo.getTitle()).isEqualTo(filename);
        assertThat(exportData.getSubType()).isEqualTo(ResourceSubType.IMAGE);
        assertThat(importedImageInfo.getResourceKey()).isEqualTo(filename);
        assertThat(importedImageInfo.getFileName()).isEqualTo(filename);
        assertThat(importedImageInfo.isPublic()).isTrue();
        assertThat(importedImageInfo.getPublicResourceKey()).isEqualTo(exportData.getPublicResourceKey());
        checkPngImageDescriptor(importedImageInfo.getDescriptor(ImageDescriptor.class));
        assertThat(downloadImage("tenant", filename)).containsExactly(PNG_IMAGE);
    }

    @Test
    public void testExportImportLargeImage() throws Exception {
        String filename = "my_png_image.png";
        uploadImage(HttpMethod.POST, "/api/image", filename, "image/png", PNG_IMAGE_5KB);
        ResourceExportData exportData = doGet("/api/images/tenant/" + filename + "/export", ResourceExportData.class);
        doPut("/api/image/import", exportData).andExpect(status().isPayloadTooLarge());
    }

    @Test
    public void testGetImages() throws Exception {
        loginSysAdmin();
        String systemImageName = "my_system_png_image.png";
        TbResourceInfo systemImage = uploadImage(HttpMethod.POST, "/api/image", systemImageName, "image/png", PNG_IMAGE);

        String systemScadaSymbolName = "my_system_scada_symbol_image.svg";
        TbResourceInfo systemScadaSymbol = uploadImage(HttpMethod.POST, "/api/image", ResourceSubType.SCADA_SYMBOL.name(), systemScadaSymbolName, "image/svg+xml", SVG_IMAGE);

        loginTenantAdmin();
        String tenantImageName = "my_jpeg_image.jpg";
        TbResourceInfo tenantImage = uploadImage(HttpMethod.POST, "/api/image", tenantImageName, "image/jpeg", JPEG_IMAGE);

        String tenantScadaSymbolName = "my_scada_symbol_image.svg";
        TbResourceInfo tenantScadaSymbol = uploadImage(HttpMethod.POST, "/api/image", ResourceSubType.SCADA_SYMBOL.name(), tenantScadaSymbolName, "image/svg+xml", SVG_IMAGE);

        List<TbResourceInfo> tenantImages = getImages(null, false, 10);
        assertThat(tenantImages).containsOnly(tenantImage);

        List<TbResourceInfo> tenantScadaSymbols = getImages(null, ResourceSubType.SCADA_SYMBOL.name(), false, 10);
        assertThat(tenantScadaSymbols).containsOnly(tenantScadaSymbol);

        List<TbResourceInfo> allImages = getImages(null, true, 10);
        assertThat(allImages).containsOnly(tenantImage, systemImage);

        List<TbResourceInfo> allScadaSymbols = getImages(null, ResourceSubType.SCADA_SYMBOL.name(), true, 10);
        assertThat(allScadaSymbols).containsOnly(tenantScadaSymbol, systemScadaSymbol);

        assertThat(getImages("png", true, 10))
                .containsOnly(systemImage);
        assertThat(getImages("jpg", true, 10))
                .containsOnly(tenantImage);

        assertThat(getImages("my_system_scada_symbol", ResourceSubType.SCADA_SYMBOL.name(), true, 10))
                .containsOnly(systemScadaSymbol);
        assertThat(getImages("my_scada_symbol", ResourceSubType.SCADA_SYMBOL.name(), true, 10))
                .containsOnly(tenantScadaSymbol);
    }

    @Test
    public void testUploadPublicImage() throws Exception {
        String filename = "my_public_image.png";
        TbResourceInfo imageInfo = uploadImage(HttpMethod.POST, "/api/image", filename, "image/png", PNG_IMAGE);

        assertThat(imageInfo.isPublic()).isTrue();
        assertThat(imageInfo.getPublicResourceKey()).hasSize(32);
        assertThat(imageInfo.getPublicLink()).isEqualTo("/api/images/public/" + imageInfo.getPublicResourceKey());

        assertThat(downloadImage("tenant", filename)).containsExactly(PNG_IMAGE);
        resetTokens();
        assertThat(downloadPublicImage(imageInfo.getPublicResourceKey())).containsExactly(PNG_IMAGE);
    }

    @Test
    public void testMakeImagePublic() throws Exception {
        String filename = "my_public_image.png";
        TbResourceInfo imageInfo = uploadImage(HttpMethod.POST, "/api/image", filename, "image/png", PNG_IMAGE);
        String publicKey = imageInfo.getPublicResourceKey();
        assertThat(publicKey).hasSize(32);

        updateImagePublicStatus(filename, false);
        doGet("/api/images/public/" + publicKey).andExpect(status().isNotFound());

        updateImagePublicStatus(filename, true);
        resetTokens();
        assertThat(downloadPublicImage(publicKey)).containsExactly(PNG_IMAGE);

        loginTenantAdmin();
        updateImagePublicStatus(filename, false);
        doGet("/api/images/public/" + publicKey).andExpect(status().isNotFound());
    }

    @Test
    public void testGetImageUploadSpecs() throws Exception {
        SystemParams systemParams = doGet("/api/system/params", SystemParams.class);
        assertThat(systemParams.getMaxResourceSize()).isZero();

        loginSysAdmin();
        updateDefaultTenantProfileConfig(tenantProfileConfig -> {
            tenantProfileConfig.setMaxResourceSize(100);
        });
        loginTenantAdmin();
        systemParams = doGet("/api/system/params", SystemParams.class);
        assertThat(systemParams.getMaxResourceSize()).isEqualTo(100);

        loginSysAdmin();
        updateDefaultTenantProfileConfig(tenantProfileConfig -> {
            tenantProfileConfig.setMaxResourceSize(0);
        });
        loginTenantAdmin();
        systemParams = doGet("/api/system/params", SystemParams.class);
        assertThat(systemParams.getMaxResourceSize()).isEqualTo(0);
    }

    @Test
    public void testInlineImages() throws Exception {
        TbResourceInfo imageInfo = uploadImage(HttpMethod.POST, "/api/image", "my_png_image.png", "image/png", PNG_IMAGE);
        DeviceProfile deviceProfile = createDeviceProfile("Test");
        deviceProfile.setImage("tb-image;" + imageInfo.getLink());
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        DeviceProfile deviceProfileInlined = doGet("/api/deviceProfile/" + deviceProfile.getUuidId() + "?inlineImages=true", DeviceProfile.class);
        assertThat(deviceProfileInlined.getImage()).isEqualTo("tb-image:" +
                Base64.getEncoder().encodeToString(imageInfo.getResourceKey().getBytes()) + ":"
                + Base64.getEncoder().encodeToString(imageInfo.getName().getBytes()) + ":"
                + Base64.getEncoder().encodeToString(imageInfo.getResourceSubType().name().getBytes()) +
                ";data:image/png;base64," + Base64.getEncoder().encodeToString(PNG_IMAGE));

        deviceProfile = doGet("/api/deviceProfile/" + deviceProfile.getUuidId(), DeviceProfile.class);
        assertThat(deviceProfile.getImage()).isEqualTo("tb-image;" + imageInfo.getLink());
    }

    private TbResourceInfo updateImagePublicStatus(String filename, boolean isPublic) throws Exception {
        return doPut("/api/images/tenant/" + filename + "/public/" + isPublic, "", TbResourceInfo.class);
    }

    private void checkPngImageDescriptor(ImageDescriptor imageDescriptor) {
        assertThat(imageDescriptor.getMediaType()).isEqualTo("image/png");
        assertThat(imageDescriptor.getWidth()).isEqualTo(200);
        assertThat(imageDescriptor.getHeight()).isEqualTo(160);
        assertThat(imageDescriptor.getSize()).isEqualTo(PNG_IMAGE.length);
        assertThat(imageDescriptor.getEtag()).isEqualTo(Hashing.sha256().hashBytes(PNG_IMAGE).toString());

        ImageDescriptor previewDescriptor = imageDescriptor.getPreviewDescriptor();
        assertThat(previewDescriptor.getMediaType()).isEqualTo("image/png");
        assertThat(previewDescriptor.getWidth()).isEqualTo(200);
        assertThat(previewDescriptor.getHeight()).isEqualTo(160);
        assertThat(previewDescriptor.getSize()).isEqualTo(PNG_IMAGE.length);
        assertThat(previewDescriptor.getEtag()).isEqualTo(imageDescriptor.getEtag());
    }

    private void checkJpegImageDescriptor(ImageDescriptor imageDescriptor) {
        assertThat(imageDescriptor.getMediaType()).isEqualTo("image/jpeg");
        assertThat(imageDescriptor.getWidth()).isEqualTo(225);
        assertThat(imageDescriptor.getHeight()).isEqualTo(225);
        assertThat(imageDescriptor.getSize()).isEqualTo(JPEG_IMAGE.length);
        assertThat(imageDescriptor.getEtag()).isEqualTo(Hashing.sha256().hashBytes(JPEG_IMAGE).toString());

        ImageDescriptor previewDescriptor = imageDescriptor.getPreviewDescriptor();
        assertThat(previewDescriptor.getMediaType()).isEqualTo("image/png");
        assertThat(previewDescriptor.getWidth()).isEqualTo(225);
        assertThat(previewDescriptor.getHeight()).isEqualTo(225);
        assertThat(previewDescriptor.getSize()).isEqualTo(52365);
        assertThat(previewDescriptor.getEtag()).isEqualTo("f231854ceb5cfce4371c246f7738d35d61770bac106ad3b9ee9b74d19817d09e");
    }

    private void checkSvgImageDescriptor(ImageDescriptor imageDescriptor) {
        assertThat(imageDescriptor.getMediaType()).isEqualTo("image/svg+xml");
        assertThat(imageDescriptor.getWidth()).isEqualTo(150);
        assertThat(imageDescriptor.getHeight()).isEqualTo(150);
        assertThat(imageDescriptor.getSize()).isEqualTo(SVG_IMAGE.length);
        assertThat(imageDescriptor.getEtag()).isEqualTo(Hashing.sha256().hashBytes(SVG_IMAGE).toString());
    }

    private List<TbResourceInfo> getImages(String searchText, boolean includeSystemImages, int limit) throws Exception {
        return this.getImages(searchText, null, includeSystemImages, limit);
    }

    private List<TbResourceInfo> getImages(String searchText, String imageSubType, boolean includeSystemImages, int limit) throws Exception {
        var url = "/api/images?includeSystemImages=" + includeSystemImages + "&";
        if (StringUtils.isNotEmpty(imageSubType)) {
            url += "imageSubType=" + imageSubType + "&";
        }
        PageData<TbResourceInfo> images = doGetTypedWithPageLink(url, new TypeReference<>() {}, new PageLink(limit, 0, searchText));
        return images.getData();
    }

    private byte[] downloadImage(String type, String key) throws Exception {
        return doGet("/api/images/" + type + "/" + key).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private byte[] downloadImagePreview(String type, String key) throws Exception {
        return doGet("/api/images/" + type + "/" + key + "/preview").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private byte[] downloadPublicImage(String publicKey) throws Exception {
        return doGet("/api/images/public/" + publicKey).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

}
