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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.mfa.config.account.TwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.config.provider.TwoFactorAuthProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class DefaultTwoFactorAuthConfigManager implements TwoFactorAuthConfigManager {

    private final UserService userService;
    private final AdminSettingsService adminSettingsService;
    private final AdminSettingsDao adminSettingsDao;
    private final AttributesService attributesService;

    protected static final String TWO_FACTOR_AUTH_ACCOUNT_CONFIG_KEY = "twoFaConfig";
    protected static final String TWO_FACTOR_AUTH_SETTINGS_KEY = "twoFaSettings";


    @Override
    public boolean isTwoFaEnabled(TenantId tenantId, UserId userId) {
        return getTwoFaAccountConfig(tenantId, userId).isPresent();
    }

    @Override
    public Optional<TwoFactorAuthAccountConfig> getTwoFaAccountConfig(TenantId tenantId, UserId userId) {
        return Optional.ofNullable(getAccountInfo(tenantId, userId).get(TWO_FACTOR_AUTH_ACCOUNT_CONFIG_KEY))
                .filter(JsonNode::isObject)
                .map(jsonNode -> JacksonUtil.treeToValue(jsonNode, TwoFactorAuthAccountConfig.class))
                .filter(twoFactorAuthAccountConfig -> {
                    return getTwoFaProviderConfig(tenantId, twoFactorAuthAccountConfig.getProviderType()).isPresent();
                });
    }

    @Override
    public void saveTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFactorAuthAccountConfig accountConfig) throws ThingsboardException {
        getTwoFaProviderConfig(tenantId, accountConfig.getProviderType())
                .orElseThrow(() -> new ThingsboardException("2FA provider is not configured", ThingsboardErrorCode.BAD_REQUEST_PARAMS));

        updateAccountInfo(tenantId, userId, accountInfo -> {
            accountInfo.set(TWO_FACTOR_AUTH_ACCOUNT_CONFIG_KEY, JacksonUtil.valueToTree(accountConfig));
        });
    }

    @Override
    public void deleteTwoFaAccountConfig(TenantId tenantId, UserId userId) {
        updateAccountInfo(tenantId, userId, accountInfo -> {
            accountInfo.remove(TWO_FACTOR_AUTH_ACCOUNT_CONFIG_KEY);
        });
    }

    private ObjectNode getAccountInfo(TenantId tenantId, UserId userId) {
        return (ObjectNode) Optional.ofNullable(userService.findUserCredentialsByUserId(tenantId, userId).getAdditionalInfo())
                .filter(JsonNode::isObject)
                .orElseGet(JacksonUtil::newObjectNode);
    }

    // FIXME [viacheslav]: upgrade script for credentials' additional info
    private void updateAccountInfo(TenantId tenantId, UserId userId, Consumer<ObjectNode> updater) {
        UserCredentials credentials = userService.findUserCredentialsByUserId(tenantId, userId);
        ObjectNode additionalInfo = (ObjectNode) Optional.ofNullable(credentials.getAdditionalInfo())
                .filter(JsonNode::isObject)
                .orElseGet(JacksonUtil::newObjectNode);
        updater.accept(additionalInfo);
        credentials.setAdditionalInfo(additionalInfo);
        userService.saveUserCredentials(tenantId, credentials);
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
