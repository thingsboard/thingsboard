/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.settings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;

@Service
@Slf4j
public class AdminSettingsServiceImpl implements AdminSettingsService {
    
    @Autowired
    private AdminSettingsDao adminSettingsDao;

    @Override
    public AdminSettings findAdminSettingsById(TenantId tenantId, AdminSettingsId adminSettingsId) {
        log.trace("Executing findAdminSettingsById [{}]", adminSettingsId);
        Validator.validateId(adminSettingsId, "Incorrect adminSettingsId " + adminSettingsId);
        return  adminSettingsDao.findById(tenantId, adminSettingsId.getId());
    }

    @Override
    public AdminSettings findAdminSettingsByKey(TenantId tenantId, String key) {
        log.trace("Executing findAdminSettingsByKey [{}]", key);
        Validator.validateString(key, "Incorrect key " + key);
        return adminSettingsDao.findByKey(tenantId, key);
    }

    @Override
    public void deleteAdminSettingsByKey(TenantId tenantId, String key) {
        log.trace("Executing deleteAdminSettingsByKey [{}]", key);
        AdminSettings adminSettings = findAdminSettingsByKey(tenantId, key);
        if (adminSettings != null) {
            adminSettingsDao.removeById(tenantId, adminSettings.getId().getId());
        }
    }

    @Override
    public AdminSettings saveAdminSettings(TenantId tenantId, AdminSettings adminSettings) {
        log.trace("Executing saveAdminSettings [{}]", adminSettings);
        adminSettingsValidator.validate(adminSettings, data -> tenantId);
        if (adminSettings.getKey().equals("mail") && "".equals(adminSettings.getJsonValue().get("password").asText())) {
            AdminSettings mailSettings = findAdminSettingsByKey(tenantId, "mail");
            if (mailSettings != null) {
                ((ObjectNode) adminSettings.getJsonValue()).put("password", mailSettings.getJsonValue().get("password").asText());
            }
        }

        return adminSettingsDao.save(tenantId, adminSettings);
    }
    
    private DataValidator<AdminSettings> adminSettingsValidator =
            new DataValidator<AdminSettings>() {

                @Override
                protected void validateCreate(TenantId tenantId, AdminSettings adminSettings) {
                    AdminSettings existentAdminSettingsWithKey = findAdminSettingsByKey(tenantId, adminSettings.getKey());
                    if (existentAdminSettingsWithKey != null) {
                        throw new DataValidationException("Admin settings with such name already exists!");
                    }
                }

                @Override
                protected void validateUpdate(TenantId tenantId, AdminSettings adminSettings) {
                    AdminSettings existentAdminSettings = findAdminSettingsById(tenantId, adminSettings.getId());
                    if (existentAdminSettings != null) {
                        if (!existentAdminSettings.getKey().equals(adminSettings.getKey())) {
                            throw new DataValidationException("Changing key of admin settings entry is prohibited!");
                        }
                    }
                }

        
                @Override
                protected void validateDataImpl(TenantId tenantId, AdminSettings adminSettings) {
                    if (StringUtils.isEmpty(adminSettings.getKey())) {
                        throw new DataValidationException("Key should be specified!");
                    }
                    if (adminSettings.getJsonValue() == null) {
                        throw new DataValidationException("Json value should be specified!");
                    }
                }
    };

}
