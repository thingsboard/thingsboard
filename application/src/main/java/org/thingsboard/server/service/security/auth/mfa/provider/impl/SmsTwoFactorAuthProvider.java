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

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.mfa.account.SmsTwoFactorAuthAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.SmsTwoFactorAuthProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFactorAuthProviderType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

@Service
@TbCoreComponent
public class SmsTwoFactorAuthProvider extends OtpBasedTwoFactorAuthProvider<SmsTwoFactorAuthProviderConfig, SmsTwoFactorAuthAccountConfig> {

    private final SmsService smsService;

    public SmsTwoFactorAuthProvider(CacheManager cacheManager, SmsService smsService) {
        super(cacheManager);
        this.smsService = smsService;
    }


    @Override
    public SmsTwoFactorAuthAccountConfig generateNewAccountConfig(User user, SmsTwoFactorAuthProviderConfig providerConfig) {
        return new SmsTwoFactorAuthAccountConfig();
    }

    @Override
    protected void sendVerificationCode(SecurityUser user, String verificationCode, SmsTwoFactorAuthProviderConfig providerConfig, SmsTwoFactorAuthAccountConfig accountConfig) throws ThingsboardException {
        Map<String, String> messageData = Map.of(
                "verificationCode", verificationCode,
                "userEmail", user.getEmail()
        );
        String message = TbNodeUtils.processTemplate(providerConfig.getSmsVerificationMessageTemplate(), messageData);
        String phoneNumber = accountConfig.getPhoneNumber();

        smsService.sendSms(user.getTenantId(), user.getCustomerId(), new String[]{phoneNumber}, message);
    }


    @Override
    public TwoFactorAuthProviderType getType() {
        return TwoFactorAuthProviderType.SMS;
    }

}
