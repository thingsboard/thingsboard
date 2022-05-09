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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.security.UserAuthSettings;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.AccountTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserAuthSettingsDao;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class DefaultTwoFaConfigManager implements TwoFaConfigManager {

    private final UserAuthSettingsDao userAuthSettingsDao;
    private final AdminSettingsService adminSettingsService;
    private final AdminSettingsDao adminSettingsDao;
    private final AttributesService attributesService;

    protected static final String TWO_FACTOR_AUTH_SETTINGS_KEY = "twoFaSettings";


    @Override
    public Optional<AccountTwoFaSettings> getAccountTwoFaSettings(TenantId tenantId, UserId userId) {
        return Optional.ofNullable(userAuthSettingsDao.findByUserId(userId))
                .flatMap(userAuthSettings -> Optional.ofNullable(userAuthSettings.getTwoFaSettings()))
                .map(twoFaSettings -> {
                    twoFaSettings.getConfigs().keySet().removeIf(providerType -> {
                        return getTwoFaProviderConfig(tenantId, providerType).isEmpty();
                    });
                    return twoFaSettings;
                });
    }

    protected AccountTwoFaSettings saveAccountTwoFaSettings(TenantId tenantId, UserId userId, AccountTwoFaSettings settings) {
        UserAuthSettings userAuthSettings = Optional.ofNullable(userAuthSettingsDao.findByUserId(userId))
                .orElseGet(() -> {
                    UserAuthSettings newUserAuthSettings = new UserAuthSettings();
                    newUserAuthSettings.setUserId(userId);
                    return newUserAuthSettings;
                });
        userAuthSettings.setTwoFaSettings(settings);
        userAuthSettingsDao.save(tenantId, userAuthSettings);
        return settings;
    }


    @Override
    public Optional<TwoFaAccountConfig> getTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaProviderType providerType) {
        return getAccountTwoFaSettings(tenantId, userId)
                .map(AccountTwoFaSettings::getConfigs)
                .flatMap(configs -> Optional.ofNullable(configs.get(providerType)));
    }

    @Override
    public AccountTwoFaSettings saveTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaAccountConfig accountConfig) {
        getTwoFaProviderConfig(tenantId, accountConfig.getProviderType())
                .orElseThrow(() -> new IllegalArgumentException("2FA provider is not configured"));

        AccountTwoFaSettings settings = getAccountTwoFaSettings(tenantId, userId).orElseGet(() -> {
            AccountTwoFaSettings newSettings = new AccountTwoFaSettings();
            newSettings.setConfigs(new LinkedHashMap<>());
            return newSettings;
        });
        if (accountConfig.isUseByDefault()) {
            settings.getConfigs().values().forEach(config -> config.setUseByDefault(false));
        }
        settings.getConfigs().put(accountConfig.getProviderType(), accountConfig);
        if (settings.getConfigs().values().stream().noneMatch(TwoFaAccountConfig::isUseByDefault)) {
            settings.getConfigs().values().stream().findFirst().ifPresent(config -> config.setUseByDefault(true));
        }
        return saveAccountTwoFaSettings(tenantId, userId, settings);
    }

    @Override
    public AccountTwoFaSettings deleteTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaProviderType providerType) {
        AccountTwoFaSettings settings = getAccountTwoFaSettings(tenantId, userId)
                .orElseThrow(() -> new IllegalArgumentException("2FA not configured"));
        settings.getConfigs().remove(providerType);
        if (!settings.getConfigs().isEmpty() && settings.getConfigs().values().stream().noneMatch(TwoFaAccountConfig::isUseByDefault)) {
            settings.getConfigs().values().stream()
                    .min(Comparator.comparing(TwoFaAccountConfig::getProviderType))
                    .ifPresent(config -> config.setUseByDefault(true));
        }
        return saveAccountTwoFaSettings(tenantId, userId, settings);
    }


    private Optional<TwoFaProviderConfig> getTwoFaProviderConfig(TenantId tenantId, TwoFaProviderType providerType) {
        return getPlatformTwoFaSettings(tenantId, true)
                .flatMap(twoFaSettings -> twoFaSettings.getProviderConfig(providerType));
    }

    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    @Override
    public Optional<PlatformTwoFaSettings> getPlatformTwoFaSettings(TenantId tenantId, boolean sysadminSettingsAsDefault) {
        if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
            return Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, TWO_FACTOR_AUTH_SETTINGS_KEY))
                    .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), PlatformTwoFaSettings.class));
        } else {
            Optional<PlatformTwoFaSettings> tenantTwoFaSettings = attributesService.find(TenantId.SYS_TENANT_ID, tenantId,
                            DataConstants.SERVER_SCOPE, TWO_FACTOR_AUTH_SETTINGS_KEY).get()
                    .map(adminSettingsAttribute -> JacksonUtil.fromString(adminSettingsAttribute.getJsonValue().get(), PlatformTwoFaSettings.class));
            if (sysadminSettingsAsDefault) {
                if (tenantTwoFaSettings.isEmpty() || tenantTwoFaSettings.get().isUseSystemTwoFactorAuthSettings()) {
                    return getPlatformTwoFaSettings(TenantId.SYS_TENANT_ID, false);
                }
            }
            return tenantTwoFaSettings;
        }
    }

    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    @Override
    public void savePlatformTwoFaSettings(TenantId tenantId, PlatformTwoFaSettings twoFactorAuthSettings) {
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
    public void deletePlatformTwoFaSettings(TenantId tenantId) {
        if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
            Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(tenantId, TWO_FACTOR_AUTH_SETTINGS_KEY))
                    .ifPresent(adminSettings -> adminSettingsDao.removeById(tenantId, adminSettings.getId().getId()));
        } else {
            attributesService.removeAll(TenantId.SYS_TENANT_ID, tenantId, DataConstants.SERVER_SCOPE,
                    Collections.singletonList(TWO_FACTOR_AUTH_SETTINGS_KEY)).get();
        }
    }

}
