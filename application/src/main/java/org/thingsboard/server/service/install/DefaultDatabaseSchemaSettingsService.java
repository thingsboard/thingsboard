/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultDatabaseSchemaSettingsService implements DatabaseSchemaSettingsService {

    // map of versions from which the upgrade to the current version is possible
    // key - supported version prefix, value - display name
    private static final Map<String, String> SUPPORTED_VERSIONS_FOR_UPGRADE = Map.of(
//            "4.2.1", "4.2.1.x" // fixme update for 4.2
    );

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

        if (SUPPORTED_VERSIONS_FOR_UPGRADE.keySet().stream().noneMatch(dbSchemaVersion::startsWith)) {
            onSchemaSettingsError(String.format("Upgrade failed: database version '%s' is not supported for upgrade. Supported versions are: %s.",
                    dbSchemaVersion, SUPPORTED_VERSIONS_FOR_UPGRADE.values()
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
            packageSchemaVersion = normalizeVersion(projectInfo.getProjectVersion());
        }
        return packageSchemaVersion;
    }

    @Override
    public String getDbSchemaVersion() {
        if (schemaVersionFromDb == null) {
            Long dbVersion = getSchemaVersionFromDb();
            if (dbVersion == null) {
                onSchemaSettingsError("Upgrade failed: the database schema version is missing.");
            }

            @SuppressWarnings("DataFlowIssue")
            long version = dbVersion;

            if (version < 1_000_000_000) {
                // Old format: MMM mmm ppp (e.g., 4002001 = 4.2.1)
                long major = version / 1_000_000;
                long minor = (version % 1_000_000) / 1000;
                long maintenance = version % 1000;
                schemaVersionFromDb = major + "." + minor + "." + maintenance + ".0";
            } else {
                // New format: MMM mmm mmm ppp (e.g., 4002001001 = 4.2.1.1)
                long major = version / 1_000_000_000;
                long minor = (version % 1_000_000_000) / 1_000_000;
                long maintenance = (version % 1_000_000) / 1000;
                long patch = version % 1000;
                schemaVersionFromDb = major + "." + minor + "." + maintenance + "." + patch;
            }
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
        long maintenance = Integer.parseInt(versionParts[2]);
        long patch = Integer.parseInt(versionParts[3]);

        return major * 1_000_000_000L + minor * 1_000_000L + maintenance * 1000L + patch;
    }

    private void onSchemaSettingsError(String message) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.error(message)));
        throw new RuntimeException(message);
    }

    private String normalizeVersion(String version) {
        String[] parts = version.split("\\.");

        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int maintenance = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        int patch = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;

        return major + "." + minor + "." + maintenance + "." + patch;
    }

}
