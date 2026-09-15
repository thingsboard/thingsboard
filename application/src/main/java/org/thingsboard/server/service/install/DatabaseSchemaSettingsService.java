// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

public interface DatabaseSchemaSettingsService {

    void validateSchemaSettings();

    void createSchemaSettings();

    void updateSchemaVersion();

    void updateSchemaVersion(String version);

    String getPackageSchemaVersion();

    String getDbSchemaVersion();

}
