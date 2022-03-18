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
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.config.account.EmailTwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.config.provider.EmailTwoFactorAuthProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
public class EmailTwoFactorAuthProvider extends OtpBasedTwoFactorAuthProvider<EmailTwoFactorAuthProviderConfig, EmailTwoFactorAuthAccountConfig> {

    private final MailService mailService;

    protected EmailTwoFactorAuthProvider(CacheManager cacheManager, MailService mailService) {
        super(cacheManager);
        this.mailService = mailService;
    }


    @Override
    public EmailTwoFactorAuthAccountConfig generateNewAccountConfig(User user, EmailTwoFactorAuthProviderConfig providerConfig) {
        EmailTwoFactorAuthAccountConfig accountConfig = new EmailTwoFactorAuthAccountConfig();
        accountConfig.setUseAccountEmail(true);
        return accountConfig;
    }

    @Override
    protected void sendVerificationCode(SecurityUser user, String verificationCode, EmailTwoFactorAuthProviderConfig providerConfig, EmailTwoFactorAuthAccountConfig accountConfig) throws ThingsboardException {
        String email;
        if (accountConfig.isUseAccountEmail()) {
            email = user.getEmail();
        } else {
            email = accountConfig.getEmail();
        }

        // FIXME [viacheslav]: mail template for 2FA verification
        mailService.sendEmail(user.getTenantId(), email, "subject", "");
    }


    @Override
    public TwoFactorAuthProviderType getType() {
        return TwoFactorAuthProviderType.EMAIL;
    }

}
