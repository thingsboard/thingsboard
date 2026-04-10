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
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
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
        // authenticate as sysadmin for admin settings management
        client.login("sysadmin@thingsboard.org", "sysadmin");

        // get mail settings
        AdminSettings mailSettings = client.getAdminSettings("mail");
        assertNotNull(mailSettings);
        assertNotNull(mailSettings.getKey());
        assertEquals("mail", mailSettings.getKey());
        assertNotNull(mailSettings.getJsonValue());

        // get general settings
        AdminSettings generalSettings = client.getAdminSettings("general");
        assertNotNull(generalSettings);
        assertEquals("general", generalSettings.getKey());
        assertNotNull(generalSettings.getJsonValue());
        assertNotNull(generalSettings.getJsonValue().get("baseUrl").asText());

        // update general settings and restore
        ((ObjectNode) generalSettings.getJsonValue()).put("prohibitDifferentUrl", true);
        AdminSettings updatedGeneralSettings = client.saveAdminSettings(generalSettings);
        assertTrue(updatedGeneralSettings.getJsonValue().get("prohibitDifferentUrl").asBoolean());

        // get security settings
        SecuritySettings securitySettings = client.getSecuritySettings();
        assertNotNull(securitySettings);
        assertNotNull(securitySettings.getPasswordPolicy());
        Integer originalMaxAttempts = securitySettings.getMaxFailedLoginAttempts();

        // update security settings
        securitySettings.setMaxFailedLoginAttempts(10);
        SecuritySettings updatedSecurity = client.saveSecuritySettings(securitySettings);
        assertNotNull(updatedSecurity);
        assertEquals(10, updatedSecurity.getMaxFailedLoginAttempts().intValue());

        // restore original security settings
        updatedSecurity.setMaxFailedLoginAttempts(originalMaxAttempts);
        client.saveSecuritySettings(updatedSecurity);

        // get JWT settings
        JwtSettings jwtSettings = client.getJwtSettings();
        assertNotNull(jwtSettings);
        assertNotNull(jwtSettings.getTokenExpirationTime());
        assertNotNull(jwtSettings.getRefreshTokenExpTime());
        assertEquals("thingsboard.io", jwtSettings.getTokenIssuer());
        assertNotNull(jwtSettings.getTokenSigningKey());

        // get system info
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

}
