// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TbMailSenderTest {

    private TbMailSender tbMailSender;

    @BeforeEach
    void setUp() {
        tbMailSender = mock(TbMailSender.class);
    }

    @Test
    public void testDoSendSendMail() {
        MimeMessage mimeMsg = new MimeMessage(Session.getInstance(new Properties()));
        List<MimeMessage> mimeMessages = new ArrayList<>(1);
        mimeMessages.add(mimeMsg);

        willCallRealMethod().given(tbMailSender).doSend(any(), any());
        tbMailSender.doSend(mimeMessages.toArray(new MimeMessage[0]), null);

        verify(tbMailSender, times(1)).updateOauth2PasswordIfExpired();
        verify(tbMailSender, times(1)).doSendSuper(any(), any());
    }

    @Test
    public void testTestConnection() throws MessagingException {
        willCallRealMethod().given(tbMailSender).testConnection();
        tbMailSender.testConnection();

        verify(tbMailSender, times(1)).updateOauth2PasswordIfExpired();
        verify(tbMailSender, times(1)).testConnectionSuper();
    }

    @Test
    public void testGiveMailTenantSettings_noFallbackToSystem() {
        var json = JacksonUtil.newObjectNode();
        json.put("useSystemMailSettings", false);

        AdminSettings tenantSettings = new AdminSettings();
        tenantSettings.setKey("mail");
        tenantSettings.setJsonValue(json);

        willReturn(tenantSettings).given(tbMailSender).getAdminMailSettings(any());
        willReturn(true).given(tbMailSender).isAllowSystemMailService();

        willCallRealMethod().given(tbMailSender).getMailSettings(any());

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        AdminSettings result = tbMailSender.getMailSettings(tenantId);

        assertSame(tenantSettings, result);
        verify(tbMailSender, times(1)).getAdminMailSettings(eq(tenantId));
        verify(tbMailSender, never()).getAdminMailSettings(eq(TenantId.SYS_TENANT_ID));
    }

    @Test
    public void testFallbackToSystemMailSettings() {
        var json = JacksonUtil.newObjectNode();

        AdminSettings tenantSettings = new AdminSettings();
        tenantSettings.setKey("mail");
        tenantSettings.setJsonValue(json);

        AdminSettings systemSettings = new AdminSettings();
        systemSettings.setKey("mail");
        systemSettings.setJsonValue(JacksonUtil.newObjectNode());

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

        willReturn(tenantSettings).given(tbMailSender).getAdminMailSettings(eq(tenantId));
        willReturn(systemSettings).given(tbMailSender).getAdminMailSettings(eq(TenantId.SYS_TENANT_ID));
        willReturn(true).given(tbMailSender).isAllowSystemMailService();

        willCallRealMethod().given(tbMailSender).getMailSettings(any());

        AdminSettings result = tbMailSender.getMailSettings(tenantId);

        assertSame(systemSettings, result);
        verify(tbMailSender, times(1)).getAdminMailSettings(eq(tenantId));
        verify(tbMailSender, times(1)).getAdminMailSettings(eq(TenantId.SYS_TENANT_ID));
    }

    @Test
    public void testFallbackNotAllowedThrowsException() {
        var json = JacksonUtil.newObjectNode();
        json.put("useSystemMailSettings", true);

        AdminSettings tenantSettings = new AdminSettings();
        tenantSettings.setKey("mail");
        tenantSettings.setJsonValue(json);

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

        willReturn(tenantSettings).given(tbMailSender).getAdminMailSettings(eq(tenantId));
        willReturn(false).given(tbMailSender).isAllowSystemMailService();

        willCallRealMethod().given(tbMailSender).getMailSettings(any());

        assertThrows(RuntimeException.class, () -> tbMailSender.getMailSettings(tenantId));

        verify(tbMailSender, times(1)).getAdminMailSettings(eq(tenantId));
        verify(tbMailSender, never()).getAdminMailSettings(eq(TenantId.SYS_TENANT_ID));
    }

    @ParameterizedTest
    @MethodSource("provideSenderConfiguration")
    public void testUpdateOauth2PasswordIfExpiredIfOauth2Enabled(boolean oauth2, long expiresIn, boolean passwordUpdateNeeded) {
        willReturn(oauth2).given(tbMailSender).getOauth2Enabled();
        willReturn(expiresIn).given(tbMailSender).getTokenExpires();

        willCallRealMethod().given(tbMailSender).updateOauth2PasswordIfExpired();
        tbMailSender.updateOauth2PasswordIfExpired();

        if (passwordUpdateNeeded) {
            verify(tbMailSender, times(1)).refreshAccessToken(any());
            verify(tbMailSender, times(1)).setPassword(any());
        } else {
            verify(tbMailSender, never()).refreshAccessToken(any());
            verify(tbMailSender, never()).setPassword(any());
        }
    }

    private static Stream<Arguments> provideSenderConfiguration() {
        return Stream.of(
                Arguments.of(true, 0L, true),
                Arguments.of(true, System.currentTimeMillis() + 5000, false),
                Arguments.of(false, 0L, false),
                Arguments.of(false, System.currentTimeMillis() + 5000, false)
        );
    }

}
