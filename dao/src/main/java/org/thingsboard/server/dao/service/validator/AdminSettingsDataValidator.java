// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.exception.DataValidationException;

@Component
@AllArgsConstructor
public class AdminSettingsDataValidator extends DataValidator<AdminSettings> {

    private final AdminSettingsService adminSettingsService;

    @Override
    protected void validateCreate(TenantId tenantId, AdminSettings adminSettings) {
        AdminSettings existingSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, adminSettings.getKey());
        if (existingSettings != null) {
            throw new DataValidationException("Admin settings with such name already exists!");
        }
    }

    @Override
    protected AdminSettings validateUpdate(TenantId tenantId, AdminSettings adminSettings) {
        AdminSettings existentAdminSettings = adminSettingsService.findAdminSettingsById(tenantId, adminSettings.getId());
        if (existentAdminSettings != null) {
            if (!existentAdminSettings.getKey().equals(adminSettings.getKey())) {
                throw new DataValidationException("Changing key of admin settings entry is prohibited!");
            }
        }
        return existentAdminSettings;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AdminSettings adminSettings) {
        validateString("Key", adminSettings.getKey());
        if (adminSettings.getJsonValue() == null) {
            throw new DataValidationException("Json value should be specified!");
        }
        if ("general".equals(adminSettings.getKey()) && adminSettings.getJsonValue().has("baseUrl")
                && !DataValidator.isValidUrl(adminSettings.getJsonValue().get("baseUrl").asText())) {
            throw new DataValidationException("Base url is invalid!");
        }
    }
}
