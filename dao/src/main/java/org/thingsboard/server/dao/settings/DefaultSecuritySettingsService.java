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
package org.thingsboard.server.dao.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.ConstraintValidator;

import static org.thingsboard.server.common.data.CacheConstants.SECURITY_SETTINGS_CACHE;

@Service
@RequiredArgsConstructor
public class DefaultSecuritySettingsService implements SecuritySettingsService {

    private final AdminSettingsService adminSettingsService;

    @Value("${security.user_activation_link_max_ttl:720}")
    private int maxActivationLinkTtl;

    public static final int DEFAULT_MOBILE_SECRET_KEY_LENGTH = 64;

    @Cacheable(cacheNames = SECURITY_SETTINGS_CACHE, key = "'securitySettings'")
    @Override
    public SecuritySettings getSecuritySettings() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "securitySettings");
        SecuritySettings securitySettings;
        if (adminSettings != null) {
            try {
                securitySettings = JacksonUtil.convertValue(adminSettings.getJsonValue(), SecuritySettings.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load security settings!", e);
            }
        } else {
            securitySettings = new SecuritySettings();
            securitySettings.setPasswordPolicy(new UserPasswordPolicy());
            securitySettings.getPasswordPolicy().setMinimumLength(6);
            securitySettings.getPasswordPolicy().setMaximumLength(72);
            securitySettings.setMobileSecretKeyLength(DEFAULT_MOBILE_SECRET_KEY_LENGTH);
            securitySettings.setPasswordResetTokenTtl(24);
            securitySettings.setUserActivationTokenTtl(24);
        }
        securitySettings.setMaxActivationLinkTtl(maxActivationLinkTtl);
        return securitySettings;
    }

    @CacheEvict(cacheNames = SECURITY_SETTINGS_CACHE, key = "'securitySettings'")
    @Override
    public SecuritySettings saveSecuritySettings(SecuritySettings securitySettings) {
        ConstraintValidator.validateFields(securitySettings);
        if (securitySettings.getUserActivationTokenTtl() > maxActivationLinkTtl) {
            throw new DataValidationException("User activation link TTL must not exceed " + maxActivationLinkTtl + " hours");
        }
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "securitySettings");
        if (adminSettings == null) {
            adminSettings = new AdminSettings();
            adminSettings.setTenantId(TenantId.SYS_TENANT_ID);
            adminSettings.setKey("securitySettings");
        }
        adminSettings.setJsonValue(JacksonUtil.valueToTree(securitySettings));
        AdminSettings savedAdminSettings = adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings);
        try {
            SecuritySettings savedSecuritySettings = JacksonUtil.convertValue(savedAdminSettings.getJsonValue(), SecuritySettings.class);
            savedSecuritySettings.setMaxActivationLinkTtl(maxActivationLinkTtl);
            return savedSecuritySettings;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load security settings!", e);
        }
    }

}
