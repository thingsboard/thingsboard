// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.mfa.provider.impl;

import lombok.Data;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.mfa.account.OtpBasedTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.OtpBasedTwoFaProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public abstract class OtpBasedTwoFaProvider<C extends OtpBasedTwoFaProviderConfig, A extends OtpBasedTwoFaAccountConfig> implements TwoFaProvider<C, A> {

    private final Cache verificationCodesCache;

    protected OtpBasedTwoFaProvider(CacheManager cacheManager) {
        this.verificationCodesCache = cacheManager.getCache(CacheConstants.TWO_FA_VERIFICATION_CODES_CACHE);
    }


    @Override
    public final void prepareVerificationCode(SecurityUser user, C providerConfig, A accountConfig) throws ThingsboardException {
        String verificationCode = StringUtils.randomNumeric(6);
        sendVerificationCode(user, verificationCode, providerConfig, accountConfig);
        verificationCodesCache.put(user.getId(), new Otp(System.currentTimeMillis(), verificationCode, accountConfig));
    }

    protected abstract void sendVerificationCode(SecurityUser user, String verificationCode, C providerConfig, A accountConfig) throws ThingsboardException;


    @Override
    public final boolean checkVerificationCode(SecurityUser user, String code, C providerConfig, A accountConfig) {
        Otp correctVerificationCode = verificationCodesCache.get(user.getId(), Otp.class);
        if (correctVerificationCode != null) {
            if (System.currentTimeMillis() - correctVerificationCode.getTimestamp()
                    > TimeUnit.SECONDS.toMillis(providerConfig.getVerificationCodeLifetime())) {
                verificationCodesCache.evict(user.getId());
                return false;
            }
            if (code.equals(correctVerificationCode.getValue())
                    && accountConfig.equals(correctVerificationCode.getAccountConfig())) {
                verificationCodesCache.evict(user.getId());
                return true;
            }
        }
        return false;
    }


    @Data
    public static class Otp implements Serializable {
        private final long timestamp;
        private final String value;
        private final OtpBasedTwoFaAccountConfig accountConfig;
    }

}
