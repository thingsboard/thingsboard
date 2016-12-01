/**
 * Copyright © 2016 The Thingsboard Authors
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

import static org.thingsboard.server.dao.DaoUtil.getData;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.AdminSettingsEntity;
import org.thingsboard.server.dao.service.DataValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.service.Validator;

@Service
@Slf4j
public class AdminSettingsServiceImpl implements AdminSettingsService {
    
    @Autowired
    private AdminSettingsDao adminSettingsDao;

    @Override
    public AdminSettings findAdminSettingsById(AdminSettingsId adminSettingsId) {
        log.trace("Executing findAdminSettingsById [{}]", adminSettingsId);
        Validator.validateId(adminSettingsId, "Incorrect adminSettingsId " + adminSettingsId);
        AdminSettingsEntity adminSettingsEntity = adminSettingsDao.findById(adminSettingsId.getId());
        return getData(adminSettingsEntity);
    }

    @Override
    public AdminSettings findAdminSettingsByKey(String key) {
        log.trace("Executing findAdminSettingsByKey [{}]", key);
        Validator.validateString(key, "Incorrect key " + key);
        AdminSettingsEntity adminSettingsEntity = adminSettingsDao.findByKey(key);
        return getData(adminSettingsEntity);
    }

    @Override
    public AdminSettings saveAdminSettings(AdminSettings adminSettings) {
        log.trace("Executing saveAdminSettings [{}]", adminSettings);
        adminSettingsValidator.validate(adminSettings);
        AdminSettingsEntity adminSettingsEntity = adminSettingsDao.save(adminSettings);
        return getData(adminSettingsEntity);
    }
    
    private DataValidator<AdminSettings> adminSettingsValidator =
            new DataValidator<AdminSettings>() {
        
                @Override
                protected void validateCreate(AdminSettings adminSettings) {
                    throw new DataValidationException("Creation of new admin settings entry is prohibited!");
                }
        
                @Override
                protected void validateDataImpl(AdminSettings adminSettings) {
                    if (StringUtils.isEmpty(adminSettings.getKey())) {
                        throw new DataValidationException("Key should be specified!");
                    }
                    if (adminSettings.getJsonValue() == null) {
                        throw new DataValidationException("Json value should be specified!");
                    }
                    AdminSettings existentAdminSettingsWithKey = findAdminSettingsByKey(adminSettings.getKey());
                    if (existentAdminSettingsWithKey == null || !isSameData(existentAdminSettingsWithKey, adminSettings)) {
                        throw new DataValidationException("Changing key of admin settings entry is prohibited!");
                    }
                    validateJsonStructure(existentAdminSettingsWithKey.getJsonValue(), adminSettings.getJsonValue());
                }
    };

}
