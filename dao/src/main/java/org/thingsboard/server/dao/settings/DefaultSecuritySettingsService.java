// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.exception.DataValidationException;

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
            return JacksonUtil.convertValue(savedAdminSettings.getJsonValue(), SecuritySettings.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load security settings!", e);
        }
    }

}
