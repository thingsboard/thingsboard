// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AdminSettingsDataValidator.class)
class AdminSettingsDataValidatorTest {

    @MockitoBean
    AdminSettingsService adminSettingsService;
    @MockitoSpyBean
    AdminSettingsDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @Test
    void testValidateNameInvocation() {
        AdminSettings adminSettings = new AdminSettings();
        adminSettings.setKey("jwt");
        adminSettings.setJsonValue(JacksonUtil.toJsonNode("{}"));

        validator.validateDataImpl(tenantId, adminSettings);
        verify(validator).validateString("Key", adminSettings.getKey());
    }

    @Test
    void whenStoredSettingsBelongToAnotherTenant_thenUpdateIsRejected() {
        AdminSettings stored = settings(TenantId.SYS_TENANT_ID);
        when(adminSettingsService.findAdminSettingsById(any(), eq(stored.getId()))).thenReturn(stored);

        AdminSettings update = new AdminSettings(stored);
        update.setTenantId(tenantId);

        assertThatThrownBy(() -> validator.validateUpdate(tenantId, update))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("different tenant");
    }

    @Test
    void whenStoredSettingsBelongToSameTenant_thenStoredSettingsReturned() {
        AdminSettings stored = settings(tenantId);
        when(adminSettingsService.findAdminSettingsById(any(), eq(stored.getId()))).thenReturn(stored);
        assertThat(validator.validateUpdate(TenantId.SYS_TENANT_ID, new AdminSettings(stored))).isEqualTo(stored);

        AdminSettings storedSystemSettings = settings(TenantId.SYS_TENANT_ID);
        when(adminSettingsService.findAdminSettingsById(any(), eq(storedSystemSettings.getId()))).thenReturn(storedSystemSettings);
        AdminSettings update = new AdminSettings(storedSystemSettings);
        update.setTenantId(null);
        assertThat(validator.validateUpdate(TenantId.SYS_TENANT_ID, update)).isEqualTo(storedSystemSettings);
    }

    private AdminSettings settings(TenantId tenantId) {
        AdminSettings settings = new AdminSettings(new AdminSettingsId(UUID.randomUUID()));
        settings.setTenantId(tenantId);
        settings.setKey("general");
        settings.setJsonValue(JacksonUtil.newObjectNode().put("v", 1));
        return settings;
    }

}
