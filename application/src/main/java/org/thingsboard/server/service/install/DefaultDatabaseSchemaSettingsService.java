// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.install.lts.LtsVersion;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultDatabaseSchemaSettingsService implements DatabaseSchemaSettingsService {

    // map of versions from which the upgrade to the current version is possible
    // key - supported version prefix, value - display name
    private static final Map<String, String> SUPPORTED_VERSIONS_FOR_UPGRADE = Map.of(
            "4.3.0", "4.3.0.x",
            "4.3.1", "4.3.1.x"
    );

    private static final String CE_PRODUCT = "CE";
    // The first CE version that converts to PE: the one that ships the community grant. Anything older takes the
    // cheap CE-to-CE patch upgrade first. 4.3 stays supported, so later 4.3.1.x releases convert as well.
    private static final LtsVersion MIN_CE_VERSION_FOR_UPGRADE = LtsVersion.parse("4.3.1.4");

    private final ProjectInfo projectInfo;
    private final JdbcTemplate jdbcTemplate;

    private String packageSchemaVersion;
    private String schemaVersionFromDb;

    @Override
    public void validateSchemaSettings() {
        // The flag also drops the CE floor below, and that is what it costs: the 4.3.1.4 minimum is
        // what keeps a CE source above 4.2.2.3, and so keeps LtsMigrationService.select() from ever returning
        // the V4_2_2_3 <-> V4_3_1_3 reproduction-duplicate pair its own invariant forbids selecting together.
        // Forcing an older CE database through without --fromVersion reopens that pair.
        if (isForcedReUpgrade()) {
            log.info("Skipped DB schema version check due to SKIP_SCHEMA_VERSION_CHECK set to 'true'.");
            return;
        }

        String dbSchemaVersion = getDbSchemaVersion();
        if (isUpgradeFromCe()) {
            LtsVersion ceVersion = LtsVersion.parse(dbSchemaVersion);
            if (ceVersion.compareTo(MIN_CE_VERSION_FOR_UPGRADE) < 0) {
                onSchemaSettingsError(String.format("Upgrade failed: transitioning from CE to PE requires the database to be at version '%s' or newer, but it is at '%s'. " +
                                                    "Please upgrade ThingsBoard CE to %s first.",
                        MIN_CE_VERSION_FOR_UPGRADE, dbSchemaVersion, MIN_CE_VERSION_FOR_UPGRADE));
            }
            // Without this the migration chain would select nothing and the PE schema would be created on top of a
            // newer CE database, silently.
            if (ceVersion.compareTo(LtsVersion.parse(getPackageSchemaVersion())) > 0) {
                onSchemaSettingsError(String.format("Upgrade failed: the database is at CE version '%s', which is newer than this ThingsBoard PE package ('%s'). " +
                                                    "Please use a PE package of version '%s' or newer.",
                        dbSchemaVersion, getPackageSchemaVersion(), dbSchemaVersion));
            }
        } else {
            if (dbSchemaVersion.equals(getPackageSchemaVersion())) {
                onSchemaSettingsError("Upgrade failed: database already upgraded to current version. You can set SKIP_SCHEMA_VERSION_CHECK to 'true' if force re-upgrade needed.");
            }

            if (SUPPORTED_VERSIONS_FOR_UPGRADE.keySet().stream().noneMatch(dbSchemaVersion::startsWith)) {
                onSchemaSettingsError(String.format("Upgrade failed: database version '%s' is not supported for upgrade. Supported versions are: %s.",
                        dbSchemaVersion, SUPPORTED_VERSIONS_FOR_UPGRADE.values()
                ));
            }
        }
    }

    @Override
    public boolean isForcedReUpgrade() {
        return DefaultDataUpdateService.getEnv("SKIP_SCHEMA_VERSION_CHECK", false);
    }

    @Override
    public boolean isUpgradeFromCe() {
        // The branch is chosen here, so the marker is validated here too: a garbage or missing value must fail
        // rather than fall through to the PE path, including when SKIP_SCHEMA_VERSION_CHECK skips validateSchemaSettings().
        String product = getProductFromDb();
        if (!CE_PRODUCT.equals(product) && !projectInfo.getProductType().equals(product)) {
            onSchemaSettingsError(String.format("Upgrade failed: unrecognized product '%s' in the database schema settings, expected '%s' or '%s'.",
                    product, CE_PRODUCT, projectInfo.getProductType()));
        }
        return CE_PRODUCT.equals(product);
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
        jdbcTemplate.execute("UPDATE tb_schema_settings SET schema_version = " + getPackageSchemaVersionForDb() + ", product = '" + projectInfo.getProductType() + "'");
    }

    @Override
    public void updateSchemaVersion(String version) {
        jdbcTemplate.execute("UPDATE tb_schema_settings SET schema_version = " + toDbVersion(version));
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
        return toDbVersion(getPackageSchemaVersion());
    }

    private long toDbVersion(String version) {
        LtsVersion v = LtsVersion.parse(version);
        return v.major() * 1_000_000_000L + v.minor() * 1_000_000L + v.maintenance() * 1000L + v.patch();
    }

    private void onSchemaSettingsError(String message) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.error(message)));
        throw new RuntimeException(message);
    }

    private String normalizeVersion(String version) {
        return LtsVersion.parse(version).toString();
    }

}
