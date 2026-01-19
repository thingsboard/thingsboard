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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TotpTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@RequiredArgsConstructor
@TbCoreComponent
public class TotpTwoFaProvider implements TwoFaProvider<TotpTwoFaProviderConfig, TotpTwoFaAccountConfig> {

    @Override
    public final TotpTwoFaAccountConfig generateNewAccountConfig(User user, TotpTwoFaProviderConfig providerConfig) {
        TotpTwoFaAccountConfig config = new TotpTwoFaAccountConfig();
        String secretKey = generateSecretKey();
        config.setAuthUrl(getTotpAuthUrl(user, secretKey, providerConfig));
        return config;
    }

    @Override
    public final boolean checkVerificationCode(SecurityUser user, String code, TotpTwoFaProviderConfig providerConfig, TotpTwoFaAccountConfig accountConfig) {
        String secretKey = UriComponentsBuilder.fromUriString(accountConfig.getAuthUrl()).build().getQueryParams().getFirst("secret");
        return new Totp(secretKey).verify(code);
    }

    @SneakyThrows
    private String getTotpAuthUrl(User user, String secretKey, TotpTwoFaProviderConfig providerConfig) {
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
    public TwoFaProviderType getType() {
        return TwoFaProviderType.TOTP;
    }

}
