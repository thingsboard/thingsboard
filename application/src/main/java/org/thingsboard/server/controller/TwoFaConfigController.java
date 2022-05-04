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
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.AccountTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;

@RestController
@RequestMapping("/api/2fa")
@TbCoreComponent
@RequiredArgsConstructor
public class TwoFaConfigController extends BaseController {

    private final TwoFaConfigManager twoFaConfigManager;
    private final TwoFactorAuthService twoFactorAuthService;


    @ApiOperation(value = "Get 2FA account config (getTwoFaAccountConfig)",
            notes = "Get user's account 2FA configuration. Returns empty result if user did not configured 2FA, " +
                    "or if a provider for previously set up account config is not now configured." + NEW_LINE +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER + NEW_LINE +
                    "Response example for TOTP 2FA: " + NEW_LINE +
                    "```\n{\n" +
                    "  \"providerType\": \"TOTP\",\n" +
                    "  \"authUrl\": \"otpauth://totp/ThingsBoard:tenant@thingsboard.org?issuer=ThingsBoard&secret=FUNBIM3CXFNNGQR6ZIPVWHP65PPFWDII\"\n" +
                    "}\n```" + NEW_LINE +
                    "Response example for SMS 2FA: " + NEW_LINE +
                    "```\n{\n" +
                    "  \"providerType\": \"SMS\",\n" +
                    "  \"phoneNumber\": \"+380505005050\"\n" +
                    "}\n```")
    @GetMapping("/account/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public AccountTwoFaSettings getAccountTwoFaSettings() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return twoFaConfigManager.getAccountTwoFaSettings(user.getTenantId(), user.getId()).orElse(null);
    }


    @ApiOperation(value = "Generate 2FA account config (generateTwoFaAccountConfig)",
            notes = "Generate new 2FA account config for specified provider type. " +
                    "This method is only useful for TOTP 2FA, as there is nothing to generate for other provider types. " +
                    "For TOTP, this will return a corresponding account config template " +
                    "with a generated OTP auth URL (with new random secret key for each API call) that can be then " +
                    "converted to a QR code to scan with an authenticator app. " +
                    "For other provider types, this method will return an empty config. " + NEW_LINE +
                    "Will throw an error (Bad Request) if the provider is not configured for usage. " +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER + NEW_LINE +
                    "Example of a generated account config for TOTP 2FA: " + NEW_LINE +
                    "```\n{\n" +
                    "  \"providerType\": \"TOTP\",\n" +
                    "  \"authUrl\": \"otpauth://totp/ThingsBoard:tenant@thingsboard.org?issuer=ThingsBoard&secret=FUNBIM3CXFNNGQR6ZIPVWHP65PPFWDII\"\n" +
                    "}\n```" + NEW_LINE +
                    "For SMS provider type it will return something like: " + NEW_LINE +
                    "```\n{\n" +
                    "  \"providerType\": \"SMS\",\n" +
                    "  \"phoneNumber\": null\n" +
                    "}\n```")
    @PostMapping("/account/config/generate")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public TwoFaAccountConfig generateTwoFaAccountConfig(@ApiParam(value = "2FA provider type to generate new account config for", defaultValue = "TOTP", required = true)
                                                         @RequestParam TwoFaProviderType providerType) throws Exception {
        SecurityUser user = getCurrentUser();
        return twoFactorAuthService.generateNewAccountConfig(user, providerType);
    }

