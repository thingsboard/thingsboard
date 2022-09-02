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
package org.thingsboard.server.cache.ota.service;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.cache.ota.files.BaseFileCacheService;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties
@ComponentScan({"org.thingsboard.server.cache.ota.files"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BaseFileCacheServiceTest {
    private final static Path PATH = Paths.get(System.getProperty("java.io.tmpdir"), "ota");
    private final static String FILE_FILLING = "Hello, testing environment";
    private static final int ONE_MEGA_BYTE = 1_000_000;
    private final static OtaPackageId OTA_PACKAGE_ID = new OtaPackageId(UUID.randomUUID());
    private final static InputStream DATA = new ByteArrayInputStream(FILE_FILLING.getBytes());

    @Autowired
    private BaseFileCacheService baseFileCacheService;

    @Test
    void testDataSavingWithNullInputStream() {
        assertThrows(RuntimeException.class, () -> baseFileCacheService.findOrLoad(OTA_PACKAGE_ID, null));
    }

    @Test
    void testDataSavingWithNullOtaPackageId() {
        assertThrows(RuntimeException.class, () -> baseFileCacheService.findOrLoad(null, () -> DATA));
    }

    @Test
    @SneakyThrows
    void testMultiSavingDataToFile() {
        File directory = PATH.toFile();
        int beginning = Objects.requireNonNull(directory.list()).length;
        Thread thread1 = new Thread(() -> baseFileCacheService.findOrLoad(OTA_PACKAGE_ID, () -> DATA));
        Thread thread2 = new Thread(() -> baseFileCacheService.findOrLoad(OTA_PACKAGE_ID, () -> DATA));
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        File directory1 = PATH.toFile();
        int ending = Objects.requireNonNull(directory1.list()).length;
        assertEquals(1, ending - beginning);
    }

    @Test
    @SneakyThrows
    void testCorrectDataSavingToFile() {
        String sha256 = calculateChecksumSHA256(new ByteArrayInputStream(FILE_FILLING.getBytes()));
        File file = baseFileCacheService.findOrLoad(OTA_PACKAGE_ID, () -> DATA);
        assertEquals(sha256, calculateChecksumSHA256(new FileInputStream(file)));
    }

    @SneakyThrows
    String calculateChecksumSHA256(InputStream stream) {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[ONE_MEGA_BYTE];
        int count = 0;
        while ((count = stream.read(buffer)) != -1) {
            md.update(buffer, 0, count);
        }
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}