/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.cache.ota.files;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Slf4j
@RequiredArgsConstructor
@Component
public class BaseFileCacheService implements FileCacheService {
    @Value("${files.temporary_files_directory}" + "/ota/")
    private String PATH;
    private final static String FILE_NAME_TEMPLATE = "%s.tmp";
    private final TemporaryFileCleaner fileCleaner;

    @Override
    public BufferedOutputStream createTempFile(OtaPackageId otaPackageId) {
        try {
            String fileName = String.format(FILE_NAME_TEMPLATE, otaPackageId.getId().toString());
            File directory = new File(PATH);
            File tempFile = new File(directory, fileName);
            if (directory.exists() && tempFile.createNewFile()) {
                fileCleaner.updateFileUsageStatus(otaPackageId);
                return new BufferedOutputStream(new FileOutputStream(tempFile.getAbsolutePath()));
            }
            return new BufferedOutputStream(OutputStream.nullOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTempFileAbsolutePath(String otaPackageId) {
        return PATH + String.format(FILE_NAME_TEMPLATE, otaPackageId);
    }
}
