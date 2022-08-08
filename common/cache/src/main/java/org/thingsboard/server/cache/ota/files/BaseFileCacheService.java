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
package org.thingsboard.server.cache.ota.files;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class BaseFileCacheService implements FileCacheService {
    @Value("${files.temporary_files_directory}/ota/")
    private String PATH;
    private final static String FILE_NAME_TEMPLATE = "%s.tmp";
    private final TemporaryFileCleaner fileCleaner;
    private final ConcurrentMap<OtaPackageId, Boolean> files = new ConcurrentHashMap<>();


    @Override
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

    @Override
    public Optional<File> getOtaDataFile(OtaPackageId otaPackageId) {
        String fileName = PATH + String.format(FILE_NAME_TEMPLATE,otaPackageId.getId().toString());
        if (exist(fileName)) {
            fileCleaner.updateFileUsageStatus(otaPackageId);
            return Optional.of(new File(fileName));
        }
        return Optional.empty();
    }

    @Override
    public File loadToFile(OtaPackageId otaPackageId, InputStream data) {
        if (otaPackageId == null || data == null) {
            log.error("Received null variables: {}", otaPackageId == null ? "otaPackageId" : "data");
            throw new RuntimeException("Input values can not be null");
        }
        files.computeIfAbsent(otaPackageId, ota -> processFileSaving(ota, data));
        String fileName = PATH + String.format(FILE_NAME_TEMPLATE,otaPackageId.getId().toString());
        return new File(fileName);
    }


    private Boolean processFileSaving(OtaPackageId otaPackageId, InputStream data) {
        String fileName = PATH + String.format(FILE_NAME_TEMPLATE,otaPackageId.getId().toString());
        saveAsSystemFile(fileName, data);
        fileCleaner.updateFileUsageStatus(otaPackageId);
        return true;
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