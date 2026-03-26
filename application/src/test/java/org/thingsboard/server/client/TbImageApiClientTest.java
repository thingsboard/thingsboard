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

            TbResourceInfo uploaded = client.uploadImage(imageFile, title, null);
            assertNotNull(uploaded);
            assertNotNull(uploaded.getResourceKey());
            assertEquals(title, uploaded.getTitle());
            assertNotNull(uploaded.getLink());

            createdImages.add(uploaded);
        }

        // list images with text search
        PageDataTbResourceInfo filteredImages = client.getImages(100, 0, null, false,
                TEST_PREFIX + "Image_" + timestamp, null, null);
        assertNotNull(filteredImages);
        assertEquals(5, filteredImages.getData().size());

        // get image info by type and key
        TbResourceInfo searchImage = createdImages.get(2);
        TbResourceInfo fetchedInfo = client.getImageInfo("tenant", searchImage.getResourceKey());
        assertEquals(searchImage.getTitle(), fetchedInfo.getTitle());
        assertEquals(searchImage.getResourceKey(), fetchedInfo.getResourceKey());

        // download image
        File downloadedImage = client.downloadImage("tenant", searchImage.getResourceKey(), null, null);
        assertNotNull(downloadedImage);
        assertTrue(downloadedImage.exists());
        assertTrue(downloadedImage.length() > 0);

        // download image preview
        File preview = client.downloadImagePreview("tenant", searchImage.getResourceKey(), null, null);
        assertNotNull(preview);
        assertTrue(preview.exists());
        assertTrue(preview.length() > 0);

        // update image file
        File updatedImageFile = createTempImage("updated_image", Color.MAGENTA);
        TbResourceInfo updatedImage = client.updateImage("tenant", searchImage.getResourceKey(), updatedImageFile);
        assertNotNull(updatedImage);
        assertEquals(searchImage.getResourceKey(), updatedImage.getResourceKey());

        // update image info (title)
        TbResourceInfo infoToUpdate = client.getImageInfo("tenant", createdImages.get(3).getResourceKey());
        infoToUpdate.setTitle(infoToUpdate.getTitle() + "_updated");
        TbResourceInfo updatedInfo = client.updateImageInfo("tenant", infoToUpdate.getResourceKey(), infoToUpdate);
        assertEquals(infoToUpdate.getTitle(), updatedInfo.getTitle());

        // make image public
        TbResourceInfo publicImage = client.updateImagePublicStatus("tenant",
                createdImages.get(1).getResourceKey(), true);
        assertTrue(publicImage.getPublic());
        assertNotNull(publicImage.getPublicResourceKey());
        assertNotNull(publicImage.getPublicLink());

        // download public image
        File publicDownload = client.downloadPublicImage(publicImage.getPublicResourceKey(), null, null);
        assertNotNull(publicDownload);
        assertTrue(publicDownload.exists());
        assertTrue(publicDownload.length() > 0);

        // make image private again
        TbResourceInfo privateImage = client.updateImagePublicStatus("tenant",
                createdImages.get(1).getResourceKey(), false);
        assertEquals(false, privateImage.getPublic());

        // export image
        ResourceExportData exportData = client.exportImage("tenant", createdImages.get(4).getResourceKey());
        assertNotNull(exportData);
        assertNotNull(exportData.getData());
        assertEquals(createdImages.get(4).getTitle(), exportData.getTitle());
        assertEquals(createdImages.get(4).getResourceKey(), exportData.getResourceKey());

        // delete image
        String keyToDelete = createdImages.get(0).getResourceKey();
        TbImageDeleteResult deleteResult = client.deleteImage("tenant", keyToDelete, false);
        assertNotNull(deleteResult);
        assertTrue(deleteResult.getSuccess());

        // verify deletion
        assertReturns404(() ->
                client.getImageInfo("tenant", keyToDelete)
        );

        PageDataTbResourceInfo imagesAfterDelete = client.getImages(100, 0, null, false,
                TEST_PREFIX + "Image_" + timestamp, null, null);
        assertEquals(4, imagesAfterDelete.getData().size());
    }

}
