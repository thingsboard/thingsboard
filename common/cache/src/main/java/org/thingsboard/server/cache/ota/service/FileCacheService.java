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

import javax.annotation.PostConstruct;
import java.io.*;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class FileCacheService {
    @Value("${java.io.tmpdir}/ota/")
    private String PATH;
    private final static String FILE_PATH_TEMPLATE = "%s";
    private final static long TEMPORARY_FILE_INACTIVITY_TIME = 300;//todo increase time
    private final ConcurrentMap<OtaPackageId, CountDownLatch> files = new ConcurrentHashMap<>();
    private final ConcurrentMap<OtaPackageId, Long> lastActivityTimes = new ConcurrentHashMap<>();

    @PostConstruct
    private void createScheduledTaskToClear() {
        createTempDirectoryIfNotExist();
        cleanDirectoryWithTemporaryFiles();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::deleteUnusedTemporaryFiles, 3, 3, TimeUnit.MINUTES); //todo increase time
    }

    private void createTempDirectoryIfNotExist() {
        File directory = new File(PATH);
        if(!directory.exists()){
            directory.mkdir();
        }
    }

    private void cleanDirectoryWithTemporaryFiles() {
        File directory = new File(PATH);
        if (directory.isDirectory()) {
            try {
                FileUtils.cleanDirectory(directory);
                log.info("Directory with temporary files cleaned");
            } catch (IOException e) {
                log.error("Failed to clean directory with temporary files", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void deleteUnusedTemporaryFiles() {
        long currentTime = System.currentTimeMillis();
        List<OtaPackageId> toBeDeleted = lastActivityTimes.entrySet()
                .stream()
                .filter(entry -> currentTime - entry.getValue() > TEMPORARY_FILE_INACTIVITY_TIME)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        try {
            toBeDeleted.forEach(otaId -> {
                deleteFile(otaId.getId().toString());
                lastActivityTimes.remove(otaId);
            });
        } catch (Exception e) {
            log.error(",,", e);
        }
        log.info("Deleted {} unused temporary files", toBeDeleted.size());
    }

    private void deleteFile(String otaId) {
        String fileName = PATH + otaId;
        File file = new File(String.format(FILE_PATH_TEMPLATE, fileName));
        deleteFile(file);
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
        String fileName = PATH + otaPackageId.getId();
        if (exist(fileName)) {
            lastActivityTimes.put(otaPackageId, System.currentTimeMillis());
            return new FileInputStream(String.format(FILE_PATH_TEMPLATE, fileName));
        }
        throw new FileNotFoundException();
    }

    private InputStream load(OtaPackageId otaPackageId, Blob data) throws FileNotFoundException {
        CountDownLatch latch = files.computeIfAbsent(otaPackageId, ota -> processFileSaving(ota, data));
        String fileName = PATH +  otaPackageId.getId();
        if (latch.getCount() == 0) {
            return new FileInputStream(String.format(FILE_PATH_TEMPLATE, fileName));
        } else {
            try {
                latch.await();
                return new FileInputStream(String.format(FILE_PATH_TEMPLATE, fileName));
            } catch (InterruptedException e) {
                log.error("Failed to save file {} to file system", fileName, e);
                throw new RuntimeException(e);
            }
        }
    }

    private CountDownLatch processFileSaving(OtaPackageId otaPackageId, Blob data) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            InputStream binaryStream = data.getBinaryStream();
            String fileName =PATH + otaPackageId.getId();
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
            File file = new File(String.format(FILE_PATH_TEMPLATE, fileName));
            FileUtils.copyInputStreamToFile(inputStream, file);
        } catch (IOException e) {
            log.error("Failed to copy stream to system file {}", fileName, e);
            throw new RuntimeException("Failed to save file");
        }
    }

    private boolean exist(String name) {
        File file = new File(String.format(FILE_PATH_TEMPLATE, name));
        return file.exists();
    }

    public InputStream loadOtaData(OtaPackageId otaPackageId, Blob data) {
        try {
            return load(otaPackageId, data);
        } catch (FileNotFoundException e) {
            log.error("Failed to upload temporary file {} to system", otaPackageId.getId(), e);
            throw new RuntimeException(e);
        }
    }

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
}