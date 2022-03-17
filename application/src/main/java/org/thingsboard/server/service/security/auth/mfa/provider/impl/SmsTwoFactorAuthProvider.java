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
package org.thingsboard.server.service.security.auth.mfa.provider.impl;

import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.config.account.SmsTwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.config.provider.SmsTwoFactorAuthProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProvider;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

@Service
@TbCoreComponent
public class SmsTwoFactorAuthProvider implements TwoFactorAuthProvider<SmsTwoFactorAuthProviderConfig, SmsTwoFactorAuthAccountConfig> {

    private final SmsService smsService;
    private final Cache verificationCodesCache;

    public SmsTwoFactorAuthProvider(SmsService smsService, CacheManager cacheManager) {
        this.smsService = smsService;
        this.verificationCodesCache = cacheManager.getCache(CacheConstants.TWO_FA_VERIFICATION_CODES_CACHE);
    }


    @Override
    public SmsTwoFactorAuthAccountConfig generateNewAccountConfig(User user, SmsTwoFactorAuthProviderConfig providerConfig) {
        return new SmsTwoFactorAuthAccountConfig();
    }

    @Override
    @SneakyThrows // fixme
    public void prepareVerificationCode(SecurityUser user, SmsTwoFactorAuthProviderConfig providerConfig, SmsTwoFactorAuthAccountConfig accountConfig) {
        String verificationCode = RandomStringUtils.randomNumeric(6);
        verificationCodesCache.put(user.getSessionId(), new VerificationCode(System.currentTimeMillis(), verificationCode));

        String phoneNumber = accountConfig.getPhoneNumber();

        Map<String, String> data = Map.of(
                "verificationCode", verificationCode,
                "userEmail", user.getEmail()
        );
        String message = TbNodeUtils.processTemplate(providerConfig.getSmsVerificationMessageTemplate(), data);

        smsService.sendSms(user.getTenantId(), user.getCustomerId(), new String[]{phoneNumber}, message);
    }

    @Override
    public boolean checkVerificationCode(SecurityUser user, String verificationCode, SmsTwoFactorAuthAccountConfig accountConfig) {
        VerificationCode correctVerificationCode = verificationCodesCache.get(user.getSessionId(), VerificationCode.class);
        if (correctVerificationCode != null && verificationCode.equals(correctVerificationCode.getValue())) {
            verificationCodesCache.evict(user.getSessionId());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public TwoFactorAuthProviderType getType() {
        return TwoFactorAuthProviderType.SMS;
    }


    @Data
    private static class VerificationCode {
        private final long timestamp;
        private final String value;
    }

}
