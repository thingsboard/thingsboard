// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install;

public interface EntityDatabaseSchemaService extends DatabaseSchemaService {

    void createOrUpdateDeviceInfoView(boolean activityStateInTelemetry);

    void createOrUpdateViewsAndFunctions() throws Exception;

    void generateClusterIdIfNotExist();

}
