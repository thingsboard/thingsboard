// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.UUID;

import static org.mockito.Mockito.verify;

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

}