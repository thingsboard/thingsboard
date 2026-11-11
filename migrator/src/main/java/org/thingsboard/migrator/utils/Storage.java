// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.migrator.utils.PostgresService.Blob;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Component
@Slf4j
public class Storage {

    @Value("${working_directory}")
    private Path workingDir;
    @Value("${mode}")
    private String mode;
    @Value("${archive_result:true}")
    private boolean archiveResult;

    private static final String FINAL_ARCHIVE_FILE = "data.tar";

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final List<Path> createdFiles = new ArrayList<>();

    private static final String BYTES_DATA_PREFIX = "BYTES:";
    private static final String ARRAY_DATA_PREFIX = "ARRAY:";
    private static final String BLOB_DATA_PREFIX = "BLOB:";

    @PostConstruct
    private void init() throws IOException {
        Files.createDirectories(workingDir);
    }

    public void newFile(String name) throws IOException {
        Path file = getPath(name);
        Files.deleteIfExists(file);
        Files.createFile(file);
        createdFiles.add(file);
    }

    @SneakyThrows
    public Writer newWriter(String file) {
        // The raw file
        FileOutputStream fos = new FileOutputStream(getPath(file).toFile());
        // Reducing disk IOPS
        BufferedOutputStream bos = new BufferedOutputStream(fos, 128 * 1024);
        // Giving GZIP its own buffer allows the compression engine to work on large chunks of data at once.
        GZIPOutputStream gzos = new GZIPOutputStream(bos, 128 * 1024);
        // The UTF-8 Writer
        return new OutputStreamWriter(gzos, StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public void addToFile(Writer writer, Map<String, Object> row) {
        row.replaceAll((column, data) -> {
            if (data instanceof PGobject object) {
                data = object.getValue();
            } else if (data instanceof byte[] bytes) {
                data = BYTES_DATA_PREFIX + Base64.getEncoder().encodeToString(bytes);
            } else if (data instanceof PgArray array) {
                try {
                    return ARRAY_DATA_PREFIX + jsonMapper.writeValueAsString(array.getArray());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (data instanceof Blob blob) {
                return BLOB_DATA_PREFIX + Base64.getEncoder().encodeToString(blob.data());
            }
            return data;
        });
        String serialized = toJson(row);
        writer.write(serialized + System.lineSeparator());
    }

    public long count(String file) throws IOException {
        Path path = getPath(file);
        if (!Files.exists(path)) {
            return 0;
        }
        try (BufferedReader reader = newReader(file)) {
            return reader.lines().count();
        }
    }

    public String getFileInfo(String file) {
        Path path = getPath(file);
        if (Files.exists(path)) {
            try {
                long size = Files.size(path);
                String lastModified = Files.getLastModifiedTime(path).toString();
                return String.format("file: %s.gz, size: %d bytes, last modified: %s", file, size, lastModified);
            } catch (IOException e) {
                return "file: " + file + ".gz, error getting info: " + e.getMessage();
            }
        } else {
            return "file: " + file + ".gz, not found";
        }
    }

    public void readAndProcess(String file, Consumer<Map<String, Object>> processor) throws IOException {
        readAndProcess(file, 0, processor);
    }

    public void readAndProcess(String file, long skip, Consumer<Map<String, Object>> processor) throws IOException {
        try (BufferedReader reader = newReader(file)) {
            reader.lines().skip(skip).forEach(line -> {
                if (StringUtils.isNotBlank(line)) {
                    Map<String, Object> data;
                    try {
                        data = jsonMapper.readValue(line, new TypeReference<>() {});
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    data.replaceAll((key, value) -> {
                        if (value == null) return null;
                        if (key.contains("id")) {
                            try {
                                value = UUID.fromString(value.toString());
                            } catch (IllegalArgumentException ignored) {}
                        } else if (value instanceof Map) {
                            value = jsonMapper.valueToTree(value);
                        } else if (value instanceof String string) {
                            if (string.startsWith(BYTES_DATA_PREFIX)) {
                                value = Base64.getDecoder().decode(StringUtils.removeStart(string, BYTES_DATA_PREFIX));
                            } else if (string.startsWith(ARRAY_DATA_PREFIX)) {
                                try {
                                    value = jsonMapper.readValue(StringUtils.removeStart(string, ARRAY_DATA_PREFIX), new TypeReference<String[]>() {});
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            } else if (string.startsWith(BLOB_DATA_PREFIX)) {
                                byte[] blobData = Base64.getDecoder().decode(StringUtils.removeStart(string, BLOB_DATA_PREFIX));
                                return new Blob(blobData);
                            }
                        }
                        return value;
                    });
                    processor.accept(data);
                }
            });
        }
    }

    private BufferedReader newReader(String file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(getPath(file).toFile());
        return new BufferedReader(new InputStreamReader(new GZIPInputStream(fileInputStream)));
    }

    @SneakyThrows
    private String toJson(Object o) {
        return jsonMapper.writeValueAsString(o);
    }

    private Path getPath(String file) {
        return workingDir.resolve(file + ".gz");
    }

    @SneakyThrows
    public void close() {
        if (createdFiles.isEmpty()) {
            return;
        }
        if (mode.equals("TENANT_DATA_EXPORT") && archiveResult) {
            log.info("Archiving {} files", createdFiles.size());
            TarArchiveOutputStream tarArchive = new TarArchiveOutputStream(new FileOutputStream(workingDir.resolve(FINAL_ARCHIVE_FILE).toFile()));
            for (Path file : createdFiles) {
                TarArchiveEntry archiveEntry = new TarArchiveEntry(file, file.getFileName().toString());
                tarArchive.putArchiveEntry(archiveEntry);
                Files.copy(file, tarArchive);
                tarArchive.closeArchiveEntry();
                Files.delete(file);
            }
            tarArchive.close();
        }
    }

    @SneakyThrows
    public void open() {
        if (mode.equals("TENANT_DATA_IMPORT") && archiveResult) {
            Path archiveFile = workingDir.resolve(FINAL_ARCHIVE_FILE);
            try (TarArchiveInputStream tarArchive = new TarArchiveInputStream(new FileInputStream(archiveFile.toFile()))) {
                log.info("Unarchiving {}", archiveFile.getFileName());
                TarArchiveEntry entry;
                while ((entry = tarArchive.getNextEntry()) != null) {
                    try (OutputStream file = new FileOutputStream(workingDir.resolve(entry.getName()).toFile())) {
                        tarArchive.transferTo(file);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