    /* TMP */
    @PostMapping("/account/config/tmp/generate/qr")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void generateTwoFaAccountConfigWithQr(@RequestParam TwoFaProviderType providerType, HttpServletResponse response) throws Exception {
        TwoFaAccountConfig config = generateTwoFaAccountConfig(providerType);
        if (providerType == TwoFaProviderType.TOTP) {
            BitMatrix qr = new QRCodeWriter().encode(((TotpTwoFaAccountConfig) config).getAuthUrl(), BarcodeFormat.QR_CODE, 200, 200);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                MatrixToImageWriter.writeToStream(qr, "PNG", outputStream);
            }
        }
        response.setHeader("config", JacksonUtil.toString(config));
    }
    /* TMP */

    @ApiOperation(value = "Submit 2FA account config (submitTwoFaAccountConfig)",
            notes = "Submit 2FA account config to prepare for a future verification. " +
                    "Basically, this method will send a verification code for a given account config, if this has " +
                    "sense for a chosen 2FA provider. This code is needed to then verify and save the account config." + NEW_LINE +
                    "Will throw an error (Bad Request) if submitted account config is not valid, " +
                    "or if the provider is not configured for usage. " +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PostMapping("/account/config/submit")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void submitTwoFaAccountConfig(@ApiParam(value = "2FA account config value. For TOTP 2FA config, authUrl value must not be blank and must match specific pattern. " +
            "For SMS 2FA, phoneNumber property must not be blank and must be of E.164 phone number format.", required = true)
                                         @Valid @RequestBody TwoFaAccountConfig accountConfig) throws Exception {
        SecurityUser user = getCurrentUser();
        twoFactorAuthService.prepareVerificationCode(user, accountConfig, false);
    }

    @ApiOperation(value = "Verify and save 2FA account config (verifyAndSaveTwoFaAccountConfig)",
            notes = "Checks the verification code for submitted config, and if it is correct, saves the provided account config. " +
                    "The config is stored in the user's additionalInfo. " + NEW_LINE +
                    "Will throw an error (Bad Request) if the provider is not configured for usage. " +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PostMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public AccountTwoFaSettings verifyAndSaveTwoFaAccountConfig(@ApiParam(value = "2FA account config to save. Validation rules are the same as in submitTwoFaAccountConfig API method", required = true)
                                                                @Valid @RequestBody TwoFaAccountConfig accountConfig,
                                                                @ApiParam(value = "6-digit code from an authenticator app in case of TOTP 2FA, or the one sent via an SMS message in case of SMS 2FA", required = true)
                                                                @RequestParam String verificationCode) throws Exception {
        SecurityUser user = getCurrentUser();
        boolean verificationSuccess = twoFactorAuthService.checkVerificationCode(user, verificationCode, accountConfig, false);
        if (verificationSuccess) {
            return twoFaConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user.getId(), accountConfig);
        } else {
            throw new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
    }

    @PutMapping("/account/config")
    public AccountTwoFaSettings updateTwoFaAccountConfig(@RequestParam TwoFaProviderType providerType,
                                                         @RequestBody TwoFaAccountConfigUpdateRequest updateRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        TwoFaAccountConfig accountConfig = twoFaConfigManager.getTwoFaAccountConfig(user.getTenantId(), user.getId(), providerType)
                .orElseThrow(() -> new IllegalArgumentException("No 2FA config for provider " + providerType));

        accountConfig.setUseByDefault(updateRequest.isUseByDefault());
        return twoFaConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user.getId(), accountConfig);
    }

    @ApiOperation(value = "Delete 2FA account config (deleteTwoFaAccountConfig)",
            notes = "Delete user's 2FA config. " + ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @DeleteMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public AccountTwoFaSettings deleteTwoFaAccountConfig(@RequestParam TwoFaProviderType providerType) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return twoFaConfigManager.deleteTwoFaAccountConfig(user.getTenantId(), user.getId(), providerType);
    }


    @GetMapping("/providers")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<TwoFaProviderType> getAvailableTwoFaProviders() throws ThingsboardException {
        return twoFaConfigManager.getPlatformTwoFaSettings(getTenantId(), true)
                .map(PlatformTwoFaSettings::getProviders).orElse(Collections.emptyList()).stream()
                .map(TwoFaProviderConfig::getProviderType)
                .collect(Collectors.toList());
    }


    @ApiOperation(value = "Get 2FA settings (getTwoFaSettings)", // FIXME [viacheslav]
            notes = "Get settings for 2FA. If 2FA is not configured, then an empty response will be returned." +
                    ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PlatformTwoFaSettings getPlatformTwoFaSettings() throws ThingsboardException {
        return twoFaConfigManager.getPlatformTwoFaSettings(getTenantId(), false).orElse(null);
    }

    @ApiOperation(value = "Save 2FA settings (saveTwoFaSettings)",
            notes = "Save settings for 2FA. If a user is sysadmin - the settings are saved as AdminSettings; " +
                    "if it is a tenant admin - as a tenant attribute." +
                    ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void savePlatformTwoFaSettings(@ApiParam(value = "Settings value", required = true)
                                          @RequestBody PlatformTwoFaSettings twoFaSettings) throws ThingsboardException {
        twoFaConfigManager.savePlatformTwoFaSettings(getTenantId(), twoFaSettings);
    }


    @Data
    public static class TwoFaAccountConfigUpdateRequest {
        private boolean useByDefault;
    }

}
