// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.storage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@Data
public class FileEventStorage implements EventStorage {

    @Autowired
    private FileEventStorageSettings settings;

    private final ReentrantLock readLock = new ReentrantLock();
    private final ReentrantLock writeLock = new ReentrantLock();

    private List<File> dataFiles;
    private File stateFile;

    private EventStorageWriter storageWriter;
    private EventStorageReader storageReader;

    @PostConstruct
    public void init() {
        initDataFolderIfNotExist();
        EventStorageFiles eventStorageFiles = initDataFiles();
        dataFiles = eventStorageFiles.getDataFiles();
        stateFile = eventStorageFiles.getStateFile();
        storageWriter = new EventStorageWriter(eventStorageFiles, settings);
        storageReader = new EventStorageReader(eventStorageFiles, settings);
    }

    @PreDestroy
    public void destroy() throws IOException {
        storageReader.destroy();
        storageWriter.destroy();
    }

    @Override
    public void write(UplinkMsg msg, IntegrationCallback<Void> callback) {
        writeLock.lock();
        try {
            storageWriter.write(msg, callback);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<UplinkMsg> readCurrentBatch() {
        writeLock.lock();
        try {
            storageWriter.flushIfNeeded();
        } finally {
            writeLock.unlock();
        }

        readLock.lock();
        try {
            return storageReader.read();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void discardCurrentBatch() {
        readLock.lock();
        try {
            storageReader.discardBatch();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void sleep() {
        try {
            Thread.sleep(settings.getNoRecordsSleepInterval());
        } catch (InterruptedException e) {
            log.warn("Sleep interrupted!");
        }
    }

    private void initDataFolderIfNotExist() {
        Path path = Paths.get(settings.getDataFolderPath());
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create data folder!", e);
            }
        }
    }

    private EventStorageFiles initDataFiles() {
        CopyOnWriteArrayList<File> dataFiles = new CopyOnWriteArrayList<>();
        File stateFile = null;

        File dir = new File(settings.getDataFolderPath());
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.getName().startsWith("data_")) {
                    dataFiles.add(file);
                } else if (file.getName().startsWith("state_")) {
                    stateFile = file;
                }
            }
            Collections.sort(dataFiles);
            if (dataFiles.size() == 0) {
                dataFiles.add(createNewDataFile());
            }

            if (stateFile == null) {
                stateFile = createFile("/state_", "file");
            }
            return new EventStorageFiles(stateFile, dataFiles);
        } else {
            log.error("[{}] The specified path is not referred to the directory!", settings.getDataFolderPath());
            throw new RuntimeException("The specified path is not referred to the directory! " + settings.getDataFolderPath());
        }
    }

    private File createNewDataFile() {
        return createFile("/data_", Long.toString(System.currentTimeMillis()));
    }

    private File createFile(String prefix, String fileName) {
        Path filePath = Paths.get(settings.getDataFolderPath() + prefix + fileName + ".txt");
        try {
            return Files.createFile(filePath).toFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create a new file!", e);
        }
    }

}
