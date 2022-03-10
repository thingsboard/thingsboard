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
package org.thingsboard.server.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFactorAuthSettings;
import org.thingsboard.server.service.security.auth.mfa.config.account.TotpTwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.config.account.TwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;
import org.thingsboard.server.service.security.model.JwtTokenPair;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TwoFactorAuthController extends BaseController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final JwtTokenFactory tokenFactory;


    @GetMapping("/2fa/account/config")
    @PreAuthorize("isAuthenticated()")
    public TwoFactorAuthAccountConfig getTwoFactorAuthAccountConfig() throws ThingsboardException {
        SecurityUser user = getCurrentUser();

        return twoFactorAuthService.getTwoFaAccountConfig(user.getTenantId(), user.getId()).orElse(null);
    }

    @PostMapping("/2fa/account/config/generate")
    @PreAuthorize("isAuthenticated()")
    public TwoFactorAuthAccountConfig generateTwoFactorAuthAccountConfig(@RequestParam TwoFactorAuthProviderType providerType) throws ThingsboardException {
        SecurityUser user = getCurrentUser();

        return twoFactorAuthService.processByTwoFaProvider(user.getTenantId(), providerType,
                (provider, providerConfig) -> {
                    return provider.generateNewAccountConfig(user, providerConfig);
                });
    }

    // temporary endpoint for testing purposes
    @PostMapping("/2fa/account/config/generate/qr")
    @PreAuthorize("isAuthenticated()")
    public void generateTwoFactorAuthAccountConfigWithQr(@RequestParam TwoFactorAuthProviderType providerType, HttpServletResponse response) throws Exception {
        TwoFactorAuthAccountConfig config = generateTwoFactorAuthAccountConfig(providerType);
        if (providerType == TwoFactorAuthProviderType.TOTP) {
            BitMatrix qr = new QRCodeWriter().encode(((TotpTwoFactorAuthAccountConfig) config).getAuthUrl(), BarcodeFormat.QR_CODE, 200, 200);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                MatrixToImageWriter.writeToStream(qr, "PNG", outputStream);
            }
        }
        response.setHeader("body", JacksonUtil.toString(config));
    }

    @PostMapping("/2fa/account/config/submit")
    @PreAuthorize("isAuthenticated()")
    public void submitTwoFactorAuthAccountConfig(@RequestBody TwoFactorAuthAccountConfig accountConfig) throws ThingsboardException {
        SecurityUser user = getCurrentUser();

        twoFactorAuthService.processByTwoFaProvider(user.getTenantId(), accountConfig.getProviderType(),
                (provider, providerConfig) -> {
                    provider.prepareVerificationCode(user, providerConfig, accountConfig);
                });
    }

    @PostMapping("/2fa/account/config")
    @PreAuthorize("isAuthenticated()")
    public void verifyAndSaveTwoFactorAuthAccountConfig(@RequestBody TwoFactorAuthAccountConfig accountConfig,
                                                        @RequestParam String verificationCode) throws ThingsboardException {
        SecurityUser user = getCurrentUser();

        boolean verificationSuccess = twoFactorAuthService.processByTwoFaProvider(user.getTenantId(), accountConfig.getProviderType(),
                (provider, providerConfig) -> {
                    return provider.checkVerificationCode(user, verificationCode, accountConfig);
                });

        if (verificationSuccess) {
            twoFactorAuthService.saveTwoFaAccountConfig(user.getTenantId(), user.getId(), accountConfig);
        } else {
            throw new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
    }


    @GetMapping("/2fa/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public TwoFactorAuthSettings getTwoFactorAuthSettings() throws ThingsboardException {
        return twoFactorAuthService.getTwoFaSettings(getTenantId()).orElse(null);
    }

    @PostMapping("/2fa/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void saveTwoFactorAuthSettings(@RequestBody TwoFactorAuthSettings twoFactorAuthSettings) throws ThingsboardException {
        twoFactorAuthService.saveTwoFaSettings(getTenantId(), twoFactorAuthSettings);
    }


    @PostMapping("/auth/2fa/verification/check")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public JwtTokenPair checkTwoFaVerificationCode(@RequestParam String verificationCode) throws ThingsboardException {
        SecurityUser user = getCurrentUser();

        boolean verificationSuccess = twoFactorAuthService.processByTwoFaProvider(user.getTenantId(), user.getId(),
                (provider, providerConfig, accountConfig) -> {
                    return provider.checkVerificationCode(user, verificationCode, accountConfig);
                });

        if (verificationSuccess) {
            return tokenFactory.createTokenPair(user);
        } else {
            throw new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.AUTHENTICATION);
        }
    }

}
