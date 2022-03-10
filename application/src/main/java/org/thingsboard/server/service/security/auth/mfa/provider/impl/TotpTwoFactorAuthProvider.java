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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.config.account.TotpTwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.config.provider.TotpTwoFactorAuthProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProvider;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@RequiredArgsConstructor
@TbCoreComponent
public class TotpTwoFactorAuthProvider implements TwoFactorAuthProvider<TotpTwoFactorAuthProviderConfig, TotpTwoFactorAuthAccountConfig> {

    @Override
    public final TotpTwoFactorAuthAccountConfig generateNewAccountConfig(User user, TotpTwoFactorAuthProviderConfig providerConfig) {
        TotpTwoFactorAuthAccountConfig config = new TotpTwoFactorAuthAccountConfig();
        String secretKey = generateSecretKey();
        config.setAuthUrl(getTotpAuthUrl(user, secretKey, providerConfig));
        return config;
    }

    @Override
    public final boolean checkVerificationCode(SecurityUser user, String verificationCode, TotpTwoFactorAuthAccountConfig accountConfig) {
        String secretKey = UriComponentsBuilder.fromUriString(accountConfig.getAuthUrl()).build().getQueryParams().getFirst("secret");
        return new Totp(secretKey).verify(verificationCode);
    }

    @SneakyThrows
    private String getTotpAuthUrl(User user, String secretKey, TotpTwoFactorAuthProviderConfig providerConfig) {
        URIBuilder uri = new URIBuilder()
                .setScheme("otpauth")
                .setHost("totp")
                .setParameter("issuer", providerConfig.getIssuerName())
                .setPath("/" + providerConfig.getIssuerName() + ":" + user.getEmail())
                .setParameter("secret", secretKey);
        return uri.build().toASCIIString();
    }

    private String generateSecretKey() {
        return Base32.encode(RandomUtils.nextBytes(20));
    }

    @Override
    public TwoFactorAuthProviderType getType() {
        return TwoFactorAuthProviderType.TOTP;
    }

}
