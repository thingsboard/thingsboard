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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
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
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/*
 *
 * TODO [viacheslav]:
 *  - Configurable softlock after XX (3) attempts: XX (15) mins - on session level
 *  - Configurable hardlock (user blocking) after a total of XX (10) unsuccessful attempts - on user level
 *
 * FIXME [viacheslav]:
 *  - Tests for 2FA
 *  - Swagger documentation
 *
 * */
// TODO [viacheslav]: maybe get rid of sessionId concept..

/*
 *
 *
 * TODO (later):
 *  - 2FA entries should be secured against code injection by code validation
 *  - ability to force users to use 2FA (maybe on log in, do not give them token pair but to give temporary
 *      token to configure 2FA account config); also will need to make users configure 2FA during activation and password setup...
 * */
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
    public TwoFactorAuthAccountConfig generateTwoFactorAuthAccountConfig(@RequestParam TwoFactorAuthProviderType providerType) throws Exception {
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
        response.setHeader("config", JacksonUtil.toString(config));
    }

    @PostMapping("/2fa/account/config/submit")
    @PreAuthorize("isAuthenticated()")
    public void submitTwoFactorAuthAccountConfig(@Valid @RequestBody TwoFactorAuthAccountConfig accountConfig) throws Exception {
        SecurityUser user = getCurrentUser();

        twoFactorAuthService.processByTwoFaProvider(user.getTenantId(), accountConfig.getProviderType(),
                (provider, providerConfig) -> {
                    provider.prepareVerificationCode(user, providerConfig, accountConfig);
                });
    }

    @PostMapping("/2fa/account/config")
    @PreAuthorize("isAuthenticated()")
    public void verifyAndSaveTwoFactorAuthAccountConfig(@Valid @RequestBody TwoFactorAuthAccountConfig accountConfig,
                                                        @RequestParam String verificationCode) throws Exception {
        SecurityUser user = getCurrentUser();

        boolean verificationSuccess = twoFactorAuthService.processByTwoFaProvider(user.getTenantId(), accountConfig.getProviderType(),
                (provider, providerConfig) -> {
                    return provider.checkVerificationCode(user, verificationCode, providerConfig, accountConfig);
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
    public void saveTwoFactorAuthSettings(@Valid @RequestBody TwoFactorAuthSettings twoFactorAuthSettings) throws ThingsboardException {
        twoFactorAuthService.saveTwoFaSettings(getTenantId(), twoFactorAuthSettings);
    }


    private final Map<String, TbRateLimits> verificationCodeSendRateLimits = new HashMap<>();
    private final Map<String, TbRateLimits> verificationCodeCheckRateLimits = new HashMap<>();

    @PostMapping("/auth/2fa/verification/send")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public void sendTwoFaVerificationCode() throws Exception {
        SecurityUser user = getCurrentUser();

        TwoFactorAuthSettings twoFaSettings = twoFactorAuthService.getTwoFaSettings(user.getTenantId()).get();
        if (StringUtils.isNotEmpty(twoFaSettings.getVerificationCodeSendRateLimit())) {
            TbRateLimits rateLimits = verificationCodeSendRateLimits.computeIfAbsent(user.getSessionId(), sessionId -> {
                return new TbRateLimits(twoFaSettings.getVerificationCodeSendRateLimit());
            });
            if (!rateLimits.tryConsume()) {
                throw new ThingsboardException(ThingsboardErrorCode.TOO_MANY_REQUESTS);
            }
        }

        twoFactorAuthService.processByTwoFaProvider(user.getTenantId(), user.getId(),
                (provider, providerConfig, accountConfig) -> {
                    provider.prepareVerificationCode(user, providerConfig, accountConfig);
                });
    }

    @PostMapping("/auth/2fa/verification/check")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public JwtTokenPair checkTwoFaVerificationCode(@RequestParam String verificationCode) throws Exception {
        SecurityUser user = getCurrentUser();



        // FIXME [viacheslav]: rate limits for verification code check
        boolean verificationSuccess = twoFactorAuthService.processByTwoFaProvider(user.getTenantId(), user.getId(),
                (provider, providerConfig, accountConfig) -> {
                    return provider.checkVerificationCode(user, verificationCode, providerConfig, accountConfig);
                });


        if (verificationSuccess) {
            return tokenFactory.createTokenPair(user);
        } else {
            TwoFactorAuthSettings twoFaSettings = twoFactorAuthService.getTwoFaSettings(user.getTenantId()).get();
            if (StringUtils.isNotEmpty(twoFaSettings.getVerificationCodeSendRateLimit())) {
                TbRateLimits rateLimits = verificationCodeSendRateLimits.computeIfAbsent(user.getSessionId(), sessionId -> {
                    return new TbRateLimits(twoFaSettings.getVerificationCodeSendRateLimit());
                });
                if (!rateLimits.tryConsume()) {
                    throw new ThingsboardException(ThingsboardErrorCode.TOO_MANY_REQUESTS);
                }
            }
            throw new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.AUTHENTICATION);
        }
    }

}
