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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ota.OtaPackageService;

import javax.annotation.PostConstruct;
import java.io.*;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class FileCacheService {
    private final static String PATH = "files/%s";
    private final static String FILE_NAME_TEMPLATE = "%s.tmp";
    private final static long TEMPORARY_FILE_INACTIVITY_TIME = 300;
    private final ConcurrentMap<OtaPackageId, CountDownLatch> files = new ConcurrentHashMap<>();
    private final ConcurrentMap<OtaPackageId, Long> lastActivityTimes = new ConcurrentHashMap<>();
    private final OtaPackageService otaPackageService;

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
        }catch (Exception e){
            log.error(",,", e);
        }
        log.info("Deleted {} unused temporary files", toBeDeleted.size());
    }

    private void deleteFile(String otaId) {
        if (exist(otaId)) {
            String fileName = String.format(FILE_NAME_TEMPLATE, otaId);
            File file = new File(String.format(PATH, fileName));
            file.delete();
        }
    }

    private void cleanDirectoryWithTemporaryFiles() {
        File directory = new File(String.format(PATH, ""));
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


    @PostConstruct
    private void createScheduledTaskToClear() {
        cleanDirectoryWithTemporaryFiles();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::deleteUnusedTemporaryFiles, 3, 3, TimeUnit.MINUTES);
    }

    public InputStreamResource getOtaResourceById(TenantId tenantId, OtaPackageId otaPackageId) {
        try {
            String fileName = String.format(FILE_NAME_TEMPLATE, otaPackageId.getId().toString());
            if (exist(fileName)) {
                lastActivityTimes.put(otaPackageId, System.currentTimeMillis());
                return new InputStreamResource(new FileInputStream(String.format(PATH, fileName)));
            }
            InputStreamResource resource = load(tenantId, otaPackageId);
            lastActivityTimes.put(otaPackageId, System.currentTimeMillis());
            files.remove(otaPackageId);
            return resource;
        } catch (FileNotFoundException e) {
            log.error("Failed to find resource for otaPackage {}", otaPackageId, e);
            throw new RuntimeException(e);
        }
    }

    private InputStreamResource load(TenantId tenantId, OtaPackageId otaPackageId) throws FileNotFoundException {
        CountDownLatch latch = files.computeIfAbsent(otaPackageId, ota -> processFileSaving(tenantId, ota));
        String fileName = String.format(FILE_NAME_TEMPLATE, otaPackageId.getId().toString());
        if (latch.getCount() == 0) {
            return new InputStreamResource(new FileInputStream(String.format(PATH, fileName)));
        } else {
            try {
                latch.await();
                return new InputStreamResource(new FileInputStream(String.format(PATH, fileName)));
            } catch (InterruptedException e) {
                log.error("Failed to save file {} to file system", fileName, e);
                throw new RuntimeException(e);
            }
        }
    }

    private CountDownLatch processFileSaving(TenantId tenantId, OtaPackageId otaPackageId) {
        CountDownLatch latch = new CountDownLatch(1);
        OtaPackage otaPackage = otaPackageService.findOtaPackageById(tenantId, otaPackageId);
        if (otaPackage == null) {
            latch.countDown();
            log.error("Can't find otaPackage to download file  {}", otaPackage);
            throw new RuntimeException("No such OtaPackageId");
        }
        try {
            Blob data = otaPackage.getData();
            InputStream binaryStream = data.getBinaryStream();
            String fileName = String.format(FILE_NAME_TEMPLATE, otaPackageId.getId().toString());
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
            File file = new File(String.format(PATH, fileName));
            FileUtils.copyInputStreamToFile(inputStream, file);
        } catch (IOException e) {
            log.error("Failed to copy stream to system file {}", fileName, e);
            throw new RuntimeException("Failed to save file");
        }
    }

    private boolean exist(String name) {
        File file = new File(String.format(PATH, name));
        return file.exists();
    }
}