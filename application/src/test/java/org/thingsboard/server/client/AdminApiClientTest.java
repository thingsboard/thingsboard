// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.GetAdminSettingsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveAdminSettingsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveSecuritySettingsArgs;
import org.thingsboard.client.model.AdminSettings;
import org.thingsboard.client.model.FeaturesInfo;
import org.thingsboard.client.model.JwtSettings;
import org.thingsboard.client.model.SecuritySettings;
import org.thingsboard.client.model.SystemInfo;
import org.thingsboard.client.model.UpdateMessage;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class AdminApiClientTest extends AbstractApiClientTest {

    @Test
    public void testAdminSettingsLifecycle() throws Exception {
        client.login("sysadmin@thingsboard.org", "sysadmin");

        AdminSettings mailSettings = client.getAdminSettings(GetAdminSettingsArgs.builder()
                .key("mail")
                .systemByDefault(true)
                .build());
        assertNotNull(mailSettings);
        assertNotNull(mailSettings.getKey());
        assertEquals("mail", mailSettings.getKey());
        assertNotNull(mailSettings.getJsonValue());

        AdminSettings generalSettings = client.getAdminSettings(GetAdminSettingsArgs.builder()
                .key("general")
                .systemByDefault(true)
                .build());
        assertNotNull(generalSettings);
        assertEquals("general", generalSettings.getKey());
        assertNotNull(generalSettings.getJsonValue());
        assertNotNull(generalSettings.getJsonValue().get("baseUrl").asText());

        ((ObjectNode) generalSettings.getJsonValue()).put("prohibitDifferentUrl", true);
        AdminSettings updatedGeneralSettings = client.saveAdminSettings(SaveAdminSettingsArgs.builder()
                .adminSettings(generalSettings)
                .build());
        assertTrue(updatedGeneralSettings.getJsonValue().get("prohibitDifferentUrl").asBoolean());

        SecuritySettings securitySettings = client.getSecuritySettings();
        assertNotNull(securitySettings);
        assertNotNull(securitySettings.getPasswordPolicy());
        Integer originalMaxAttempts = securitySettings.getMaxFailedLoginAttempts();

        securitySettings.setMaxFailedLoginAttempts(10);
        SecuritySettings updatedSecurity = client.saveSecuritySettings(SaveSecuritySettingsArgs.builder()
                .securitySettings(securitySettings)
                .build());
        assertNotNull(updatedSecurity);
        assertEquals(10, updatedSecurity.getMaxFailedLoginAttempts().intValue());

        updatedSecurity.setMaxFailedLoginAttempts(originalMaxAttempts);
        client.saveSecuritySettings(SaveSecuritySettingsArgs.builder()
                .securitySettings(updatedSecurity)
                .build());

        JwtSettings jwtSettings = client.getJwtSettings();
        assertNotNull(jwtSettings);
        assertNotNull(jwtSettings.getTokenExpirationTime());
        assertNotNull(jwtSettings.getRefreshTokenExpTime());
        assertEquals("thingsboard.io", jwtSettings.getTokenIssuer());
        assertNotNull(jwtSettings.getTokenSigningKey());

        SystemInfo systemInfo = client.getSystemInfo();
        assertNotNull(systemInfo);

        // get features info
        FeaturesInfo featuresInfo = client.getFeaturesInfo();
        assertNotNull(featuresInfo);
        assertFalse(featuresInfo.getSmsEnabled());
        assertFalse(featuresInfo.getOauthEnabled());

        // check updates
        UpdateMessage updateMessage = client.checkUpdates();
        assertNotNull(updateMessage);
    }

    @Test
    public void testTenantAdminSettingsAccess() throws Exception {
        client.login(TENANT_ADMIN_USERNAME, TEST_PASSWORD);

        AdminSettings mailSettings = client.getAdminSettings(GetAdminSettingsArgs.builder()
                .key("mail")
                .systemByDefault(true)
                .build());
        assertNotNull(mailSettings);
        assertEquals("mail", mailSettings.getKey());
        assertNotNull(mailSettings.getJsonValue());

        AdminSettings generalSettings = client.getAdminSettings(GetAdminSettingsArgs.builder()
                .key("general")
                .systemByDefault(true)
                .build());
        assertNotNull(generalSettings);
        assertEquals("general", generalSettings.getKey());
        assertNotNull(generalSettings.getJsonValue());

        assertReturns403(() -> client.getJwtSettings());
        assertReturns403(() -> client.getSystemInfo());
        assertReturns403(() -> client.checkUpdates());
        assertReturns403(() -> client.getSecuritySettings());
    }

}
