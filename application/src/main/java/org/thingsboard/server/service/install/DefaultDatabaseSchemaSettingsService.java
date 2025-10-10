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
package org.thingsboard.server.service.install;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.util.List;

@Service
@Profile("install")
@Slf4j
@RequiredArgsConstructor
public class DefaultDatabaseSchemaSettingsService implements DatabaseSchemaSettingsService {

    // This list should include all versions which are compatible for the upgrade.
    // The compatibility cycle usually breaks when we have some scripts written in Java that may not work after new release.
    private static final List<String> SUPPORTED_VERSIONS_FOR_UPGRADE = List.of("4.2.1");

    private final ProjectInfo projectInfo;
    private final JdbcTemplate jdbcTemplate;

    private String packageSchemaVersion;
    private String schemaVersionFromDb;

    @Override
    public void validateSchemaSettings() {
        if (DefaultDataUpdateService.getEnv("SKIP_SCHEMA_VERSION_CHECK", false)) {
            log.info("Skipped DB schema version check due to SKIP_SCHEMA_VERSION_CHECK set to 'true'.");
            return;
        }

        String product = getProductFromDb();
        if (!projectInfo.getProductType().equals(product)) {
            onSchemaSettingsError(String.format("Upgrade failed: can't upgrade ThingsBoard %s database using ThingsBoard %s.", product, projectInfo.getProductType()));
        }

        String dbSchemaVersion = getDbSchemaVersion();
        if (dbSchemaVersion.equals(getPackageSchemaVersion())) {
            onSchemaSettingsError("Upgrade failed: database already upgraded to current version. You can set SKIP_SCHEMA_VERSION_CHECK to 'true' if force re-upgrade needed.");
        }

        if (!SUPPORTED_VERSIONS_FOR_UPGRADE.contains(dbSchemaVersion)) {
            onSchemaSettingsError(String.format("Upgrade failed: database version '%s' is not supported for upgrade. Supported versions are: %s.",
                    dbSchemaVersion, SUPPORTED_VERSIONS_FOR_UPGRADE
            ));
        }
    }

    @Override
    public void createSchemaSettings() {
        Long schemaVersion = getSchemaVersionFromDb();
        if (schemaVersion == null) {
            jdbcTemplate.execute("INSERT INTO tb_schema_settings (schema_version, product) VALUES (" + getPackageSchemaVersionForDb() + ", '" + projectInfo.getProductType() + "')");
        }
    }

    @Override
    public void updateSchemaVersion() {
        jdbcTemplate.execute("UPDATE tb_schema_settings SET schema_version = " + getPackageSchemaVersionForDb());
    }

    @Override
    public String getPackageSchemaVersion() {
        if (packageSchemaVersion == null) {
            packageSchemaVersion = projectInfo.getProjectVersion();
        }
        return packageSchemaVersion;
    }

    @Override
    public String getDbSchemaVersion() {
        if (schemaVersionFromDb == null) {
            Long version = getSchemaVersionFromDb();
            if (version == null) {
                onSchemaSettingsError("Upgrade failed: the database schema version is missing.");
            }

            @SuppressWarnings("DataFlowIssue")
            long major = version / 1000000;
            long minor = (version % 1000000) / 1000;
            long patch = version % 1000;

            schemaVersionFromDb = major + "." + minor + "." + patch;
        }
        return schemaVersionFromDb;
    }

    private Long getSchemaVersionFromDb() {
        return jdbcTemplate.queryForList("SELECT schema_version FROM tb_schema_settings", Long.class).stream().findFirst().orElse(null);
    }

    private String getProductFromDb() {
        return jdbcTemplate.queryForList("SELECT product FROM tb_schema_settings", String.class).stream().findFirst().orElse(null);
    }

    private long getPackageSchemaVersionForDb() {
        String[] versionParts = getPackageSchemaVersion().split("\\.");

        long major = Integer.parseInt(versionParts[0]);
        long minor = Integer.parseInt(versionParts[1]);
        long patch = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;

        return major * 1000000 + minor * 1000 + patch;
    }

    private void onSchemaSettingsError(String message) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.error(message)));
        throw new RuntimeException(message);
    }
}
