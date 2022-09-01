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
package org.thingsboard.server.cache.ota.files;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TemporaryFileCleaner {
    @Value("${files.temporary_files_directory}/ota/")
    private String PATH;
    private final static String FILE_NAME_TEMPLATE = "%s%s.tmp";
    private final static long TEMPORARY_FILE_INACTIVITY_TIME = 900_000;
    private final ConcurrentMap<OtaPackageId, Long> lastActivityTimes = new ConcurrentHashMap<>();

    public void updateFileUsageStatus(OtaPackageId otaPackageId) {
        lastActivityTimes.put(otaPackageId, System.currentTimeMillis());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanDirectoryWithTemporaryFiles() {
        createTempDirectoryIfNotExist();
        cleanDirectory();
        log.info("Directory {} with temporary ota files cleaned", PATH);
    }

    private void createTempDirectoryIfNotExist() {
        File directory = new File(PATH);
        if (!directory.exists()) {
            try{
                FileUtils.forceMkdir(directory);
            } catch(IOException e){
                log.error("Failed to create directory for temporary files ", e);
            }
        }
    }

    private void cleanDirectory() {
        File directory = new File(PATH);
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files == null) return;
            Arrays.stream(files).forEach(
                    file -> {
                        try {
                            FileUtils.delete(file);
                        } catch (Exception e) {
                            log.error("Failed to delete file {}", file.getName(), e);
                        }
                    }
            );
        }
    }

    @Scheduled(fixedDelay = 600_000)
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
            log.error("Failed to delete unused files", e);
        }
        log.info("Deleted {} unused temporary files", toBeDeleted.size());
    }

    private synchronized void deleteFile(String otaId) {
        String fileName = String.format(FILE_NAME_TEMPLATE, PATH, otaId);
        File file = new File(fileName);
        try (FileChannel channel = FileChannel.open(Path.of(URI.create(file.getPath())), StandardOpenOption.APPEND)) {
            FileLock lock = channel.lock();
            if (file.exists()) {
                FileUtils.delete(file);
                log.info("System file {} was deleted", file.getName());
            }
            lock.release();
        } catch (IOException e) {
            log.error("Failed to delete file {}", file.getName(), e);
        }
    }
}
