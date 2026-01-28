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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.AccountTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;

@RestController
@RequestMapping("/api/2fa")
@TbCoreComponent
@RequiredArgsConstructor
public class TwoFactorAuthConfigController extends BaseController {

    private final TwoFaConfigManager twoFaConfigManager;
    private final TwoFactorAuthService twoFactorAuthService;

    @ApiOperation(value = "Get account 2FA settings (getAccountTwoFaSettings)",
            notes = "Get user's account 2FA configuration. Configuration contains configs for different 2FA providers." + NEW_LINE +
                    "Example:\n" +
                    "```\n{\n  \"configs\": {\n" +
                    "    \"EMAIL\": {\n      \"providerType\": \"EMAIL\",\n      \"useByDefault\": true,\n      \"email\": \"tenant@thingsboard.org\"\n    },\n" +
                    "    \"TOTP\": {\n      \"providerType\": \"TOTP\",\n      \"useByDefault\": false,\n      \"authUrl\": \"otpauth://totp/TB%202FA:tenant@thingsboard.org?issuer=TB+2FA&secret=P6Z2TLYTASOGP6LCJZAD24ETT5DACNNX\"\n    },\n" +
                    "    \"SMS\": {\n      \"providerType\": \"SMS\",\n      \"useByDefault\": false,\n      \"phoneNumber\": \"+380501253652\"\n    }\n" +
                    "  }\n}\n```" +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @GetMapping("/account/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER', 'MFA_CONFIGURATION_TOKEN')")
    public AccountTwoFaSettings getAccountTwoFaSettings() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return twoFaConfigManager.getAccountTwoFaSettings(user.getTenantId(), user).orElse(null);
    }

