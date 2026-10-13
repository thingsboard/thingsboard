// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install;

public interface DatabaseSchemaSettingsService {

    void validateSchemaSettings();

    /**
     * Whether the operator forced this offline upgrade past the version check (SKIP_SCHEMA_VERSION_CHECK). Besides
     * suppressing the validation errors, it makes the LTS migration selection inclusive of the stored version, so a
     * database already at the package version re-runs that version's own migration instead of selecting nothing.
     */
    boolean isForcedReUpgrade();

    /**
     * Whether the database to be upgraded is a CE one, decided by the product marker the database itself carries
     * rather than by an operator flag.
     */
    boolean isUpgradeFromCe();

    void createSchemaSettings();

    void updateSchemaVersion();

    void updateSchemaVersion(String version);

    String getPackageSchemaVersion();

    String getDbSchemaVersion();

}
