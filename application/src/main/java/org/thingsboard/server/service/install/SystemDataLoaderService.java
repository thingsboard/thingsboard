// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

public interface SystemDataLoaderService {

    void createSysAdmin() throws Exception;

    void createDefaultTenantProfiles() throws Exception;

    void createAdminSettings() throws Exception;

    void createRandomJwtSettings() throws Exception;

    void updateSecuritySettings() throws Exception;

    void createOAuth2Templates() throws Exception;

    void loadSystemWidgets() throws Exception;

    void loadDemoData() throws Exception;

    void createQueues();

    void createDefaultNotificationConfigs();

    void updateDefaultNotificationConfigs(boolean updateTenants);

}
