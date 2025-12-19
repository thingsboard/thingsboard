/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth.mfa;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.targets.platform.SystemLevelUsersFilter;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@TbCoreComponent
public class DefaultTwoFactorAuthService implements TwoFactorAuthService {

    private final TwoFaConfigManager configManager;
    private final SystemSecurityService systemSecurityService;
    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final Map<TwoFaProviderType, TwoFaProvider<TwoFaProviderConfig, TwoFaAccountConfig>> providers = new EnumMap<>(TwoFaProviderType.class);

    private static final ThingsboardException ACCOUNT_NOT_CONFIGURED_ERROR = new ThingsboardException("2FA is not configured for account", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    private static final ThingsboardException PROVIDER_NOT_CONFIGURED_ERROR = new ThingsboardException("2FA provider is not configured", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    private static final ThingsboardException PROVIDER_NOT_AVAILABLE_ERROR = new ThingsboardException("2FA provider is not available", ThingsboardErrorCode.GENERAL);
    private static final ThingsboardException TOO_MANY_REQUESTS_ERROR = new ThingsboardException("Too many requests", ThingsboardErrorCode.TOO_MANY_REQUESTS);

    @Override
    public boolean isTwoFaEnabled(TenantId tenantId, User user) {
        return configManager.getAccountTwoFaSettings(tenantId, user)
                .map(settings -> !settings.getConfigs().isEmpty())
                .orElse(false);
    }

    @Override
    public boolean isEnforceTwoFaEnabled(TenantId tenantId, User user) {
        SystemLevelUsersFilter enforcedUsersFilter = configManager.getPlatformTwoFaSettings(TenantId.SYS_TENANT_ID, true)
                .filter(PlatformTwoFaSettings::isEnforceTwoFa)
                .map(PlatformTwoFaSettings::getEnforcedUsersFilter)
                .orElse(null);
        if (enforcedUsersFilter == null) {
            return false;
        }

        return userService.matchesFilter(tenantId, enforcedUsersFilter, user);
    }

    @Override
    public void checkProvider(TenantId tenantId, TwoFaProviderType providerType) throws ThingsboardException {
        getTwoFaProvider(providerType).check(tenantId);
    }


    @Override
    public void prepareVerificationCode(SecurityUser user, TwoFaProviderType providerType, boolean checkLimits) throws Exception {
        TwoFaAccountConfig accountConfig = configManager.getTwoFaAccountConfig(user.getTenantId(), user, providerType)
                .orElseThrow(() -> ACCOUNT_NOT_CONFIGURED_ERROR);
        prepareVerificationCode(user, accountConfig, checkLimits);
    }

    @Override
    public void prepareVerificationCode(SecurityUser user, TwoFaAccountConfig accountConfig, boolean checkLimits) throws ThingsboardException {
        PlatformTwoFaSettings twoFaSettings = configManager.getPlatformTwoFaSettings(user.getTenantId(), true)
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
        if (checkLimits) {
            Integer minVerificationCodeSendPeriod = twoFaSettings.getMinVerificationCodeSendPeriod();
            String rateLimit = null;
            if (minVerificationCodeSendPeriod != null && minVerificationCodeSendPeriod > 4) {
                rateLimit = "1:" + minVerificationCodeSendPeriod;
            }
            if (!rateLimitService.checkRateLimit(LimitedApi.TWO_FA_VERIFICATION_CODE_SEND,
                    Pair.of(user.getId(), accountConfig.getProviderType()), rateLimit)) {
                throw TOO_MANY_REQUESTS_ERROR;
            }
        }

        TwoFaProviderConfig providerConfig = twoFaSettings.getProviderConfig(accountConfig.getProviderType())
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
        getTwoFaProvider(accountConfig.getProviderType()).prepareVerificationCode(user, providerConfig, accountConfig);
    }


    @Override
    public boolean checkVerificationCode(SecurityUser user, TwoFaProviderType providerType, String verificationCode, boolean checkLimits) throws ThingsboardException {
        TwoFaAccountConfig accountConfig = configManager.getTwoFaAccountConfig(user.getTenantId(), user, providerType)
                .orElseThrow(() -> ACCOUNT_NOT_CONFIGURED_ERROR);
        return checkVerificationCode(user, verificationCode, accountConfig, checkLimits);
    }

    @Override
    public boolean checkVerificationCode(SecurityUser user, String verificationCode, TwoFaAccountConfig accountConfig, boolean checkLimits) throws ThingsboardException {
        if (!userService.findUserCredentialsByUserId(user.getTenantId(), user.getId()).isEnabled()) {
            throw new ThingsboardException("User is disabled", ThingsboardErrorCode.AUTHENTICATION);
        }

        PlatformTwoFaSettings twoFaSettings = configManager.getPlatformTwoFaSettings(user.getTenantId(), true)
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
        if (checkLimits) {
            if (!rateLimitService.checkRateLimit(LimitedApi.TWO_FA_VERIFICATION_CODE_CHECK,
                    Pair.of(user.getId(), accountConfig.getProviderType()), twoFaSettings.getVerificationCodeCheckRateLimit())) {
                throw TOO_MANY_REQUESTS_ERROR;
            }
        }
        TwoFaProviderConfig providerConfig = twoFaSettings.getProviderConfig(accountConfig.getProviderType())
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);

        boolean verificationSuccess = false;
        if (StringUtils.isNotBlank(verificationCode)) {
            if (StringUtils.isNumeric(verificationCode) || accountConfig.getProviderType() == TwoFaProviderType.BACKUP_CODE) {
                verificationSuccess = getTwoFaProvider(accountConfig.getProviderType()).checkVerificationCode(user, verificationCode, providerConfig, accountConfig);
            }
        }
        if (checkLimits) {
            try {
                systemSecurityService.validateTwoFaVerification(user, verificationSuccess, twoFaSettings);
            } catch (LockedException e) {
                cleanUpRateLimits(user.getId());
                throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.AUTHENTICATION);
            }
            if (verificationSuccess) {
                cleanUpRateLimits(user.getId());
            }
        }
        return verificationSuccess;
    }

