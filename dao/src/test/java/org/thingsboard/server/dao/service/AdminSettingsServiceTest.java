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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DaoSqlTest
public class AdminSettingsServiceTest extends AbstractServiceTest {

    @Autowired
    AdminSettingsService adminSettingsService;

    @Test
    public void testFindAdminSettingsByKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        Assert.assertNotNull(adminSettings);
        adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
        Assert.assertNotNull(adminSettings);
        adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "unknown");
        Assert.assertNull(adminSettings);
    }

    @Test
    public void testFindAdminSettingsById() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        AdminSettings foundAdminSettings = adminSettingsService.findAdminSettingsById(SYSTEM_TENANT_ID, adminSettings.getId());
        Assert.assertNotNull(foundAdminSettings);
        Assert.assertEquals(adminSettings, foundAdminSettings);
    }

    @Test
    public void testSaveAdminSettings() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        JsonNode json = adminSettings.getJsonValue();
        ((ObjectNode) json).put("baseUrl", "http://myhost.org");
        adminSettings.setJsonValue(json);
        adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, adminSettings);
        AdminSettings savedAdminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        Assert.assertNotNull(savedAdminSettings);
        Assert.assertEquals(adminSettings.getJsonValue(), savedAdminSettings.getJsonValue());
    }

    @Test
    public void testSaveAdminSettingsWithEmptyKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
        adminSettings.setKey(null);
        Assertions.assertThrows(DataValidationException.class, () -> {
            adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, adminSettings);
        });
    }

    @Test
    public void testChangeAdminSettingsKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
        adminSettings.setKey("newKey");
        Assertions.assertThrows(DataValidationException.class, () -> {
            adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, adminSettings);
        });
    }

    @Test
    public void whenSavingAdminSettingsWithAlreadyExistingKey_thenReturnError() {
        String key = RandomStringUtils.randomAlphanumeric(15);
        ObjectNode value = JacksonUtil.newObjectNode().put("test", "test");

        AdminSettings systemSettings = new AdminSettings();
        systemSettings.setTenantId(TenantId.SYS_TENANT_ID);
        systemSettings.setKey(key);
        systemSettings.setJsonValue(value);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, systemSettings);

        assertThatThrownBy(() -> {
            adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, systemSettings);
        }).hasMessageContaining("already exists");

        AdminSettings tenantSettings = new AdminSettings();
        tenantSettings.setTenantId(tenantId);
        tenantSettings.setKey(key);
        tenantSettings.setJsonValue(value);
        assertDoesNotThrow(() -> {
            adminSettingsService.saveAdminSettings(tenantId, tenantSettings);
        });

        assertThatThrownBy(() -> {
            adminSettingsService.saveAdminSettings(tenantId, tenantSettings);
        }).hasMessageContaining("already exists");
    }

}
