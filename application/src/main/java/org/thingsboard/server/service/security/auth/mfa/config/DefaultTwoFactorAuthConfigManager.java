/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth.mfa.config;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.security.UserAuthSettings;
import org.thingsboard.server.common.data.security.model.mfa.TwoFactorAuthSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFactorAuthAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFactorAuthProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFactorAuthProviderType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserAuthSettingsDao;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class DefaultTwoFactorAuthConfigManager implements TwoFactorAuthConfigManager {

    private final UserAuthSettingsDao userAuthSettingsDao;
    private final AdminSettingsService adminSettingsService;
    private final AdminSettingsDao adminSettingsDao;
    private final AttributesService attributesService;

    protected static final String TWO_FACTOR_AUTH_SETTINGS_KEY = "twoFaSettings";


    @Override
    public boolean isTwoFaEnabled(TenantId tenantId, UserId userId) {
        return getTwoFaAccountConfig(tenantId, userId).isPresent();
    }

    @Override
    public Optional<TwoFactorAuthAccountConfig> getTwoFaAccountConfig(TenantId tenantId, UserId userId) {
        return Optional.ofNullable(userAuthSettingsDao.findByUserId(userId))
                .flatMap(userAuthSettings -> Optional.ofNullable(userAuthSettings.getTwoFaAccountConfig()))
                .filter(twoFaAccountConfig -> getTwoFaProviderConfig(tenantId, twoFaAccountConfig.getProviderType()).isPresent());
    }

    @Override
    public void saveTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFactorAuthAccountConfig accountConfig) throws ThingsboardException {
        getTwoFaProviderConfig(tenantId, accountConfig.getProviderType())
                .orElseThrow(() -> new ThingsboardException("2FA provider is not configured", ThingsboardErrorCode.BAD_REQUEST_PARAMS));

        UserAuthSettings userAuthSettings = Optional.ofNullable(userAuthSettingsDao.findByUserId(userId))
                .orElseGet(() -> {
                    UserAuthSettings newUserAuthSettings = new UserAuthSettings();
                    newUserAuthSettings.setUserId(userId);
                    return newUserAuthSettings;
                });
        userAuthSettings.setTwoFaAccountConfig(accountConfig);
        userAuthSettingsDao.save(tenantId, userAuthSettings);
    }

    @Override
    public void deleteTwoFaAccountConfig(TenantId tenantId, UserId userId) {
        Optional.ofNullable(userAuthSettingsDao.findByUserId(userId))
                .ifPresent(userAuthSettings -> {
                    userAuthSettings.setTwoFaAccountConfig(null);
                    userAuthSettingsDao.save(tenantId, userAuthSettings);
                });
    }


    private Optional<TwoFactorAuthProviderConfig> getTwoFaProviderConfig(TenantId tenantId, TwoFactorAuthProviderType providerType) {
        return getTwoFaSettings(tenantId, true)
                .flatMap(twoFaSettings -> twoFaSettings.getProviderConfig(providerType));
    }

    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    @Override
    public Optional<TwoFactorAuthSettings> getTwoFaSettings(TenantId tenantId, boolean sysadminSettingsAsDefault) {
        if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
            return Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, TWO_FACTOR_AUTH_SETTINGS_KEY))
                    .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), TwoFactorAuthSettings.class));
        } else {
            Optional<TwoFactorAuthSettings> tenantTwoFaSettings = attributesService.find(TenantId.SYS_TENANT_ID, tenantId,
                    DataConstants.SERVER_SCOPE, TWO_FACTOR_AUTH_SETTINGS_KEY).get()
                    .map(adminSettingsAttribute -> JacksonUtil.fromString(adminSettingsAttribute.getJsonValue().get(), TwoFactorAuthSettings.class));
            if (sysadminSettingsAsDefault) {
                if (tenantTwoFaSettings.isEmpty() || tenantTwoFaSettings.get().isUseSystemTwoFactorAuthSettings()) {
                    return getTwoFaSettings(TenantId.SYS_TENANT_ID, false);
                }
            }
            return tenantTwoFaSettings;
        }
    }

    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    @Override
    public void saveTwoFaSettings(TenantId tenantId, TwoFactorAuthSettings twoFactorAuthSettings) {
        if (tenantId.equals(TenantId.SYS_TENANT_ID) || !twoFactorAuthSettings.isUseSystemTwoFactorAuthSettings()) {
            ConstraintValidator.validateFields(twoFactorAuthSettings);
        }
        if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
            AdminSettings settings = Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(tenantId, TWO_FACTOR_AUTH_SETTINGS_KEY))
                    .orElseGet(() -> {
                        AdminSettings newSettings = new AdminSettings();
                        newSettings.setKey(TWO_FACTOR_AUTH_SETTINGS_KEY);
                        return newSettings;
                    });
            settings.setJsonValue(JacksonUtil.valueToTree(twoFactorAuthSettings));
            adminSettingsService.saveAdminSettings(tenantId, settings);
        } else {
            attributesService.save(TenantId.SYS_TENANT_ID, tenantId, DataConstants.SERVER_SCOPE, Collections.singletonList(
                    new BaseAttributeKvEntry(new JsonDataEntry(TWO_FACTOR_AUTH_SETTINGS_KEY, JacksonUtil.toString(twoFactorAuthSettings)), System.currentTimeMillis())
            )).get();
        }
    }

    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    @Override
    public void deleteTwoFaSettings(TenantId tenantId) {
        if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
            Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(tenantId, TWO_FACTOR_AUTH_SETTINGS_KEY))
                    .ifPresent(adminSettings -> adminSettingsDao.removeById(tenantId, adminSettings.getId().getId()));
        } else {
            attributesService.removeAll(TenantId.SYS_TENANT_ID, tenantId, DataConstants.SERVER_SCOPE,
                    Collections.singletonList(TWO_FACTOR_AUTH_SETTINGS_KEY)).get();
        }
    }

}
