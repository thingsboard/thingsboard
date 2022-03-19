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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFactorAuthConfigManager;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFactorAuthSettings;
import org.thingsboard.server.service.security.auth.mfa.config.account.TotpTwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.config.account.TwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
public class TwoFactorAuthConfigController extends BaseController {

    private final TwoFactorAuthConfigManager twoFactorAuthConfigManager;
    private final TwoFactorAuthService twoFactorAuthService;


    @GetMapping("/account/config")
    @PreAuthorize("isAuthenticated()")
    public TwoFactorAuthAccountConfig getTwoFaAccountConfig() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return twoFactorAuthConfigManager.getTwoFaAccountConfig(user.getTenantId(), user.getId()).orElse(null);
    }

    @PostMapping("/account/config/generate")
    @PreAuthorize("isAuthenticated()")
    public TwoFactorAuthAccountConfig generateTwoFaAccountConfig(@RequestParam TwoFactorAuthProviderType providerType) throws Exception {
        SecurityUser user = getCurrentUser();
        return twoFactorAuthService.generateNewAccountConfig(user, providerType);
    }

    /* TMP */
    @PostMapping("/account/config/generate/qr")
    @PreAuthorize("isAuthenticated()")
    public void generateTwoFaAccountConfigWithQr(@RequestParam TwoFactorAuthProviderType providerType, HttpServletResponse response) throws Exception {
        TwoFactorAuthAccountConfig config = generateTwoFaAccountConfig(providerType);
        if (providerType == TwoFactorAuthProviderType.TOTP) {
            BitMatrix qr = new QRCodeWriter().encode(((TotpTwoFactorAuthAccountConfig) config).getAuthUrl(), BarcodeFormat.QR_CODE, 200, 200);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                MatrixToImageWriter.writeToStream(qr, "PNG", outputStream);
            }
        }
        response.setHeader("config", JacksonUtil.toString(config));
    }
    /* TMP */

    @PostMapping("/account/config/submit")
    @PreAuthorize("isAuthenticated()")
    public void submitTwoFaAccountConfig(@Valid @RequestBody TwoFactorAuthAccountConfig accountConfig) throws Exception {
        SecurityUser user = getCurrentUser();
        twoFactorAuthService.prepareVerificationCode(user, accountConfig, false);
    }

    @PostMapping("/account/config")
    @PreAuthorize("isAuthenticated()")
    public void verifyAndSaveTwoFaAccountConfig(@Valid @RequestBody TwoFactorAuthAccountConfig accountConfig,
                                                @RequestParam String verificationCode) throws Exception {
        SecurityUser user = getCurrentUser();
        boolean verificationSuccess = twoFactorAuthService.checkVerificationCode(user, verificationCode, accountConfig, false);
        if (verificationSuccess) {
            twoFactorAuthConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user.getId(), accountConfig);
        } else {
            throw new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
    }

    @DeleteMapping("/account/config")
    @PreAuthorize("isAuthenticated()")
    public void deleteTwoFactorAuthAccountConfig() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        twoFactorAuthConfigManager.deleteTwoFaAccountConfig(user.getTenantId(), user.getId());
    }


    @GetMapping("/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public TwoFactorAuthSettings getTwoFactorAuthSettings() throws ThingsboardException {
        return twoFactorAuthConfigManager.getTwoFaSettings(getTenantId()).orElse(null);
    }

    @PostMapping("/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void saveTwoFactorAuthSettings(@RequestBody TwoFactorAuthSettings twoFactorAuthSettings) throws ThingsboardException {
        twoFactorAuthConfigManager.saveTwoFaSettings(getTenantId(), twoFactorAuthSettings);
    }

}
