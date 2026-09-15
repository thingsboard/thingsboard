// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
        return Base32.encode(RandomUtils.secure().randomBytes(20));
    }


    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.TOTP;
    }

}