    @Override
    public TwoFaAccountConfig generateNewAccountConfig(User user, TwoFaProviderType providerType) throws ThingsboardException {
        TwoFaProviderConfig providerConfig = getTwoFaProviderConfig(user.getTenantId(), providerType);
        return getTwoFaProvider(providerType).generateNewAccountConfig(user, providerConfig);
    }

    private void cleanUpRateLimits(UserId userId) {
        for (TwoFaProviderType providerType : TwoFaProviderType.values()) {
            rateLimitService.cleanUp(LimitedApi.TWO_FA_VERIFICATION_CODE_SEND, Pair.of(userId, providerType));
            rateLimitService.cleanUp(LimitedApi.TWO_FA_VERIFICATION_CODE_CHECK, Pair.of(userId, providerType));
        }
    }

    private TwoFaProviderConfig getTwoFaProviderConfig(TenantId tenantId, TwoFaProviderType providerType) throws ThingsboardException {
        return configManager.getPlatformTwoFaSettings(tenantId, true)
                .flatMap(twoFaSettings -> twoFaSettings.getProviderConfig(providerType))
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
    }

    private TwoFaProvider<TwoFaProviderConfig, TwoFaAccountConfig> getTwoFaProvider(TwoFaProviderType providerType) throws ThingsboardException {
        return Optional.ofNullable(providers.get(providerType))
                .orElseThrow(() -> PROVIDER_NOT_AVAILABLE_ERROR);
    }

    @Autowired
    private void setProviders(Collection<TwoFaProvider> providers) {
        providers.forEach(provider -> {
            this.providers.put(provider.getType(), provider);
        });
    }

}
