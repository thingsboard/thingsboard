// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteImageArgs;
import org.thingsboard.client.api.ThingsboardApi.DownloadImageArgs;
import org.thingsboard.client.api.ThingsboardApi.DownloadImagePreviewArgs;
import org.thingsboard.client.api.ThingsboardApi.DownloadPublicImageArgs;
import org.thingsboard.client.api.ThingsboardApi.ExportImageArgs;
import org.thingsboard.client.api.ThingsboardApi.GetImageInfoArgs;
import org.thingsboard.client.api.ThingsboardApi.GetImagesArgs;
import org.thingsboard.client.api.ThingsboardApi.UpdateImageArgs;
import org.thingsboard.client.api.ThingsboardApi.UpdateImageInfoArgs;
import org.thingsboard.client.api.ThingsboardApi.UpdateImagePublicStatusArgs;
import org.thingsboard.client.api.ThingsboardApi.UploadImageArgs;
import org.thingsboard.client.model.PageDataTbResourceInfo;
import org.thingsboard.client.model.ResourceExportData;
import org.thingsboard.client.model.TbImageDeleteResult;
import org.thingsboard.client.model.TbResourceInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class TbImageApiClientTest extends AbstractApiClientTest {

    private File createTempImage(String name, Color color) throws IOException {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 100, 100);
        g.dispose();

        File tempFile = File.createTempFile(name, ".png");
        tempFile.deleteOnExit();
        ImageIO.write(img, "png", tempFile);
        return tempFile;
    }

    @Test
    public void testImageLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<TbResourceInfo> createdImages = new ArrayList<>();
        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN};

        // upload 5 images
        for (int i = 0; i < 5; i++) {
            String title = TEST_PREFIX + "Image_" + timestamp + "_" + i;
            File imageFile = createTempImage("test_image_" + i, colors[i]);

            TbResourceInfo uploaded = client.uploadImage(UploadImageArgs.builder()
                    ._file(imageFile)
                    .title(title)
                    .build());
            assertNotNull(uploaded);
            assertNotNull(uploaded.getResourceKey());
            assertEquals(title, uploaded.getTitle());
            assertNotNull(uploaded.getLink());

            createdImages.add(uploaded);
        }

        // list images with text search
        PageDataTbResourceInfo filteredImages = client.getImages(GetImagesArgs.builder()
                .pageSize(100)
                .page(0)
                .includeSystemImages(false)
                .textSearch(TEST_PREFIX + "Image_" + timestamp)
                .build());
        assertNotNull(filteredImages);
        assertEquals(5, filteredImages.getData().size());

        // get image info by type and key
        TbResourceInfo searchImage = createdImages.get(2);
        TbResourceInfo fetchedInfo = client.getImageInfo(GetImageInfoArgs.builder()
                .type("tenant")
                .key(searchImage.getResourceKey())
                .build());
        assertEquals(searchImage.getTitle(), fetchedInfo.getTitle());
        assertEquals(searchImage.getResourceKey(), fetchedInfo.getResourceKey());

        // download image
        File downloadedImage = client.downloadImage(DownloadImageArgs.builder()
                .type("tenant")
                .key(searchImage.getResourceKey())
                .build());
        assertNotNull(downloadedImage);
        assertTrue(downloadedImage.exists());
        assertTrue(downloadedImage.length() > 0);

        // download image preview
        File preview = client.downloadImagePreview(DownloadImagePreviewArgs.builder()
                .type("tenant")
                .key(searchImage.getResourceKey())
                .build());
        assertNotNull(preview);
        assertTrue(preview.exists());
        assertTrue(preview.length() > 0);

        // update image file
        File updatedImageFile = createTempImage("updated_image", Color.MAGENTA);
        TbResourceInfo updatedImage = client.updateImage(UpdateImageArgs.builder()
                .type("tenant")
                .key(searchImage.getResourceKey())
                ._file(updatedImageFile)
                .build());
        assertNotNull(updatedImage);
        assertEquals(searchImage.getResourceKey(), updatedImage.getResourceKey());

        // update image info (title)
        TbResourceInfo infoToUpdate = client.getImageInfo(GetImageInfoArgs.builder()
                .type("tenant")
                .key(createdImages.get(3).getResourceKey())
                .build());
        infoToUpdate.setTitle(infoToUpdate.getTitle() + "_updated");
        TbResourceInfo updatedInfo = client.updateImageInfo(UpdateImageInfoArgs.builder()
                .type("tenant")
                .key(infoToUpdate.getResourceKey())
                .tbResourceInfo(infoToUpdate)
                .build());
        assertEquals(infoToUpdate.getTitle(), updatedInfo.getTitle());

        // make image public
        TbResourceInfo publicImage = client.updateImagePublicStatus(UpdateImagePublicStatusArgs.builder()
                .type("tenant")
                .key(createdImages.get(1).getResourceKey())
                .isPublic(true)
                .build());
        assertTrue(publicImage.getPublic());
        assertNotNull(publicImage.getPublicResourceKey());
        assertNotNull(publicImage.getPublicLink());

        // download public image
        File publicDownload = client.downloadPublicImage(DownloadPublicImageArgs.builder()
                .publicResourceKey(publicImage.getPublicResourceKey())
                .build());
        assertNotNull(publicDownload);
        assertTrue(publicDownload.exists());
        assertTrue(publicDownload.length() > 0);

        // make image private again
        TbResourceInfo privateImage = client.updateImagePublicStatus(UpdateImagePublicStatusArgs.builder()
                .type("tenant")
                .key(createdImages.get(1).getResourceKey())
                .isPublic(false)
                .build());
        assertEquals(false, privateImage.getPublic());

        // export image
        ResourceExportData exportData = client.exportImage(ExportImageArgs.builder()
                .type("tenant")
                .key(createdImages.get(4).getResourceKey())
                .build());
        assertNotNull(exportData);
        assertNotNull(exportData.getData());
        assertEquals(createdImages.get(4).getTitle(), exportData.getTitle());
        assertEquals(createdImages.get(4).getResourceKey(), exportData.getResourceKey());

        // delete image
        String keyToDelete = createdImages.get(0).getResourceKey();
        TbImageDeleteResult deleteResult = client.deleteImage(DeleteImageArgs.builder()
                .type("tenant")
                .key(keyToDelete)
                .force(false)
                .build());
        assertNotNull(deleteResult);
        assertTrue(deleteResult.getSuccess());

        // verify deletion
        assertReturns404(() ->
                client.getImageInfo(GetImageInfoArgs.builder()
                        .type("tenant")
                        .key(keyToDelete)
                        .build())
        );

        PageDataTbResourceInfo imagesAfterDelete = client.getImages(GetImagesArgs.builder()
                .pageSize(100)
                .page(0)
                .includeSystemImages(false)
                .textSearch(TEST_PREFIX + "Image_" + timestamp)
                .build());
        assertEquals(4, imagesAfterDelete.getData().size());
    }

}
