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
