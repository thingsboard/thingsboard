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
package org.thingsboard.server.service.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

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

        Mockito.verify(tbMailSender, times(1)).updateOauth2PasswordIfExpired();
        Mockito.verify(tbMailSender, times(1)).doSendSuper(any(), any());
    }

    @Test
    public void testTestConnection() throws MessagingException {
        willCallRealMethod().given(tbMailSender).testConnection();
        tbMailSender.testConnection();

        Mockito.verify(tbMailSender, times(1)).updateOauth2PasswordIfExpired();
        Mockito.verify(tbMailSender, times(1)).testConnectionSuper();
    }

    @ParameterizedTest
    @MethodSource("provideSenderConfiguration")
    public void testUpdateOauth2PasswordIfExpiredIfOauth2Enabled(boolean oauth2, long expiresIn, boolean passwordUpdateNeeded) {
        willReturn(oauth2).given(tbMailSender).getOauth2Enabled();
        willReturn(expiresIn).given(tbMailSender).getTokenExpires();

        willCallRealMethod().given(tbMailSender).updateOauth2PasswordIfExpired();
        tbMailSender.updateOauth2PasswordIfExpired();

        if (passwordUpdateNeeded) {
            Mockito.verify(tbMailSender, times(1)).refreshAccessToken();
            Mockito.verify(tbMailSender, times(1)).setPassword(any());
        } else {
            Mockito.verify(tbMailSender, Mockito.never()).refreshAccessToken();
            Mockito.verify(tbMailSender, Mockito.never()).setPassword(any());
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
