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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.mfa.config.account.TwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.config.provider.TwoFactorAuthProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class DefaultTwoFactorAuthConfigManager implements TwoFactorAuthConfigManager {

    private final UserService userService;
    private final AdminSettingsService adminSettingsService;
    private final AttributesService attributesService;

    protected static final String TWO_FACTOR_AUTH_ACCOUNT_CONFIG_KEY = "twoFaConfig";
    protected static final String TWO_FACTOR_AUTH_SETTINGS_KEY = "twoFaSettings";


    @Override
    public boolean isTwoFaEnabled(User user) {
        return getTwoFaAccountConfig(user.getTenantId(), user.getId()).isPresent();
    }

    @Override
    public Optional<TwoFactorAuthAccountConfig> getTwoFaAccountConfig(TenantId tenantId, UserId userId) {
        User user = userService.findUserById(tenantId, userId);
        return Optional.ofNullable(user.getAdditionalInfo())
                .flatMap(additionalInfo -> Optional.ofNullable(additionalInfo.get(TWO_FACTOR_AUTH_ACCOUNT_CONFIG_KEY)).filter(jsonNode -> !jsonNode.isNull()))
                .map(jsonNode -> JacksonUtil.treeToValue(jsonNode, TwoFactorAuthAccountConfig.class))
                .filter(twoFactorAuthAccountConfig -> {
                    return getTwoFaProviderConfig(tenantId, twoFactorAuthAccountConfig.getProviderType()).isPresent();
                });
    }

    @Override
    public void saveTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFactorAuthAccountConfig accountConfig) throws ThingsboardException {
        getTwoFaProviderConfig(tenantId, accountConfig.getProviderType())
                .orElseThrow(() -> new ThingsboardException("2FA provider is not configured", ThingsboardErrorCode.BAD_REQUEST_PARAMS));

        User user = userService.findUserById(tenantId, userId);
        ObjectNode additionalInfo = (ObjectNode) Optional.ofNullable(user.getAdditionalInfo())
                .orElseGet(JacksonUtil::newObjectNode);
        additionalInfo.set(TWO_FACTOR_AUTH_ACCOUNT_CONFIG_KEY, JacksonUtil.valueToTree(accountConfig));
        user.setAdditionalInfo(additionalInfo);

        userService.saveUser(user);
    }

    @Override
    public void deleteTwoFaAccountConfig(TenantId tenantId, UserId userId) {
        User user = userService.findUserById(tenantId, userId);
        ObjectNode additionalInfo = (ObjectNode) Optional.ofNullable(user.getAdditionalInfo())
                .orElseGet(JacksonUtil::newObjectNode);
        additionalInfo.remove(TWO_FACTOR_AUTH_ACCOUNT_CONFIG_KEY);
        user.setAdditionalInfo(additionalInfo);

        userService.saveUser(user);
    }


    private Optional<TwoFactorAuthProviderConfig> getTwoFaProviderConfig(TenantId tenantId, TwoFactorAuthProviderType providerType) {
        return getTwoFaSettings(tenantId)
                .flatMap(twoFaSettings -> twoFaSettings.getProviderConfig(providerType));
    }

    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    @Override
    public Optional<TwoFactorAuthSettings> getTwoFaSettings(TenantId tenantId) {
        if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
            return Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(tenantId, TWO_FACTOR_AUTH_SETTINGS_KEY))
                    .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), TwoFactorAuthSettings.class));
        } else {
            return attributesService.find(TenantId.SYS_TENANT_ID, tenantId, DataConstants.SERVER_SCOPE, TWO_FACTOR_AUTH_SETTINGS_KEY).get()
                    .map(adminSettingsAttribute -> JacksonUtil.fromString(adminSettingsAttribute.getJsonValue().get(), TwoFactorAuthSettings.class))
                    .filter(tenantTwoFactorAuthSettings -> !tenantTwoFactorAuthSettings.isUseSystemTwoFactorAuthSettings())
                    .or(() -> getTwoFaSettings(TenantId.SYS_TENANT_ID));
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

}
