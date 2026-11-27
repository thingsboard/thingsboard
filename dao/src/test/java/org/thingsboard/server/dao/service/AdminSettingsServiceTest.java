// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.exception.DataValidationException;

import static org.assertj.core.api.Assertions.assertThat;
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
        String key = RandomStringUtils.secure().nextAlphanumeric(15);
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

    @Test
    public void whenSavingSystemMailSettingsWithTenantId_thenRefreshTokenIsNotDropped() {
        AdminSettings system = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
        JsonNode originalSystemJson = system.getJsonValue().deepCopy();
        try {
            // System mail: OAuth2 / Office 365. The first save sets the provider fields
            // (the token is dropped because they changed), the second adds the token.
            ObjectNode systemJson = (ObjectNode) system.getJsonValue();
            systemJson.put("enableOauth2", true);
            systemJson.put("providerId", "OFFICE_365");
            systemJson.put("clientId", "system-client");
            systemJson.put("clientSecret", "system-secret");
            systemJson.put("redirectUri", "https://system.example/redirect");
            systemJson.put("providerTenantId", "system-azure-tenant");
            adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, system);

            system = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
            systemJson = (ObjectNode) system.getJsonValue();
            systemJson.put("refreshToken", "system-refresh-token");
            systemJson.put("refreshTokenExpires", System.currentTimeMillis() + 86_400_000L);
            systemJson.put("tokenGenerated", true);
            adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, system);

            // Tenant mail: uses system settings, but its OAuth2 fields differ
            AdminSettings tenantMail = new AdminSettings();
            tenantMail.setTenantId(tenantId);
            tenantMail.setKey("mail");
            tenantMail.setJsonValue(JacksonUtil.newObjectNode()
                    .put("useSystemMailSettings", true)
                    .put("enableOauth2", true)
                    .put("providerId", "OFFICE_365")
                    .put("clientId", "tenant-client")
                    .put("clientSecret", "tenant-secret")
                    .put("redirectUri", "https://tenant.example/redirect")
                    .put("providerTenantId", "tenant-azure-tenant"));
            adminSettingsService.saveAdminSettings(tenantId, tenantMail);

            // What TbMailSender.refreshAccessToken did before the fix:
            // saving the SYSTEM row with the TENANT's id
            AdminSettings systemToRefresh = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
            ((ObjectNode) systemToRefresh.getJsonValue()).put("refreshToken", "rotated-refresh-token");
            adminSettingsService.saveAdminSettings(tenantId, systemToRefresh);

            JsonNode after = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail").getJsonValue();
            assertThat(after.get("refreshToken").asText()).isEqualTo("rotated-refresh-token");
            assertThat(after.get("tokenGenerated").asBoolean()).isTrue();
            assertThat(after.has("refreshTokenExpires")).isTrue();
        } finally {
            adminSettingsService.deleteAdminSettingsByTenantIdAndKey(tenantId, "mail");
            AdminSettings restore = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
            restore.setJsonValue(originalSystemJson);
            adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, restore);
        }
    }
    
    @Test
    public void whenUpdatingSettingsOfAnotherTenantById_thenSaveIsRejectedAndStoredSettingsUnchanged() {
        AdminSettings systemSettings = saveSettings(SYSTEM_TENANT_ID, 1);
        AdminSettings systemSettingsCopy = new AdminSettings(systemSettings);
        systemSettingsCopy.setTenantId(tenantId);
        systemSettingsCopy.setJsonValue(JacksonUtil.newObjectNode().put("v", 2));
        assertThatThrownBy(() -> adminSettingsService.saveAdminSettings(tenantId, systemSettingsCopy))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("different tenant");
        AdminSettings storedSystemSettings = adminSettingsService.findAdminSettingsById(SYSTEM_TENANT_ID, systemSettings.getId());
        assertThat(storedSystemSettings.getTenantId()).isEqualTo(SYSTEM_TENANT_ID);
        assertThat(storedSystemSettings.getJsonValue()).isEqualTo(systemSettings.getJsonValue());

        AdminSettings tenantSettings = saveSettings(tenantId, 1);
        AdminSettings tenantSettingsCopy = new AdminSettings(tenantSettings);
        tenantSettingsCopy.setTenantId(SYSTEM_TENANT_ID);
        tenantSettingsCopy.setJsonValue(JacksonUtil.newObjectNode().put("v", 2));
        assertThatThrownBy(() -> adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, tenantSettingsCopy))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("different tenant");
        AdminSettings storedTenantSettings = adminSettingsService.findAdminSettingsById(tenantId, tenantSettings.getId());
        assertThat(storedTenantSettings.getTenantId()).isEqualTo(tenantId);
        assertThat(storedTenantSettings.getJsonValue()).isEqualTo(tenantSettings.getJsonValue());
    }

    @Test
    public void whenUpdatingOwnSettingsById_thenSaved() {
        AdminSettings tenantSettings = saveSettings(tenantId, 1);
        tenantSettings.setJsonValue(JacksonUtil.newObjectNode().put("v", 2));
        adminSettingsService.saveAdminSettings(tenantId, tenantSettings);
        AdminSettings storedTenantSettings = adminSettingsService.findAdminSettingsById(tenantId, tenantSettings.getId());
        assertThat(storedTenantSettings.getTenantId()).isEqualTo(tenantId);
        assertThat(storedTenantSettings.getJsonValue()).isEqualTo(tenantSettings.getJsonValue());

        AdminSettings systemSettings = saveSettings(SYSTEM_TENANT_ID, 1);
        systemSettings.setTenantId(null);
        systemSettings.setJsonValue(JacksonUtil.newObjectNode().put("v", 2));
        adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, systemSettings);
        AdminSettings storedSystemSettings = adminSettingsService.findAdminSettingsById(SYSTEM_TENANT_ID, systemSettings.getId());
        assertThat(storedSystemSettings.getTenantId()).isEqualTo(SYSTEM_TENANT_ID);
        assertThat(storedSystemSettings.getJsonValue()).isEqualTo(systemSettings.getJsonValue());

        storedSystemSettings.setJsonValue(JacksonUtil.newObjectNode().put("v", 3));
        adminSettingsService.saveAdminSettings(tenantId, storedSystemSettings);
        AdminSettings updatedSystemSettings = adminSettingsService.findAdminSettingsById(SYSTEM_TENANT_ID, systemSettings.getId());
        assertThat(updatedSystemSettings.getTenantId()).isEqualTo(SYSTEM_TENANT_ID);
        assertThat(updatedSystemSettings.getJsonValue()).isEqualTo(storedSystemSettings.getJsonValue());
    }

    private AdminSettings saveSettings(TenantId tenantId, int value) {
        AdminSettings settings = new AdminSettings();
        settings.setTenantId(tenantId);
        settings.setKey(RandomStringUtils.randomAlphanumeric(15));
        settings.setJsonValue(JacksonUtil.newObjectNode().put("v", value));
        return adminSettingsService.saveAdminSettings(tenantId, settings);
    }

    @Test
    public void testFindAllByTenantId() {
        int pageSize = 10;
        int totalElements = 100;

        for (int i = 0; i < totalElements; i++) {
            AdminSettings settings = new AdminSettings();
            settings.setTenantId(tenantId);
            String key = RandomStringUtils.secure().nextAlphanumeric(15);
            settings.setKey(key);
            settings.setJsonValue(JacksonUtil.newObjectNode().put("value", i));
            adminSettingsService.saveAdminSettings(tenantId, settings);
        }

        PageData<AdminSettings> pageData = adminSettingsService.findAllByTenantId(tenantId, new PageLink(pageSize));
        assertThat(pageData.getData().size()).isEqualTo(pageSize);
        assertThat(pageData.getTotalElements()).isEqualTo(totalElements);

    }

}
