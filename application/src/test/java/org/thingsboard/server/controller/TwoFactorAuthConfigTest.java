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

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFactorAuthConfigManager;
import org.thingsboard.server.common.data.security.model.mfa.TwoFactorAuthSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.SmsTwoFactorAuthAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFactorAuthAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFactorAuthAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.SmsTwoFactorAuthProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TotpTwoFactorAuthProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFactorAuthProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFactorAuthProviderType;
import org.thingsboard.server.service.security.auth.mfa.provider.impl.OtpBasedTwoFactorAuthProvider;
import org.thingsboard.server.service.security.auth.mfa.provider.impl.TotpTwoFactorAuthProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class TwoFactorAuthConfigTest extends AbstractControllerTest {

    @SpyBean
    private TotpTwoFactorAuthProvider totpTwoFactorAuthProvider;
    @MockBean
    private SmsService smsService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private TwoFactorAuthConfigManager twoFactorAuthConfigManager;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();
    }

    @After
    public void afterEach() {
        twoFactorAuthConfigManager.deleteTwoFaSettings(TenantId.SYS_TENANT_ID);
        twoFactorAuthConfigManager.deleteTwoFaSettings(tenantId);
    }


    @Test
    public void testSaveTwoFaSettings() throws Exception {
        loginSysAdmin();
        testSaveTestTwoFaSettings();

        loginTenantAdmin();
        testSaveTestTwoFaSettings();
    }

    private void testSaveTestTwoFaSettings() throws Exception {
        TotpTwoFactorAuthProviderConfig totpTwoFaProviderConfig = new TotpTwoFactorAuthProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("tb");
        SmsTwoFactorAuthProviderConfig smsTwoFaProviderConfig = new SmsTwoFactorAuthProviderConfig();
        smsTwoFaProviderConfig.setSmsVerificationMessageTemplate("${verificationCode}");
        smsTwoFaProviderConfig.setVerificationCodeLifetime(60);

        TwoFactorAuthSettings twoFaSettings = new TwoFactorAuthSettings();
        twoFaSettings.setProviders(List.of(totpTwoFaProviderConfig, smsTwoFaProviderConfig));
        twoFaSettings.setVerificationCodeSendRateLimit("1:60");
        twoFaSettings.setVerificationCodeCheckRateLimit("3:900");
        twoFaSettings.setMaxVerificationFailuresBeforeUserLockout(10);
        twoFaSettings.setTotalAllowedTimeForVerification(3600);

        doPost("/api/2fa/settings", twoFaSettings).andExpect(status().isOk());

        TwoFactorAuthSettings savedTwoFaSettings = readResponse(doGet("/api/2fa/settings").andExpect(status().isOk()), TwoFactorAuthSettings.class);

        assertThat(savedTwoFaSettings.getProviders()).hasSize(2);
        assertThat(savedTwoFaSettings.getProviders()).contains(totpTwoFaProviderConfig, smsTwoFaProviderConfig);
    }

    @Test
    public void testSaveTwoFaSettings_validationError() throws Exception {
        loginTenantAdmin();

        TwoFactorAuthSettings twoFaSettings = new TwoFactorAuthSettings();
        twoFaSettings.setProviders(Collections.emptyList());
        twoFaSettings.setVerificationCodeSendRateLimit("ab:aba");
        twoFaSettings.setVerificationCodeCheckRateLimit("0:12");
        twoFaSettings.setMaxVerificationFailuresBeforeUserLockout(-1);
        twoFaSettings.setTotalAllowedTimeForVerification(0);

        String errorMessage = getErrorMessage(doPost("/api/2fa/settings", twoFaSettings)
                .andExpect(status().isBadRequest()));

        assertThat(errorMessage).contains(
                "verification code send rate limit configuration is invalid",
                "verification code check rate limit configuration is invalid",
                "maximum number of verification failure before user lockout must be positive",
                "total amount of time allotted for verification must be greater than 0"
        );

        twoFaSettings.setUseSystemTwoFactorAuthSettings(true);
        doPost("/api/2fa/settings", twoFaSettings)
                .andExpect(status().isOk());

        twoFaSettings.setVerificationCodeSendRateLimit(null);
        twoFaSettings.setVerificationCodeCheckRateLimit(null);
        twoFaSettings.setMaxVerificationFailuresBeforeUserLockout(0);
        twoFaSettings.setTotalAllowedTimeForVerification(null);

        doPost("/api/2fa/settings", twoFaSettings)
                .andExpect(status().isOk());
    }

    @Test
    public void testGetTwoFaSettings_useSysadminSettingsAsDefault() throws Exception {
        loginSysAdmin();
        TwoFactorAuthSettings sysadminTwoFaSettings = new TwoFactorAuthSettings();
        TotpTwoFactorAuthProviderConfig totpTwoFaProviderConfig = new TotpTwoFactorAuthProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("tb");
        sysadminTwoFaSettings.setProviders(Collections.singletonList(totpTwoFaProviderConfig));
        sysadminTwoFaSettings.setMaxVerificationFailuresBeforeUserLockout(25);
        doPost("/api/2fa/settings", sysadminTwoFaSettings).andExpect(status().isOk());

        loginTenantAdmin();
        TwoFactorAuthSettings tenantTwoFaSettings = new TwoFactorAuthSettings();
        tenantTwoFaSettings.setUseSystemTwoFactorAuthSettings(true);
        tenantTwoFaSettings.setProviders(Collections.emptyList());
        doPost("/api/2fa/settings", tenantTwoFaSettings).andExpect(status().isOk());
        TwoFactorAuthSettings twoFaSettings = readResponse(doGet("/api/2fa/settings").andExpect(status().isOk()), TwoFactorAuthSettings.class);
        assertThat(twoFaSettings).isEqualTo(tenantTwoFaSettings);

        doPost("/api/2fa/account/config/generate?providerType=TOTP")
                .andExpect(status().isOk());

        tenantTwoFaSettings.setUseSystemTwoFactorAuthSettings(false);
        tenantTwoFaSettings.setProviders(Collections.emptyList());
        tenantTwoFaSettings.setMaxVerificationFailuresBeforeUserLockout(10);
        doPost("/api/2fa/settings", tenantTwoFaSettings).andExpect(status().isOk());
        twoFaSettings = readResponse(doGet("/api/2fa/settings").andExpect(status().isOk()), TwoFactorAuthSettings.class);
        assertThat(twoFaSettings).isEqualTo(tenantTwoFaSettings);

        assertThat(getErrorMessage(doPost("/api/2fa/account/config/generate?providerType=TOTP")
                .andExpect(status().isBadRequest()))).containsIgnoringCase("provider is not configured");

        loginSysAdmin();
        sysadminTwoFaSettings.setProviders(Collections.emptyList());
        doPost("/api/2fa/settings", sysadminTwoFaSettings).andExpect(status().isOk());
        loginTenantAdmin();
        tenantTwoFaSettings.setUseSystemTwoFactorAuthSettings(true);
        tenantTwoFaSettings.setProviders(Collections.singletonList(totpTwoFaProviderConfig));
        doPost("/api/2fa/settings", tenantTwoFaSettings).andExpect(status().isOk());

        assertThat(getErrorMessage(doPost("/api/2fa/account/config/generate?providerType=TOTP")
                .andExpect(status().isBadRequest()))).containsIgnoringCase("provider is not configured");

        tenantTwoFaSettings.setUseSystemTwoFactorAuthSettings(false);
        doPost("/api/2fa/settings", tenantTwoFaSettings).andExpect(status().isOk());

        doPost("/api/2fa/account/config/generate?providerType=TOTP")
                .andExpect(status().isOk());

        loginSysAdmin();
        twoFaSettings = readResponse(doGet("/api/2fa/settings").andExpect(status().isOk()), TwoFactorAuthSettings.class);
        assertThat(twoFaSettings).isEqualTo(sysadminTwoFaSettings);
    }

    @Test
    public void testSaveTotpTwoFaProviderConfig_validationError() throws Exception {
        TotpTwoFactorAuthProviderConfig invalidTotpTwoFaProviderConfig = new TotpTwoFactorAuthProviderConfig();
        invalidTotpTwoFaProviderConfig.setIssuerName("   ");

        String errorResponse = saveTwoFaSettingsAndGetError(invalidTotpTwoFaProviderConfig);
        assertThat(errorResponse).containsIgnoringCase("issuer name must not be blank");
    }

    @Test
    public void testSaveSmsTwoFaProviderConfig_validationError() throws Exception {
        SmsTwoFactorAuthProviderConfig invalidSmsTwoFaProviderConfig = new SmsTwoFactorAuthProviderConfig();
        invalidSmsTwoFaProviderConfig.setSmsVerificationMessageTemplate("does not contain verification code");
        invalidSmsTwoFaProviderConfig.setVerificationCodeLifetime(60);

        String errorResponse = saveTwoFaSettingsAndGetError(invalidSmsTwoFaProviderConfig);
        assertThat(errorResponse).containsIgnoringCase("must contain verification code");

        invalidSmsTwoFaProviderConfig.setSmsVerificationMessageTemplate(null);
        invalidSmsTwoFaProviderConfig.setVerificationCodeLifetime(0);
        errorResponse = saveTwoFaSettingsAndGetError(invalidSmsTwoFaProviderConfig);
        assertThat(errorResponse).containsIgnoringCase("verification message template is required");
        assertThat(errorResponse).containsIgnoringCase("verification code lifetime is required");
    }

    private String saveTwoFaSettingsAndGetError(TwoFactorAuthProviderConfig invalidTwoFaProviderConfig) throws Exception {
        TwoFactorAuthSettings twoFaSettings = new TwoFactorAuthSettings();
        twoFaSettings.setProviders(Collections.singletonList(invalidTwoFaProviderConfig));

        return getErrorMessage(doPost("/api/2fa/settings", twoFaSettings)
                .andExpect(status().isBadRequest()));
    }

    @Test
    public void testSaveTwoFaAccountConfig_providerNotConfigured() throws Exception {
        configureSmsTwoFaProvider("${verificationCode}");

        loginTenantAdmin();

        TwoFactorAuthProviderType notConfiguredProviderType = TwoFactorAuthProviderType.TOTP;
        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config/generate?providerType=" + notConfiguredProviderType)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("provider is not configured");

        TotpTwoFactorAuthAccountConfig notConfiguredProviderAccountConfig = new TotpTwoFactorAuthAccountConfig();
        notConfiguredProviderAccountConfig.setAuthUrl("otpauth://totp/aba:aba?issuer=aba&secret=ABA");
        errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", notConfiguredProviderAccountConfig));
        assertThat(errorMessage).containsIgnoringCase("provider is not configured");
    }

    @Test
    public void testGenerateTotpTwoFaAccountConfig() throws Exception {
        TotpTwoFactorAuthProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        assertThat(readResponse(doGet("/api/2fa/account/config").andExpect(status().isOk()), String.class)).isNullOrEmpty();
        generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);
    }

    @Test
    public void testSubmitTotpTwoFaAccountConfig() throws Exception {
        TotpTwoFactorAuthProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFactorAuthAccountConfig generatedTotpTwoFaAccountConfig = generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);
        doPost("/api/2fa/account/config/submit", generatedTotpTwoFaAccountConfig).andExpect(status().isOk());
        verify(totpTwoFactorAuthProvider).prepareVerificationCode(argThat(user -> user.getEmail().equals(TENANT_ADMIN_EMAIL)),
                eq(totpTwoFaProviderConfig), eq(generatedTotpTwoFaAccountConfig));
    }

    @Test
    public void testSubmitTotpTwoFaAccountConfig_validationError() throws Exception {
        configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFactorAuthAccountConfig totpTwoFaAccountConfig = new TotpTwoFactorAuthAccountConfig();
        totpTwoFaAccountConfig.setAuthUrl(null);

        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", totpTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("otp auth url cannot be blank");

        totpTwoFaAccountConfig.setAuthUrl("otpauth://totp/T B: aba");
        errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", totpTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("otp auth url is invalid");

        totpTwoFaAccountConfig.setAuthUrl("otpauth://totp/ThingsBoard%20(Tenant):tenant@thingsboard.org?issuer=ThingsBoard+%28Tenant%29&secret=FUNBIM3CXFNNGQR6ZIPVWHP65PPFWDII");
        doPost("/api/2fa/account/config/submit", totpTwoFaAccountConfig)
                .andExpect(status().isOk());
    }

    @Test
    public void testVerifyAndSaveTotpTwoFaAccountConfig() throws Exception {
        TotpTwoFactorAuthProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFactorAuthAccountConfig generatedTotpTwoFaAccountConfig = generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);

        String secret = UriComponentsBuilder.fromUriString(generatedTotpTwoFaAccountConfig.getAuthUrl()).build()
                .getQueryParams().getFirst("secret");
        String correctVerificationCode = new Totp(secret).now();

        doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, generatedTotpTwoFaAccountConfig)
                .andExpect(status().isOk());

        TwoFactorAuthAccountConfig twoFaAccountConfig = readResponse(doGet("/api/2fa/account/config").andExpect(status().isOk()), TwoFactorAuthAccountConfig.class);
        assertThat(twoFaAccountConfig).isEqualTo(generatedTotpTwoFaAccountConfig);
    }

    @Test
    public void testVerifyAndSaveTotpTwoFaAccountConfig_incorrectVerificationCode() throws Exception {
        TotpTwoFactorAuthProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFactorAuthAccountConfig generatedTotpTwoFaAccountConfig = generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);

        String incorrectVerificationCode = "100000";
        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config?verificationCode=" + incorrectVerificationCode, generatedTotpTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));

        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");
    }

    private TotpTwoFactorAuthAccountConfig generateTotpTwoFaAccountConfig(TotpTwoFactorAuthProviderConfig totpTwoFaProviderConfig) throws Exception {
        TwoFactorAuthAccountConfig generatedTwoFaAccountConfig = readResponse(doPost("/api/2fa/account/config/generate?providerType=TOTP")
                .andExpect(status().isOk()), TwoFactorAuthAccountConfig.class);
        assertThat(generatedTwoFaAccountConfig).isInstanceOf(TotpTwoFactorAuthAccountConfig.class);

        assertThat(((TotpTwoFactorAuthAccountConfig) generatedTwoFaAccountConfig)).satisfies(accountConfig -> {
            UriComponents otpAuthUrl = UriComponentsBuilder.fromUriString(accountConfig.getAuthUrl()).build();
            assertThat(otpAuthUrl.getScheme()).isEqualTo("otpauth");
            assertThat(otpAuthUrl.getHost()).isEqualTo("totp");
            assertThat(otpAuthUrl.getQueryParams().getFirst("issuer")).isEqualTo(totpTwoFaProviderConfig.getIssuerName());
            assertThat(otpAuthUrl.getPath()).isEqualTo("/%s:%s", totpTwoFaProviderConfig.getIssuerName(), TENANT_ADMIN_EMAIL);
            assertThat(otpAuthUrl.getQueryParams().getFirst("secret")).satisfies(secretKey -> {
                assertDoesNotThrow(() -> Base32.decode(secretKey));
            });
        });
        return (TotpTwoFactorAuthAccountConfig) generatedTwoFaAccountConfig;
    }

    @Test
    public void testGetTwoFaAccountConfig_whenProviderNotConfigured() throws Exception {
        testVerifyAndSaveTotpTwoFaAccountConfig();
        assertThat(readResponse(doGet("/api/2fa/account/config").andExpect(status().isOk()),
                TotpTwoFactorAuthAccountConfig.class)).isNotNull();

        loginSysAdmin();

        saveProvidersConfigs();

        assertThat(readResponse(doGet("/api/2fa/account/config").andExpect(status().isOk()), String.class))
                .isNullOrEmpty();
    }

    @Test
    public void testGenerateSmsTwoFaAccountConfig() throws Exception {
        configureSmsTwoFaProvider("${verificationCode}");
        doPost("/api/2fa/account/config/generate?providerType=SMS")
                .andExpect(status().isOk());
    }

    @Test
    public void testSubmitSmsTwoFaAccountConfig() throws Exception {
        String verificationMessageTemplate = "Here is your verification code: ${verificationCode}";
        configureSmsTwoFaProvider(verificationMessageTemplate);

        loginTenantAdmin();

        SmsTwoFactorAuthAccountConfig smsTwoFaAccountConfig = new SmsTwoFactorAuthAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38054159785");

        doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig).andExpect(status().isOk());

        String verificationCode = cacheManager.getCache(CacheConstants.TWO_FA_VERIFICATION_CODES_CACHE)
                .get(tenantAdminUserId, OtpBasedTwoFactorAuthProvider.Otp.class).getValue();

        verify(smsService).sendSms(eq(tenantId), any(), argThat(phoneNumbers -> {
            return phoneNumbers[0].equals(smsTwoFaAccountConfig.getPhoneNumber());
        }), eq("Here is your verification code: " + verificationCode));
    }

    @Test
    public void testSubmitSmsTwoFaAccountConfig_validationError() throws Exception {
        configureSmsTwoFaProvider("${verificationCode}");

        SmsTwoFactorAuthAccountConfig smsTwoFaAccountConfig = new SmsTwoFactorAuthAccountConfig();
        String blankPhoneNumber = "";
        smsTwoFaAccountConfig.setPhoneNumber(blankPhoneNumber);

        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("phone number cannot be blank");

        String nonE164PhoneNumber = "8754868";
        smsTwoFaAccountConfig.setPhoneNumber(nonE164PhoneNumber);

        errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("phone number is not of E.164 format");
    }

    @Test
    public void testVerifyAndSaveSmsTwoFaAccountConfig() throws Exception {
        configureSmsTwoFaProvider("${verificationCode}");

        loginTenantAdmin();

        SmsTwoFactorAuthAccountConfig smsTwoFaAccountConfig = new SmsTwoFactorAuthAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38051889445");

        ArgumentCaptor<String> verificationCodeCaptor = ArgumentCaptor.forClass(String.class);
        doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig).andExpect(status().isOk());

        verify(smsService).sendSms(eq(tenantId), any(), argThat(phoneNumbers -> {
            return phoneNumbers[0].equals(smsTwoFaAccountConfig.getPhoneNumber());
        }), verificationCodeCaptor.capture());

        String correctVerificationCode = verificationCodeCaptor.getValue();
        doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, smsTwoFaAccountConfig)
                .andExpect(status().isOk());

        TwoFactorAuthAccountConfig accountConfig = readResponse(doGet("/api/2fa/account/config").andExpect(status().isOk()), TwoFactorAuthAccountConfig.class);
        assertThat(accountConfig).isEqualTo(smsTwoFaAccountConfig);
    }

    @Test
    public void testVerifyAndSaveSmsTwoFaAccountConfig_incorrectVerificationCode() throws Exception {
        configureSmsTwoFaProvider("${verificationCode}");

        loginTenantAdmin();

        SmsTwoFactorAuthAccountConfig smsTwoFaAccountConfig = new SmsTwoFactorAuthAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38051889445");

        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config?verificationCode=100000", smsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");
    }

    @Test
    public void testVerifyAndSaveSmsTwoFaAccountConfig_differentAccountConfigs() throws Exception {
        configureSmsTwoFaProvider("${verificationCode}");
        loginTenantAdmin();

        SmsTwoFactorAuthAccountConfig initialSmsTwoFaAccountConfig = new SmsTwoFactorAuthAccountConfig();
        initialSmsTwoFaAccountConfig.setPhoneNumber("+38051889445");

        ArgumentCaptor<String> verificationCodeCaptor = ArgumentCaptor.forClass(String.class);
        doPost("/api/2fa/account/config/submit", initialSmsTwoFaAccountConfig).andExpect(status().isOk());

        verify(smsService).sendSms(eq(tenantId), any(), argThat(phoneNumbers -> {
            return phoneNumbers[0].equals(initialSmsTwoFaAccountConfig.getPhoneNumber());
        }), verificationCodeCaptor.capture());

        String correctVerificationCode = verificationCodeCaptor.getValue();

        SmsTwoFactorAuthAccountConfig anotherSmsTwoFaAccountConfig = new SmsTwoFactorAuthAccountConfig();
        anotherSmsTwoFaAccountConfig.setPhoneNumber("+38111111111");
        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, anotherSmsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");

        doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, initialSmsTwoFaAccountConfig)
                .andExpect(status().isOk());
        TwoFactorAuthAccountConfig accountConfig = readResponse(doGet("/api/2fa/account/config").andExpect(status().isOk()), TwoFactorAuthAccountConfig.class);
        assertThat(accountConfig).isEqualTo(initialSmsTwoFaAccountConfig);
    }

    private TotpTwoFactorAuthProviderConfig configureTotpTwoFaProvider() throws Exception {
        TotpTwoFactorAuthProviderConfig totpTwoFaProviderConfig = new TotpTwoFactorAuthProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("tb");

        saveProvidersConfigs(totpTwoFaProviderConfig);
        return totpTwoFaProviderConfig;
    }

    private SmsTwoFactorAuthProviderConfig configureSmsTwoFaProvider(String verificationMessageTemplate) throws Exception {
        SmsTwoFactorAuthProviderConfig smsTwoFaProviderConfig = new SmsTwoFactorAuthProviderConfig();
        smsTwoFaProviderConfig.setSmsVerificationMessageTemplate(verificationMessageTemplate);
        smsTwoFaProviderConfig.setVerificationCodeLifetime(60);

        saveProvidersConfigs(smsTwoFaProviderConfig);
        return smsTwoFaProviderConfig;
    }

    private void saveProvidersConfigs(TwoFactorAuthProviderConfig... providerConfigs) throws Exception {
        TwoFactorAuthSettings twoFaSettings = new TwoFactorAuthSettings();

        twoFaSettings.setProviders(Arrays.stream(providerConfigs).collect(Collectors.toList()));
        doPost("/api/2fa/settings", twoFaSettings).andExpect(status().isOk());
    }

    @Test
    public void testIsTwoFaEnabled() throws Exception {
        configureSmsTwoFaProvider("${verificationCode}");
        SmsTwoFactorAuthAccountConfig accountConfig = new SmsTwoFactorAuthAccountConfig();
        accountConfig.setPhoneNumber("+38050505050");
        twoFactorAuthConfigManager.saveTwoFaAccountConfig(tenantId, tenantAdminUserId, accountConfig);

        assertThat(twoFactorAuthConfigManager.isTwoFaEnabled(tenantId, tenantAdminUserId)).isTrue();
    }

    @Test
    public void testDeleteTwoFaAccountConfig() throws Exception {
        configureSmsTwoFaProvider("${verificationCode}");
        SmsTwoFactorAuthAccountConfig accountConfig = new SmsTwoFactorAuthAccountConfig();
        accountConfig.setPhoneNumber("+38050505050");

        loginTenantAdmin();

        twoFactorAuthConfigManager.saveTwoFaAccountConfig(tenantId, tenantAdminUserId, accountConfig);

        TwoFactorAuthAccountConfig savedAccountConfig = readResponse(doGet("/api/2fa/account/config").andExpect(status().isOk()), TwoFactorAuthAccountConfig.class);
        assertThat(savedAccountConfig).isEqualTo(accountConfig);

        doDelete("/api/2fa/account/config").andExpect(status().isOk());

        assertThat(readResponse(doGet("/api/2fa/account/config").andExpect(status().isOk()), String.class))
                .isNullOrEmpty();
    }

}