    @ApiOperation(value = "Generate 2FA account config (generateTwoFaAccountConfig)",
            notes = "Generate new 2FA account config template for specified provider type. " + NEW_LINE +
                    "For TOTP, this will return a corresponding account config template " +
                    "with a generated OTP auth URL (with new random secret key for each API call) that can be then " +
                    "converted to a QR code to scan with an authenticator app. Example:\n" +
                    "```\n{\n" +
                    "  \"providerType\": \"TOTP\",\n" +
                    "  \"useByDefault\": false,\n" +
                    "  \"authUrl\": \"otpauth://totp/TB%202FA:tenant@thingsboard.org?issuer=TB+2FA&secret=PNJDNWJVAK4ZTUYT7RFGPQLXA7XGU7PX\"\n" +
                    "}\n```" + NEW_LINE +
                    "For EMAIL, the generated config will contain email from user's account:\n" +
                    "```\n{\n" +
                    "  \"providerType\": \"EMAIL\",\n" +
                    "  \"useByDefault\": false,\n" +
                    "  \"email\": \"tenant@thingsboard.org\"\n" +
                    "}\n```" + NEW_LINE +
                    "For SMS 2FA this method will just return a config with empty/default values as there is nothing to generate/preset:\n" +
                    "```\n{\n" +
                    "  \"providerType\": \"SMS\",\n" +
                    "  \"useByDefault\": false,\n" +
                    "  \"phoneNumber\": null\n" +
                    "}\n```" + NEW_LINE +
                    "Will throw an error (Bad Request) if the provider is not configured for usage. " +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PostMapping("/account/config/generate")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER', 'MFA_CONFIGURATION_TOKEN')")
    public TwoFaAccountConfig generateTwoFaAccountConfig(@Parameter(description = "2FA provider type to generate new account config for", schema = @Schema(defaultValue = "TOTP", requiredMode = Schema.RequiredMode.REQUIRED))
                                                         @RequestParam TwoFaProviderType providerType) throws Exception {
        SecurityUser user = getCurrentUser();
        return twoFactorAuthService.generateNewAccountConfig(user, providerType);
    }

    @ApiOperation(value = "Submit 2FA account config (submitTwoFaAccountConfig)",
            notes = "Submit 2FA account config to prepare for a future verification. " +
                    "Basically, this method will send a verification code for a given account config, if this has " +
                    "sense for a chosen 2FA provider. This code is needed to then verify and save the account config." + NEW_LINE +
                    "Example of EMAIL 2FA account config:\n" +
                    "```\n{\n" +
                    "  \"providerType\": \"EMAIL\",\n" +
                    "  \"useByDefault\": true,\n" +
                    "  \"email\": \"separate-email-for-2fa@thingsboard.org\"\n" +
                    "}\n```" + NEW_LINE +
                    "Example of SMS 2FA account config:\n" +
                    "```\n{\n" +
                    "  \"providerType\": \"SMS\",\n" +
                    "  \"useByDefault\": false,\n" +
                    "  \"phoneNumber\": \"+38012312321\"\n" +
                    "}\n```" + NEW_LINE +
                    "For TOTP this method does nothing." + NEW_LINE +
                    "Will throw an error (Bad Request) if submitted account config is not valid, " +
                    "or if the provider is not configured for usage. " +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PostMapping("/account/config/submit")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER', 'MFA_CONFIGURATION_TOKEN')")
    public void submitTwoFaAccountConfig(@Valid @RequestBody TwoFaAccountConfig accountConfig) throws Exception {
        SecurityUser user = getCurrentUser();
        twoFactorAuthService.prepareVerificationCode(user, accountConfig, false);
    }

    @ApiOperation(value = "Verify and save 2FA account config (verifyAndSaveTwoFaAccountConfig)",
            notes = "Checks the verification code for submitted config, and if it is correct, saves the provided account config. " + NEW_LINE +
                    "Returns whole account's 2FA settings object.\n" +
                    "Will throw an error (Bad Request) if the provider is not configured for usage. " +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PostMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER', 'MFA_CONFIGURATION_TOKEN')")
    public AccountTwoFaSettings verifyAndSaveTwoFaAccountConfig(@Valid @RequestBody TwoFaAccountConfig accountConfig,
                                                                @RequestParam(required = false) String verificationCode) throws Exception {
        SecurityUser user = getCurrentUser();
        if (twoFaConfigManager.getTwoFaAccountConfig(user.getTenantId(), user, accountConfig.getProviderType()).isPresent()) {
            throw new IllegalArgumentException("2FA provider is already configured");
        }

        boolean verificationSuccess;
        if (accountConfig.getProviderType() != TwoFaProviderType.BACKUP_CODE) {
            verificationSuccess = twoFactorAuthService.checkVerificationCode(user, verificationCode, accountConfig, false);
        } else {
            verificationSuccess = true;
        }
        if (verificationSuccess) {
            return twoFaConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user, accountConfig);
        } else {
            throw new IllegalArgumentException("Verification code is incorrect");
        }
    }

    @ApiOperation(value = "Update 2FA account config (updateTwoFaAccountConfig)", notes =
            "Update config for a given provider type. \n" +
            "Update request example:\n" +
            "```\n{\n  \"useByDefault\": true\n}\n```\n" +
            "Returns whole account's 2FA settings object.\n" +
            ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PutMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public AccountTwoFaSettings updateTwoFaAccountConfig(@RequestParam TwoFaProviderType providerType,
                                                         @RequestBody TwoFaAccountConfigUpdateRequest updateRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();

        TwoFaAccountConfig accountConfig = twoFaConfigManager.getTwoFaAccountConfig(user.getTenantId(), user, providerType)
                .orElseThrow(() -> new IllegalArgumentException("Config for " + providerType + " 2FA provider not found"));
        accountConfig.setUseByDefault(updateRequest.isUseByDefault());
        return twoFaConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user, accountConfig);
    }

    @ApiOperation(value = "Delete 2FA account config (deleteTwoFaAccountConfig)", notes =
            "Delete 2FA config for a given 2FA provider type. \n" +
            "Returns whole account's 2FA settings object.\n" +
            ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @DeleteMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public AccountTwoFaSettings deleteTwoFaAccountConfig(@RequestParam TwoFaProviderType providerType) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return twoFaConfigManager.deleteTwoFaAccountConfig(user.getTenantId(), user, providerType);
    }

    @ApiOperation(value = "Get available 2FA providers (getAvailableTwoFaProviders)", notes =
            "Get the list of provider types available for user to use (the ones configured by tenant or sysadmin).\n" +
            "Example of response:\n" +
            "```\n[\n  \"TOTP\",\n  \"EMAIL\",\n  \"SMS\"\n]\n```" +
            ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER
    )
    @GetMapping("/providers")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER', 'MFA_CONFIGURATION_TOKEN')")
    public List<TwoFaProviderType> getAvailableTwoFaProviders() throws ThingsboardException {
        return twoFaConfigManager.getPlatformTwoFaSettings(getTenantId(), true)
                .map(PlatformTwoFaSettings::getProviders).orElse(Collections.emptyList()).stream()
                .map(TwoFaProviderConfig::getProviderType)
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Get platform 2FA settings (getPlatformTwoFaSettings)",
            notes = "Get platform settings for 2FA. The settings are described for savePlatformTwoFaSettings API method. " +
                    "If 2FA is not configured, then an empty response will be returned." +
                    ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    public PlatformTwoFaSettings getPlatformTwoFaSettings() throws ThingsboardException {
        return twoFaConfigManager.getPlatformTwoFaSettings(getTenantId(), false).orElse(null);
    }

    @ApiOperation(value = "Save platform 2FA settings (savePlatformTwoFaSettings)",
            notes = "Save 2FA settings for platform. The settings have following properties:\n" +
                    "- `providers` - the list of 2FA providers' configs. Users will only be allowed to use 2FA providers from this list. \n\n" +
                    "- `minVerificationCodeSendPeriod` - minimal period in seconds to wait after verification code send request to send next request. \n" +
                    "- `verificationCodeCheckRateLimit` - rate limit configuration for verification code checking.\n" +
                    "The format is standard: 'amountOfRequests:periodInSeconds'. The value of '1:60' would limit verification " +
                    "code checking requests to one per minute.\n" +
                    "- `maxVerificationFailuresBeforeUserLockout` - maximum number of verification failures before a user gets disabled.\n" +
                    "- `totalAllowedTimeForVerification` - total amount of time in seconds allotted for verification. " +
                    "Basically, this property sets a lifetime for pre-verification token. If not set, default value of 30 minutes is used.\n" + NEW_LINE +
                    "TOTP 2FA provider config has following settings:\n" +
                    "- `issuerName` - issuer name that will be displayed in an authenticator app near a username. Must not be blank.\n\n" +
                    "For SMS 2FA provider:\n" +
                    "- `smsVerificationMessageTemplate` - verification message template.  Available template variables " +
                    "are ${code} and ${userEmail}. It must not be blank and must contain verification code variable.\n" +
                    "- `verificationCodeLifetime` - verification code lifetime in seconds. Required to be positive.\n\n" +
                    "For EMAIL provider type:\n" +
                    "- `verificationCodeLifetime` - the same as for SMS." + NEW_LINE +
                    "Example of the settings:\n" +
                    "```\n{\n" +
                    "  \"providers\": [\n" +
                    "    {\n" +
                    "      \"providerType\": \"TOTP\",\n" +
                    "      \"issuerName\": \"TB\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"providerType\": \"EMAIL\",\n" +
                    "      \"verificationCodeLifetime\": 60\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"providerType\": \"SMS\",\n" +
                    "      \"verificationCodeLifetime\": 60,\n" +
                    "      \"smsVerificationMessageTemplate\": \"Here is your verification code: ${code}\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"minVerificationCodeSendPeriod\": 60,\n" +
                    "  \"verificationCodeCheckRateLimit\": \"3:900\",\n" +
                    "  \"maxVerificationFailuresBeforeUserLockout\": 10,\n" +
                    "  \"totalAllowedTimeForVerification\": 600\n" +
                    "}\n```" +
                    ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    public PlatformTwoFaSettings savePlatformTwoFaSettings(@Parameter(description = "Settings value", required = true)
                                                           @RequestBody PlatformTwoFaSettings twoFaSettings) throws ThingsboardException {
        return twoFaConfigManager.savePlatformTwoFaSettings(getTenantId(), twoFaSettings);
    }

    @Data
    public static class TwoFaAccountConfigUpdateRequest {
        private boolean useByDefault;
    }

}
