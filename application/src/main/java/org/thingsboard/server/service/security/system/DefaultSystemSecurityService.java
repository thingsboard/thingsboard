/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.service.security.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.passay.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.exception.UserPasswordExpiredException;
import org.thingsboard.server.service.security.model.SecuritySettings;
import org.thingsboard.server.service.security.model.UserPasswordPolicy;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_CACHE;
import static org.thingsboard.server.common.data.CacheConstants.SECURITY_SETTINGS_CACHE;

@Service
@Slf4j
public class DefaultSystemSecurityService implements SystemSecurityService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private UserService userService;

    @Resource
    private SystemSecurityService self;

    @Cacheable(cacheNames = SECURITY_SETTINGS_CACHE, key = "'securitySettings'")
    @Override
    public SecuritySettings getSecuritySettings(TenantId tenantId) {
        SecuritySettings securitySettings = null;
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(tenantId, "securitySettings");
        if (adminSettings != null) {
            try {
                securitySettings = objectMapper.treeToValue(adminSettings.getJsonValue(), SecuritySettings.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load security settings!", e);
            }
        } else {
            securitySettings = new SecuritySettings();
            securitySettings.setPasswordPolicy(new UserPasswordPolicy());
            securitySettings.getPasswordPolicy().setMinimumLength(6);
        }
        return securitySettings;
    }

    @CacheEvict(cacheNames = SECURITY_SETTINGS_CACHE, key = "'securitySettings'")
    @Override
    public SecuritySettings saveSecuritySettings(TenantId tenantId, SecuritySettings securitySettings) {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(tenantId, "securitySettings");
        if (adminSettings == null) {
            adminSettings = new AdminSettings();
            adminSettings.setKey("securitySettings");
        }
        adminSettings.setJsonValue(objectMapper.valueToTree(securitySettings));
        AdminSettings savedAdminSettings = adminSettingsService.saveAdminSettings(tenantId, adminSettings);
        try {
            return objectMapper.treeToValue(savedAdminSettings.getJsonValue(), SecuritySettings.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load security settings!", e);
        }
    }

    @Override
    public void validateUserCredentials(TenantId tenantId, UserCredentials userCredentials, String password) throws AuthenticationException {

        if (!encoder.matches(password, userCredentials.getPassword())) {
            throw new BadCredentialsException("Authentication Failed. Username or Password not valid.");
        }

        if (!userCredentials.isEnabled()) {
            throw new DisabledException("User is not active");
        }

        SecuritySettings securitySettings = self.getSecuritySettings(tenantId);
        if (isPositiveInteger(securitySettings.getPasswordPolicy().getPasswordExpirationPeriodDays())) {
            if ((userCredentials.getCreatedTime()
                    + TimeUnit.DAYS.toMillis(securitySettings.getPasswordPolicy().getPasswordExpirationPeriodDays()))
                < System.currentTimeMillis()) {
                userCredentials = userService.requestExpiredPasswordReset(tenantId, userCredentials.getId());
                throw new UserPasswordExpiredException("User password expired!", userCredentials.getResetToken());
            }
        }
    }

    @Override
    public void validatePassword(TenantId tenantId, String password) throws DataValidationException {
        SecuritySettings securitySettings = self.getSecuritySettings(tenantId);
        UserPasswordPolicy passwordPolicy = securitySettings.getPasswordPolicy();

        List<Rule> passwordRules = new ArrayList<>();
        passwordRules.add(new LengthRule(passwordPolicy.getMinimumLength(), Integer.MAX_VALUE));
        if (isPositiveInteger(passwordPolicy.getMinimumUppercaseLetters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.UpperCase, passwordPolicy.getMinimumUppercaseLetters()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumLowercaseLetters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.LowerCase, passwordPolicy.getMinimumLowercaseLetters()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumDigits())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.Digit, passwordPolicy.getMinimumDigits()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumSpecialCharacters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.Special, passwordPolicy.getMinimumSpecialCharacters()));
        }
        PasswordValidator validator = new PasswordValidator(passwordRules);
        PasswordData passwordData = new PasswordData(password);
        RuleResult result = validator.validate(passwordData);
        if (!result.isValid()) {
            String message = String.join("\n", validator.getMessages(result));
            throw new DataValidationException(message);
        }
    }

    private static boolean isPositiveInteger(Integer val) {
        return val != null && val.intValue() > 0;
    }
}
