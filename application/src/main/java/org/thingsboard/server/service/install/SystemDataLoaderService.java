// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JsonNode;

public interface SystemDataLoaderService {

    void createDefaultTenantProfiles() throws Exception;

    void createAdminSettings() throws Exception;

    void createRandomJwtSettings() throws Exception;

    void updateSecuritySettings() throws Exception;

    void loadMailTemplates() throws Exception;

    void updateMailTemplates(JsonNode value) throws Exception;

    void createOAuth2Templates() throws Exception;

    void loadSystemWidgets() throws Exception;

    void createQueues();

    void createDefaultNotificationConfigs();

    void updateDefaultNotificationConfigs(boolean updateTenants);

    void createDefaultCustomMenu();
}
