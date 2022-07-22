/**
 * Copyright © 2016-2022 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.cache.ota.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.*;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
@RequiredArgsConstructor
@Component
public class BaseFileCacheService implements FileCacheService {
    @Value("${java.io.tmpdir}/ota/")
    private String PATH;

    private final TemporaryFileCleaner fileCleaner;
    private final ConcurrentMap<OtaPackageId, CountDownLatch> files = new ConcurrentHashMap<>();


    public File saveDataTemporaryFile(InputStream inputStream) {
        File path = new File(PATH);
        try {
            File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp", path);
            FileUtils.copyInputStreamToFile(inputStream, tempFile);
            return tempFile;
        } catch (IOException e) {
            log.error("Failed to create temp file", e);
            throw new RuntimeException("Failed to create temp file for input stream");
        }
    }

    public File getOtaDataFile(OtaPackageId otaPackageId) throws FileNotFoundException {
        String fileName = PATH + otaPackageId.getId();
        if (exist(fileName)) {
            fileCleaner.updateFileUsageStatus(otaPackageId);
            return new File(fileName);
        }
        throw new FileNotFoundException();
    }

    public File loadToFile(OtaPackageId otaPackageId, Blob data) {
        if (otaPackageId == null || data == null) {
            log.error("Received null variables: {}", otaPackageId == null ? "otaPackageId" : "data");
            throw new NullPointerException();
        }
        CountDownLatch latch = files.computeIfAbsent(otaPackageId, ota -> processFileSaving(ota, data));
        String fileName = PATH + otaPackageId.getId();
        if (latch.getCount() == 0) {
            return new File(fileName);
        } else {
            try {
                latch.await();
                return new File(fileName);
            } catch (InterruptedException e) {
                log.error("Failed to save file {} to file system", fileName, e);
                throw new RuntimeException(e);
            }
        }
    }

    public void deleteFile(File file) {
        try {
            if (file.exists()) {
                FileUtils.delete(file);
                log.info("System file {} was deleted", file.getName());
            }
        } catch (IOException e) {
            log.error("Failed to delete file {}", file.getName(), e);
        }
    }

    public InputStream getOtaDataStream(OtaPackageId otaPackageId) throws FileNotFoundException {
        return new FileInputStream(getOtaDataFile(otaPackageId));
    }

    public InputStream loadOtaData(OtaPackageId otaPackageId, Blob data) {
        try {
            return new FileInputStream(loadToFile(otaPackageId, data));
        } catch (FileNotFoundException e) {
            log.error("Failed to upload temporary file {} to system", otaPackageId.getId(), e);
            throw new RuntimeException(e);
        }
    }

    private CountDownLatch processFileSaving(OtaPackageId otaPackageId, Blob data) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            InputStream binaryStream = data.getBinaryStream();
            String fileName = PATH + otaPackageId.getId();
            saveAsSystemFile(fileName, binaryStream);
            latch.countDown();
            return latch;
        } catch (SQLException e) {
            log.error("Failed to get binary data from otaPackage {}", otaPackageId, e);
            throw new RuntimeException("Failed to get binary data from database");
        }
    }

    private void saveAsSystemFile(String fileName, InputStream inputStream) {
        try {
            File file = new File(fileName);
            FileUtils.copyInputStreamToFile(inputStream, file);
        } catch (IOException e) {
            log.error("Failed to copy stream to system file {}", fileName, e);
            throw new RuntimeException("Failed to save file");
        }
    }

    private boolean exist(String name) {
        File file = new File(name);
        return file.exists();
    }
}