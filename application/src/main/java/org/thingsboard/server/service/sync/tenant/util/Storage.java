/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.tenant.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
@Slf4j
public class Storage {

    @Value("${tenant.export.working_directory:/tmp/tenants-export-import}")
    private String workingDirectory;

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private final Map<UUID, Map<Path, Writer>> files = new HashMap<>();

    @SneakyThrows
    public void init(UUID tenantId) {
        Path workingDirectory = Path.of(this.workingDirectory, tenantId.toString());
        Files.createDirectories(workingDirectory);
        files.put(tenantId, new HashMap<>());
    }

    @SneakyThrows
    public void save(UUID tenantId, String type, DataWrapper dataWrapper) {
        Path file = getPath(tenantId, type);

        Writer writer = files.get(tenantId).computeIfAbsent(file, path -> {
            try {
                Files.deleteIfExists(file);
                Files.createFile(file);

                FileOutputStream fileOutputStream = new FileOutputStream(file.toFile());
                return new OutputStreamWriter(new GZIPOutputStream(fileOutputStream), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        String data = toJson(dataWrapper);
        writer.write(data + System.lineSeparator());
//        log.info("[{}] Saved entity to {}: {}", tenantId, file, dataWrapper.getEntity());
    }

    @SneakyThrows
    public void finish(UUID tenantId) {
        for (Writer writer : files.get(tenantId).values()) {
            writer.close();
        }

        Map<Path, Writer> files = this.files.get(tenantId);
        TarArchiveOutputStream tarArchive = new TarArchiveOutputStream(new FileOutputStream(Path.of(workingDirectory, tenantId.toString(), "data.tar").toFile()));
        for (Path file : files.keySet()) {
            TarArchiveEntry archiveEntry = new TarArchiveEntry(file, file.getFileName().toString());
            tarArchive.putArchiveEntry(archiveEntry);
            Files.copy(file, tarArchive);
            tarArchive.closeArchiveEntry();
//            Files.delete(file);
        }
        tarArchive.close();
    }

    @SneakyThrows
    public InputStream download(UUID tenantId) {
        return Files.newInputStream(Path.of(workingDirectory, tenantId.toString(), "data.tar"));
    }

    @SneakyThrows
    public void unwrap(InputStream dataStream) {
        Path workingDirectory = Path.of(this.workingDirectory, "importing");
        FileUtils.deleteDirectory(workingDirectory.toFile());
        Files.createDirectories(workingDirectory);

        TarArchiveInputStream tarArchive = new TarArchiveInputStream(dataStream);
        TarArchiveEntry archiveEntry;
        while ((archiveEntry = tarArchive.getNextEntry()) != null) {
            Path file = workingDirectory.resolve(archiveEntry.getName());
            Files.createFile(file);
            IOUtils.copy(tarArchive, Files.newOutputStream(file));
        }
    }

    @SneakyThrows
    public void read(String type, Consumer<DataWrapper> processor) {
        Path file = Path.of(this.workingDirectory, "importing", type + ".gz");
        if (!Files.exists(file)) {
            log.info("No data for {}", type);
            return;
        }

        FileInputStream fileInputStream = new FileInputStream(file.toFile());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(fileInputStream)))) {
            reader.lines().forEach(line -> {
                DataWrapper dataWrapper;
                try {
                    dataWrapper = jsonMapper.readValue(line, DataWrapper.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                processor.accept(dataWrapper);
            });
        }
    }

    @SneakyThrows
    private String toJson(Object o) {
        return jsonMapper.writeValueAsString(o);
    }

    private Path getPath(UUID tenantId, String name) {
        return Path.of(workingDirectory, tenantId.toString(), name + ".gz");
    }

}
