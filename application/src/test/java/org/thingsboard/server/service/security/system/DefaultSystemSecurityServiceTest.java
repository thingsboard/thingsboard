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
package org.thingsboard.server.service.security.system;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.settings.SecuritySettingsService;
import org.thingsboard.server.dao.user.UserService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultSystemSecurityServiceTest {

    @Mock
    private AdminSettingsService adminSettingsService;
    @Mock
    private BCryptPasswordEncoder encoder;
    @Mock
    private UserService userService;
    @Mock
    private MailService mailService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private SecuritySettingsService securitySettingsService;

    private DefaultSystemSecurityService systemSecurityService;

    private TenantId tenantId;
    private UserId userId;
    private UserCredentials userCredentials;
    private SecuritySettings securitySettings;
    private String username;
    private String password;
    private String encodedPassword;

    @Before
    public void setUp() {
        systemSecurityService = new DefaultSystemSecurityService(adminSettingsService, encoder, userService, mailService, auditLogService, securitySettingsService);

        tenantId = TenantId.fromUUID(UUID.randomUUID());
        userId = new UserId(UUID.randomUUID());
        username = "tenant@example.com";
        password = "correctPassword";
        encodedPassword = "$2a$10$encodedPasswordHash";

        userCredentials = new UserCredentials();
        userCredentials.setUserId(userId);
        userCredentials.setEnabled(true);
        userCredentials.setPassword(encodedPassword);
        userCredentials.setCreatedTime(System.currentTimeMillis());

        securitySettings = new SecuritySettings();
        securitySettings.setMaxFailedLoginAttempts(5);
        securitySettings.setPasswordPolicy(new UserPasswordPolicy());
    }

    @Test
    public void testValidateUserCredentials_successfulLogin() {
        when(encoder.matches(password, encodedPassword)).thenReturn(true);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password);

        verify(encoder).matches(password, encodedPassword);
        verify(userService).resetFailedLoginAttempts(tenantId, userId);
        verify(userService, never()).increaseFailedLoginAttempts(any(), any());
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_incrementsFailedAttempts() {
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(3);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Authentication Failed");

        verify(userService).increaseFailedLoginAttempts(tenantId, userId);
        verify(userService, never()).setUserCredentialsEnabled(any(), any(), eq(false));
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_accountLocked() {
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(6);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("locked due to security policy");

        verify(userService).increaseFailedLoginAttempts(tenantId, userId);
        verify(userService).setUserCredentialsEnabled(TenantId.SYS_TENANT_ID, userId, false);
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_exactlyAtThreshold_noLock() {
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(5);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Authentication Failed");

        verify(userService).increaseFailedLoginAttempts(tenantId, userId);
        verify(userService, never()).setUserCredentialsEnabled(any(), any(), eq(false));
    }

    @Test
    public void testValidateUserCredentials_correctPassword_disabledUser_throwsDisabledException() {
        userCredentials.setEnabled(false);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(DisabledException.class)
                .hasMessage("User is not active");

        verify(encoder, never()).matches(any(), any());
        verify(userService, never()).increaseFailedLoginAttempts(any(), any());
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_maxAttemptsDisabled() {
        securitySettings.setMaxFailedLoginAttempts(null);
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(100);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(BadCredentialsException.class);

        verify(userService).increaseFailedLoginAttempts(tenantId, userId);
        verify(userService, never()).setUserCredentialsEnabled(any(), any(), eq(false));
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_maxAttemptsSetToZero() {
        securitySettings.setMaxFailedLoginAttempts(0);
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(100);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(BadCredentialsException.class);

        verify(userService).increaseFailedLoginAttempts(tenantId, userId);
        verify(userService, never()).setUserCredentialsEnabled(any(), any(), eq(false));
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_withNotificationEmail() throws ThingsboardException {
        String notificationEmail = "admin@example.com";
        securitySettings.setUserLockoutNotificationEmail(notificationEmail);
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(6);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(LockedException.class);

        verify(userService).setUserCredentialsEnabled(TenantId.SYS_TENANT_ID, userId, false);
        verify(mailService).sendAccountLockoutEmail(eq(username), eq(notificationEmail), eq(5));
    }

}
