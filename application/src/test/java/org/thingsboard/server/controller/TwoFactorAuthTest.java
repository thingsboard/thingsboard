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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.EmailTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.EmailTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.SmsTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TotpTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class TwoFactorAuthTest extends AbstractControllerTest {

    @Autowired
    private TwoFaConfigManager twoFaConfigManager;
    @SpyBean
    private TwoFactorAuthService twoFactorAuthService;
    @MockBean
    private SmsService smsService;
    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private UserService userService;

    private User user;
    private String username;
    private String password;

    @Before
    public void beforeEach() throws Exception {
        username = "mfa@tb.io";
        password = "psswrd";

        user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail(username);
        user.setTenantId(tenantId);

        loginSysAdmin();
        user = createUser(user, password);
        doNothing().when(twoFactorAuthService).checkProvider(any(), any());
    }

    @After
    public void afterEach() {
        twoFaConfigManager.deletePlatformTwoFaSettings(tenantId);
        twoFaConfigManager.deletePlatformTwoFaSettings(TenantId.SYS_TENANT_ID);
    }

    @Test
    public void testTwoFa_totp() throws Exception {
        TotpTwoFaAccountConfig totpTwoFaAccountConfig = configureTotpTwoFa();

        logInWithPreVerificationToken(username, password);

        doPost("/api/auth/2fa/verification/send?providerType=TOTP")
                .andExpect(status().isOk());

        String correctVerificationCode = getCorrectTotp(totpTwoFaAccountConfig);

        JsonNode tokenPair = readResponse(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + correctVerificationCode)
                .andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenPair, username);

        User currentUser = readResponse(doGet("/api/auth/user")
                .andExpect(status().isOk()), User.class);
        assertThat(currentUser.getId()).isEqualTo(user.getId());
    }

    @Test
    public void testTwoFa_sms() throws Exception {
        configureSmsTwoFa();

        logInWithPreVerificationToken(username, password);

        doPost("/api/auth/2fa/verification/send?providerType=SMS")
                .andExpect(status().isOk());

        ArgumentCaptor<String> verificationCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendSms(eq(tenantId), any(), any(), verificationCodeCaptor.capture());
        String correctVerificationCode = verificationCodeCaptor.getValue();

        JsonNode tokenPair = readResponse(doPost("/api/auth/2fa/verification/check?providerType=SMS&verificationCode=" + correctVerificationCode)
                .andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenPair, username);

        User currentUser = readResponse(doGet("/api/auth/user")
                .andExpect(status().isOk()), User.class);
        assertThat(currentUser.getId()).isEqualTo(user.getId());
    }

    @Test
    public void testTwoFaPreVerificationTokenLifetime() throws Exception {
        configureTotpTwoFa(twoFaSettings -> {
            twoFaSettings.setTotalAllowedTimeForVerification(65);
        });

        logInWithPreVerificationToken(username, password);

        await("expiration of the pre-verification token")
                .atLeast(Duration.ofSeconds(30).plusMillis(500))
                .atMost(Duration.ofSeconds(70))
                .untilAsserted(() -> {
                    doPost("/api/auth/2fa/verification/send?providerType=TOTP")
                            .andExpect(status().isUnauthorized());
                });
    }

    @Test
    public void testCheckVerificationCode_userBlocked() throws Exception {
        configureTotpTwoFa(twoFaSettings -> {
            twoFaSettings.setMaxVerificationFailuresBeforeUserLockout(10);
        });

        logInWithPreVerificationToken(username, password);

        Stream.generate(() -> StringUtils.randomNumeric(6))
                .limit(9)
                .forEach(incorrectVerificationCode -> {
                    try {
                        String errorMessage = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + incorrectVerificationCode)
                                .andExpect(status().isBadRequest()));
                        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");
                    } catch (Exception e) {
                        fail();
                    }
                });

        String errorMessage = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + StringUtils.randomNumeric(6))
                .andExpect(status().isUnauthorized()));
        assertThat(errorMessage).containsIgnoringCase("account was locked due to exceeded 2fa verification attempts");

        errorMessage = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + StringUtils.randomNumeric(6))
                .andExpect(status().isUnauthorized()));
        assertThat(errorMessage).containsIgnoringCase("user is disabled");
    }

    @Test
    public void testSendVerificationCode_rateLimit() throws Exception {
        configureTotpTwoFa(twoFaSettings -> {
            twoFaSettings.setMinVerificationCodeSendPeriod(10);
        });

        logInWithPreVerificationToken(username, password);

        doPost("/api/auth/2fa/verification/send?providerType=TOTP")
                .andExpect(status().isOk());

        String rateLimitExceededError = getErrorMessage(doPost("/api/auth/2fa/verification/send?providerType=TOTP")
                .andExpect(status().isTooManyRequests()));
        assertThat(rateLimitExceededError).containsIgnoringCase("too many requests");

        await("verification code sending rate limit resetting")
                .atLeast(Duration.ofSeconds(8))
                .atMost(Duration.ofSeconds(12))
                .untilAsserted(() -> {
                    doPost("/api/auth/2fa/verification/send?providerType=TOTP")
                            .andExpect(status().isOk());
                });
    }

    @Test
    public void testCheckVerificationCode_rateLimit() throws Exception {
        TotpTwoFaAccountConfig totpTwoFaAccountConfig = configureTotpTwoFa(twoFaSettings -> {
            twoFaSettings.setVerificationCodeCheckRateLimit("3:10");
        });

        logInWithPreVerificationToken(username, password);

        for (int i = 0; i < 3; i++) {
            String incorrectVerificationCodeError = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=incorrect")
                    .andExpect(status().isBadRequest()));
            assertThat(incorrectVerificationCodeError).containsIgnoringCase("verification code is incorrect");
        }

        String rateLimitExceededError = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=incorrect")
                .andExpect(status().isTooManyRequests()));
        assertThat(rateLimitExceededError).containsIgnoringCase("too many requests");

        await("verification code checking rate limit resetting")
                .atLeast(Duration.ofSeconds(8))
                .atMost(Duration.ofSeconds(12))
                .untilAsserted(() -> {
                    String incorrectVerificationCodeError = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=incorrect")
                            .andExpect(status().isBadRequest()));
                    assertThat(incorrectVerificationCodeError).containsIgnoringCase("verification code is incorrect");
                });

        doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + getCorrectTotp(totpTwoFaAccountConfig))
                .andExpect(status().isOk());
    }

    @Test
    public void testCheckVerificationCode_invalidVerificationCode() throws Exception {
        configureTotpTwoFa();
        logInWithPreVerificationToken(username, password);

        for (String invalidVerificationCode : new String[]{"1234567", "ab1212", "12311 ", "oewkriwejqf"}) {
            String errorMessage = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + invalidVerificationCode)
                    .andExpect(status().isBadRequest()));
            assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");
        }
    }

    @Test
    public void testCheckVerificationCode_codeExpiration() throws Exception {
        configureSmsTwoFa(smsTwoFaProviderConfig -> {
            smsTwoFaProviderConfig.setVerificationCodeLifetime(10);
        });

        logInWithPreVerificationToken(username, password);

        ArgumentCaptor<String> verificationCodeCaptor = ArgumentCaptor.forClass(String.class);
        doPost("/api/auth/2fa/verification/send?providerType=SMS").andExpect(status().isOk());
        verify(smsService).sendSms(eq(tenantId), any(), any(), verificationCodeCaptor.capture());

        String correctVerificationCode = verificationCodeCaptor.getValue();

        await("verification code expiration")
                .pollDelay(10, TimeUnit.SECONDS)
                .atLeast(10, TimeUnit.SECONDS)
                .atMost(12, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String incorrectVerificationCodeError = getErrorMessage(doPost("/api/auth/2fa/verification/check?providerType=SMS&verificationCode=" + correctVerificationCode)
                            .andExpect(status().isBadRequest()));
                    assertThat(incorrectVerificationCodeError).containsIgnoringCase("verification code is incorrect");
                });
    }

    @Test
    public void testTwoFa_logLoginAction() throws Exception {
        TotpTwoFaAccountConfig totpTwoFaAccountConfig = configureTotpTwoFa();

        logInWithPreVerificationToken(username, password);
        await("async audit log saving").during(1, TimeUnit.SECONDS);

        doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=incorrect")
                .andExpect(status().isBadRequest());

        // there is the first login audit log after user activation
        await("async audit log saving").atMost(1, TimeUnit.SECONDS)
                .until(() -> getLogInAuditLogs().size() == 2);
        assertThat(getLogInAuditLogs().get(0)).satisfies(failedLogInAuditLog -> {
            assertThat(failedLogInAuditLog.getActionStatus()).isEqualTo(ActionStatus.FAILURE);
            assertThat(failedLogInAuditLog.getActionFailureDetails()).containsIgnoringCase("verification code is incorrect");
            assertThat(failedLogInAuditLog.getUserName()).isEqualTo(username);
        });

        doPost("/api/auth/2fa/verification/check?providerType=TOTP&verificationCode=" + getCorrectTotp(totpTwoFaAccountConfig))
                .andExpect(status().isOk());
        await("async audit log saving").atMost(1, TimeUnit.SECONDS)
                .until(() -> getLogInAuditLogs().size() == 3);
        assertThat(getLogInAuditLogs().get(0)).satisfies(successfulLogInAuditLog -> {
            assertThat(successfulLogInAuditLog.getActionStatus()).isEqualTo(ActionStatus.SUCCESS);
            assertThat(successfulLogInAuditLog.getUserName()).isEqualTo(username);
        });

        loginTenantAdmin();
        assertThat(doGet("/api/user/" + user.getId(), User.class).getAdditionalInfo()
                .get("lastLoginTs").asLong())
                .isGreaterThan(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(3));
    }

    private List<AuditLog> getLogInAuditLogs() {
        return auditLogService.findAuditLogsByTenantIdAndUserId(tenantId, user.getId(), List.of(ActionType.LOGIN),
                new TimePageLink(new PageLink(10, 0, null, new SortOrder("createdTime", SortOrder.Direction.DESC)), 0L, System.currentTimeMillis())).getData();
    }

    @Test
    public void testAuthWithoutTwoFaAccountConfig() throws ThingsboardException {
        configureTotpTwoFa();
        twoFaConfigManager.deleteTwoFaAccountConfig(tenantId, user.getId(), TwoFaProviderType.TOTP);

        assertDoesNotThrow(() -> {
            login(username, password);
        });
    }

    @Test
    public void testTwoFa_multipleProviders() throws Exception {
        PlatformTwoFaSettings platformTwoFaSettings = new PlatformTwoFaSettings();

        TotpTwoFaProviderConfig totpTwoFaProviderConfig = new TotpTwoFaProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("TB");

        SmsTwoFaProviderConfig smsTwoFaProviderConfig = new SmsTwoFaProviderConfig();
        smsTwoFaProviderConfig.setVerificationCodeLifetime(60);
        smsTwoFaProviderConfig.setSmsVerificationMessageTemplate("${code}");

        EmailTwoFaProviderConfig emailTwoFaProviderConfig = new EmailTwoFaProviderConfig();
        emailTwoFaProviderConfig.setVerificationCodeLifetime(60);

        platformTwoFaSettings.setProviders(List.of(totpTwoFaProviderConfig, smsTwoFaProviderConfig, emailTwoFaProviderConfig));
        platformTwoFaSettings.setMinVerificationCodeSendPeriod(5);
        platformTwoFaSettings.setTotalAllowedTimeForVerification(100);
        twoFaConfigManager.savePlatformTwoFaSettings(TenantId.SYS_TENANT_ID, platformTwoFaSettings);

        User twoFaUser = new User();
        twoFaUser.setAuthority(Authority.TENANT_ADMIN);
        twoFaUser.setTenantId(tenantId);
        twoFaUser.setEmail("2fa@thingsboard.org");
        twoFaUser = createUserAndLogin(twoFaUser, "12345678");

        TotpTwoFaAccountConfig totpTwoFaAccountConfig = (TotpTwoFaAccountConfig) twoFactorAuthService.generateNewAccountConfig(twoFaUser, TwoFaProviderType.TOTP);
        totpTwoFaAccountConfig.setUseByDefault(true);
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, twoFaUser.getId(), totpTwoFaAccountConfig);

        SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38012312322");
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, twoFaUser.getId(), smsTwoFaAccountConfig);

        EmailTwoFaAccountConfig emailTwoFaAccountConfig = new EmailTwoFaAccountConfig();
        emailTwoFaAccountConfig.setEmail(twoFaUser.getEmail());
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, twoFaUser.getId(), emailTwoFaAccountConfig);

        logInWithPreVerificationToken(twoFaUser.getEmail(), "12345678");

        Map<TwoFaProviderType, TwoFactorAuthController.TwoFaProviderInfo> providersInfos = readResponse(doGet("/api/auth/2fa/providers").andExpect(status().isOk()), new TypeReference<List<TwoFactorAuthController.TwoFaProviderInfo>>() {}).stream()
                .collect(Collectors.toMap(TwoFactorAuthController.TwoFaProviderInfo::getType, v -> v));

        assertThat(providersInfos).size().isEqualTo(3);

        assertThat(providersInfos).containsKey(TwoFaProviderType.TOTP);
        assertThat(providersInfos.get(TwoFaProviderType.TOTP).isDefault()).isTrue();

        assertThat(providersInfos).containsKey(TwoFaProviderType.SMS);
        assertThat(providersInfos.get(TwoFaProviderType.SMS).isDefault()).isFalse();

        assertThat(providersInfos).containsKey(TwoFaProviderType.EMAIL);
        assertThat(providersInfos.get(TwoFaProviderType.EMAIL).isDefault()).isFalse();
    }

    private void logInWithPreVerificationToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);

        JwtPair response = readResponse(doPost("/api/auth/login", loginRequest).andExpect(status().isOk()), JwtPair.class);
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getScope()).isEqualTo(Authority.PRE_VERIFICATION_TOKEN);

        this.token = response.getToken();
    }

    private TotpTwoFaAccountConfig configureTotpTwoFa(Consumer<PlatformTwoFaSettings>... customizer) throws ThingsboardException {
        TotpTwoFaProviderConfig totpTwoFaProviderConfig = new TotpTwoFaProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("tb");

        PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(Arrays.stream(new TwoFaProviderConfig[]{totpTwoFaProviderConfig}).collect(Collectors.toList()));
        twoFaSettings.setMinVerificationCodeSendPeriod(5);
        twoFaSettings.setTotalAllowedTimeForVerification(100);
        Arrays.stream(customizer).forEach(c -> c.accept(twoFaSettings));
        twoFaConfigManager.savePlatformTwoFaSettings(TenantId.SYS_TENANT_ID, twoFaSettings);

        TotpTwoFaAccountConfig totpTwoFaAccountConfig = (TotpTwoFaAccountConfig) twoFactorAuthService.generateNewAccountConfig(user, TwoFaProviderType.TOTP);
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, user.getId(), totpTwoFaAccountConfig);
        return totpTwoFaAccountConfig;
    }

    private SmsTwoFaAccountConfig configureSmsTwoFa(Consumer<SmsTwoFaProviderConfig>... customizer) throws ThingsboardException {
        SmsTwoFaProviderConfig smsTwoFaProviderConfig = new SmsTwoFaProviderConfig();
        smsTwoFaProviderConfig.setVerificationCodeLifetime(60);
        smsTwoFaProviderConfig.setSmsVerificationMessageTemplate("${code}");
        Arrays.stream(customizer).forEach(c -> c.accept(smsTwoFaProviderConfig));

        PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(Arrays.stream(new TwoFaProviderConfig[]{smsTwoFaProviderConfig}).collect(Collectors.toList()));
        twoFaSettings.setMinVerificationCodeSendPeriod(5);
        twoFaSettings.setTotalAllowedTimeForVerification(100);
        twoFaConfigManager.savePlatformTwoFaSettings(TenantId.SYS_TENANT_ID, twoFaSettings);

        SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38050505050");
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, user.getId(), smsTwoFaAccountConfig);
        return smsTwoFaAccountConfig;
    }

    private String getCorrectTotp(TotpTwoFaAccountConfig totpTwoFaAccountConfig) {
        String secret = StringUtils.substringAfterLast(totpTwoFaAccountConfig.getAuthUrl(), "secret=");
        return new Totp(secret).now();
    }

}
