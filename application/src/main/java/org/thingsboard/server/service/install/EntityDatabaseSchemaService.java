// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

public interface EntityDatabaseSchemaService extends DatabaseSchemaService {

    void createOrUpdateDeviceInfoView(boolean activityStateInTelemetry);

    void createOrUpdateViewsAndFunctions() throws Exception;

}
