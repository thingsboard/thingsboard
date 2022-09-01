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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.OtaPackageId;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class BaseFileCacheService implements FileCacheService {

    @Value("${files.temporary_files_directory:}")
    private String tmpDir;
    @Value("${java.io.tmpdir}")
    private String defaultTmpDir;

    private final static String FILE_NAME_TEMPLATE = "%s.tmp";
    private final static long TEMPORARY_FILE_INACTIVITY_TIME = 900_000;
    private final ConcurrentMap<EntityId, OtaFileState> files = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(tmpDir)) {
            tmpDir = defaultTmpDir;
        }
    }

    @Override
    public File findOrLoad(OtaPackageId otaId, Supplier<InputStream> data) {
        if (otaId == null || data == null) {
            log.error("Received null variables: {}", otaId == null ? "otaPackageId" : "data");
            throw new RuntimeException("Input values can not be null");
        }
        var state = files.computeIfAbsent(otaId, tmp ->
                new OtaFileState(otaId, Paths.get(tmpDir, "ota", String.format(FILE_NAME_TEMPLATE, otaId.getId().toString()))));
        Lock lock = state.getLock();
        lock.lock();
        try {
            if (!state.exists()) {
                saveAsSystemFile(state.getFile(), data.get());
            }
            state.updateLastActivityTime();
            return state.getFile();
        } finally {
            lock.unlock();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanDirectoryWithTemporaryFiles() {
        createTempDirectoryIfNotExist();
        cleanDirectory();
        log.info("Directory {} with temporary ota files cleaned", tmpDir);
    }

    private void createTempDirectoryIfNotExist() {
        File directory = Paths.get(tmpDir, "ota").toFile();
        if (!directory.exists()) {
            try {
                FileUtils.forceMkdir(directory);
            } catch (IOException e) {
                log.error("Failed to create directory for temporary files ", e);
            }
        }
    }

    private void cleanDirectory() {
        File directory = Paths.get(tmpDir, "ota").toFile();
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
        List<OtaFileState> toBeDeleted = files.values()
                .stream()
                .filter(entry -> isExpired(currentTime, entry))
                .collect(Collectors.toList());
        try {
            toBeDeleted.forEach(state -> {
                state.getLock().lock();
                try {
                    if (isExpired(currentTime, state)) {
                        deleteFile(state.getFile());
                    }
                    files.remove(state.getOtaId());
                } finally {
                    state.getLock().unlock();
                }
            });
        } catch (Exception e) {
            log.error("Failed to delete unused files", e);
        }
        log.info("Deleted {} unused temporary files", toBeDeleted.size());
    }

    private boolean isExpired(long currentTime, OtaFileState entry) {
        return currentTime - entry.getLastActivityTime() > TEMPORARY_FILE_INACTIVITY_TIME;
    }

    private synchronized void deleteFile(File file) {
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

    private void saveAsSystemFile(File file, InputStream inputStream) {
        try {
            FileUtils.copyInputStreamToFile(inputStream, file);
        } catch (IOException e) {
            log.error("Failed to copy stream to system file {}", file.getName(), e);
            throw new RuntimeException("Failed to save file");
        }
    }

}