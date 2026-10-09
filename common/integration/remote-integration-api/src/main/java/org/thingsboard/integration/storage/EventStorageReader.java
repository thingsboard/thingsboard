// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Data
@Slf4j
class EventStorageReader {
    private EventStorageFiles files;
    private FileEventStorageSettings settings;

    private BufferedReader bufferedReader;

    private volatile EventStorageReaderPointer currentPos;
    private volatile EventStorageReaderPointer newPos;
    private List<UplinkMsg> currentBatch;

    EventStorageReader(EventStorageFiles files, FileEventStorageSettings settings) {
        this.files = files;
        this.settings = settings;
        this.currentPos = readStateFile();
        this.newPos = currentPos.copy();
    }

    List<UplinkMsg> read() {
        log.debug("[{}:{}] Check for new messages in storage", newPos.getFile(), newPos.getLine());
        if (!CollectionUtils.isEmpty(currentBatch) && !currentPos.equals(newPos)) {
            log.debug("The previous batch was not discarded!");
            return currentBatch;
        }
        currentBatch = new ArrayList<>();
        int recordsToRead = settings.getMaxReadRecordsCount();
        while (recordsToRead > 0) {
            try {
                int currentLineInFile = newPos.getLine();
                BufferedReader reader = getOrInitBufferedReader(newPos);
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        currentBatch.add(UplinkMsg.parseFrom(Base64.getDecoder().decode(line)));
                        recordsToRead--;
                    } catch (Exception e) {
                        log.warn("Could not parse line [{}] to uplink message!", line, e);
                    } finally {
                        currentLineInFile++;
                    }

                    newPos.setLine(currentLineInFile);
                    if (recordsToRead == 0) {
                        break;
                    }
                }

                if (currentLineInFile >= settings.getMaxRecordsPerFile()) {
                    File nextFile = getNextFile(files, newPos);
                    if (nextFile != null) {
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                        bufferedReader = null;
                        newPos = new EventStorageReaderPointer(nextFile, 0);
                    } else {
                        // No more records to read for now
                        break;
                    }
                } else {
                    // No more records to read for now
                    break;
                }
            } catch (IOException e) {
                log.warn("[{}] Failed to read file!", newPos.getFile().getName(), e);
                break;
            }
        }
        log.debug("Got {} mesages from storage", currentBatch.size());
        return currentBatch;
    }

    void discardBatch() {
        currentPos = newPos.copy();
        writeInfoToStateFile(currentPos);
    }

    private File getNextFile(EventStorageFiles files, EventStorageReaderPointer newPos) {
        boolean found = false;
        for (File file : files.getDataFiles()) {
            if (found) {
                return file;
            }
            if (file.getName().equals(newPos.getFile().getName())) {
                found = true;
            }
        }
        if (found) {
            return null;
        } else {
            return files.getDataFiles().get(0);
        }
    }

    private BufferedReader getOrInitBufferedReader(EventStorageReaderPointer pointer) {
        try {
            if (bufferedReader == null) {
                bufferedReader = Files.newBufferedReader(pointer.getFile().toPath());
                int linesToSkip = pointer.getLine();
                if (linesToSkip > 0) {
                    while (linesToSkip != 0 && bufferedReader.readLine() != null) {
                        linesToSkip--;
                    }
                }
            }
            return bufferedReader;
        } catch (IOException e) {
            log.error("Failed to initialize buffered reader!", e);
            throw new RuntimeException("Failed to initialize buffered reader!", e);
        }
    }

    private EventStorageReaderPointer readStateFile() {
        JsonNode stateDataNode = null;
        try (BufferedReader br = Files.newBufferedReader(files.getStateFile().toPath())) {
            stateDataNode = JacksonUtil.OBJECT_MAPPER.readTree(br);
        } catch (IOException e) {
            log.warn("Failed to fetch info from state file!", e);
        }

        File readerFile = null;
        int readerPos = 0;
        if (stateDataNode != null && stateDataNode.has("position") && stateDataNode.has("file")) {
            readerPos = stateDataNode.get("position").asInt();
            for (File file : files.getDataFiles()) {
                if (file.getName().equals(stateDataNode.get("file").asText())) {
                    readerFile = file;
                    break;
                }
            }
        }
        if (readerFile == null) {
            readerFile = files.getDataFiles().get(0);
            readerPos = 0;
        }
        log.info("Initializing from state file: [{}:{}]", readerFile.getAbsolutePath(), readerPos);
        return new EventStorageReaderPointer(readerFile, readerPos);
    }

    private void writeInfoToStateFile(EventStorageReaderPointer pointer) {
        try {
            ObjectNode stateFileNode = JacksonUtil.newObjectNode();
            stateFileNode.put("position", pointer.getLine());
            stateFileNode.put("file", pointer.getFile().getName());
            Files.write(Paths.get(files.getStateFile().toURI()), JacksonUtil.toString(stateFileNode).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Failed to update state file!", e);
        }
    }

    void destroy() throws IOException {
        if (bufferedReader != null) {
            bufferedReader.close();
        }
    }
}
