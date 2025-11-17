/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.hash.Hashing;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.DatabaseSchemaSettingsService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Runs at application startup and applies no-downtime data updates
 * when the package PATCH version increases (e.g., 4.2.1.0 -> 4.2.1.1).
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class SystemPatchApplier {

    private static final long ADVISORY_LOCK_ID = 7536891047216478431L;

    private final JdbcTemplate jdbcTemplate;
    private final InstallScripts installScripts;
    private final DatabaseSchemaSettingsService schemaSettingsService;
    private final WidgetTypeService widgetTypeService;

    @PostConstruct
    private void init() {
        ExecutorService executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("system-patch-applier"));
        executor.submit(() -> {
            try {
                applyPatchIfNeeded();
            } catch (Exception e) {
                log.error("Failed to apply system data patch updates", e);
            } finally {
                executor.shutdown();
            }
        });
    }

    private void applyPatchIfNeeded() {
        boolean skipVersionCheck = DefaultDataUpdateService.getEnv("SKIP_PATCH_VERSION_CHECK", false);
        if (!skipVersionCheck && !isVersionChanged()) {
            return;
        }

        if (!acquireAdvisoryLock()) {
            log.trace("Could not acquire advisory lock. Another node is processing patch updates.");
            return;
        }

        try {
            int updated = updateWidgetTypes();
            log.info("Updated {} widget types", updated);

            schemaSettingsService.updateSchemaVersion();
            log.info("System data patch update completed successfully");

        } finally {
            releaseAdvisoryLock();
        }
    }

    private boolean isVersionChanged() {
        String packageVersion = schemaSettingsService.getPackageSchemaVersion();
        String dbVersion = schemaSettingsService.getDbSchemaVersion();

        log.trace("Package version: {}, DB schema version: {}", packageVersion, dbVersion);

        VersionInfo packageVersionInfo = parseVersion(packageVersion);
        VersionInfo dbVersionInfo = parseVersion(dbVersion);

        if (packageVersionInfo == null || dbVersionInfo == null) {
            log.warn("Unable to parse versions. Package: {}, DB: {}", packageVersion, dbVersion);
            return false;
        }

        if (!isPatchVersionChanged(packageVersionInfo, dbVersionInfo)) {
            return false;
        }

        log.info("Patch version increased from {} to {}. Starting system data update.", dbVersion, packageVersion);
        return true;
    }

    private boolean isPatchVersionChanged(VersionInfo packageVersion, VersionInfo dbVersion) {
        return packageVersion.major == dbVersion.major && packageVersion.minor == dbVersion.minor
                && packageVersion.maintenance == dbVersion.maintenance && packageVersion.patch > dbVersion.patch;
    }

    private int updateWidgetTypes() {
        AtomicInteger updated = new AtomicInteger();
        Path widgetTypesDir = installScripts.getWidgetTypesDir();

        if (!Files.exists(widgetTypesDir)) {
            log.trace("Widget types directory does not exist: {}", widgetTypesDir);
            return 0;
        }

        try (Stream<Path> dirStream = listDir(widgetTypesDir).filter(path -> path.toString().endsWith(InstallScripts.JSON_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            if (updateWidgetTypeFromFile(path)) {
                                updated.incrementAndGet();
                            }
                        } catch (Exception e) {
                            log.error("Unable to update widget type from json: [{}]", path.toString());
                            throw new RuntimeException("Unable to update widget type from json", e);
                        }
                    }
            );
        }

        return updated.get();
    }

    private boolean updateWidgetTypeFromFile(Path filePath) {
        JsonNode json = JacksonUtil.toJsonNode(filePath.toFile());
        WidgetTypeDetails fileWidgetType = JacksonUtil.treeToValue(json, WidgetTypeDetails.class);
        String fqn = fileWidgetType.getFqn();

        WidgetTypeDetails existingWidgetType = widgetTypeService.findWidgetTypeDetailsByTenantIdAndFqn(TenantId.SYS_TENANT_ID, fqn);
        if (existingWidgetType == null) {
            // We expect only update here, so it's probably never happening, but for test purpose leave it like this:
            throw new RuntimeException("Widget type not found: " + fqn);
        }
        if (isWidgetTypeChanged(existingWidgetType, fileWidgetType)) {
            existingWidgetType.setDescription(fileWidgetType.getDescription());
            existingWidgetType.setName(fileWidgetType.getName());
            existingWidgetType.setDescriptor(fileWidgetType.getDescriptor());
            widgetTypeService.saveWidgetType(existingWidgetType);
            log.trace("Updated widget type: {}", fqn);
            return true;
        }

        log.trace("Widget type unchanged: {}", fqn);
        return false;
    }

    private boolean isWidgetTypeChanged(WidgetTypeDetails existing, WidgetTypeDetails file) {
        if (!isDescriptorEqual(existing.getDescriptor(), file.getDescriptor())) {
            return true;
        }

        if (!Objects.equals(existing.getName(), file.getName())) {
            return true;
        }

        return !Objects.equals(existing.getDescription(), file.getDescription());
    }

    private boolean isDescriptorEqual(JsonNode desc1, JsonNode desc2) {
        if (desc1 == null && desc2 == null) {
            return true;
        }
        if (desc1 == null || desc2 == null) {
            return false;
        }

        try {
            String hash1 = computeChecksum(desc1);
            String hash2 = computeChecksum(desc2);
            return Objects.equals(hash1, hash2);
        } catch (Exception e) {
            log.warn("Failed to compare descriptors using checksum, falling back to equals", e);
            return desc1.equals(desc2);
        }
    }

    private String computeChecksum(JsonNode node) {
        String canonicalString = JacksonUtil.toCanonicalString(node);
        if (canonicalString == null) {
            return null;
        }
        return Hashing.sha256().hashBytes(canonicalString.getBytes()).toString();
    }

    private boolean acquireAdvisoryLock() {
        try {
            Boolean acquired = jdbcTemplate.queryForObject(
                    "SELECT pg_try_advisory_lock(?)",
                    Boolean.class,
                    ADVISORY_LOCK_ID
            );
            if (Boolean.TRUE.equals(acquired)) {
                log.trace("Acquired advisory lock");
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to acquire advisory lock", e);
            return false;
        }
    }

    private void releaseAdvisoryLock() {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT pg_advisory_unlock(?)",
                    Boolean.class,
                    ADVISORY_LOCK_ID
            );
            log.debug("Released advisory lock");
        } catch (Exception e) {
            log.error("Failed to release advisory lock", e);
        }
    }

    private VersionInfo parseVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int maintenance = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            int patch = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
            return new VersionInfo(major, minor, maintenance, patch);
        } catch (Exception e) {
            log.error("Failed to parse version: {}", version, e);
            return null;
        }
    }

    private Stream<Path> listDir(Path dir) {
        try {
            return Files.list(dir);
        } catch (NoSuchFileException e) {
            return Stream.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public record VersionInfo(int major, int minor, int maintenance, int patch) {}

}
