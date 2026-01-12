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
package org.thingsboard.rule.engine.mail;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class TbMsgToEmailNodeTest {

    private static final int EXPECTED_TEMPERATURE = 30;
    private static final String EXPECTED_DEVICE_NAME = "TH-001";
    private static final String EXPECTED_DEVICE_TYPE = "thermostat";
    private static final String EXPECTED_SUBJECT = "Device " + EXPECTED_DEVICE_TYPE + " temperature high";
    private static final String EXPECTED_BODY = "Device " + EXPECTED_DEVICE_NAME + " has high temperature " + EXPECTED_TEMPERATURE;
    private static final String EXPECTED_TO_EMAIL = "user@email.io";
    private static final String DYNAMIC_MAIL_BODY_TYPE = "dynamic";

    private EntityId originator;
    private TbMsgToEmailNode node;
    private TbMsgToEmailNodeConfiguration config;

    private TbContext ctxMock;

    @BeforeEach
    void setUp() throws TbNodeException {
        ctxMock = mock(TbContext.class);
        originator = new DeviceId(UUID.randomUUID());
        config = new TbMsgToEmailNodeConfiguration().defaultConfiguration();
        node = new TbMsgToEmailNode();
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        // GIVEN-WHEN-THEN
        assertThat(config.getFromTemplate()).isEqualTo("info@testmail.org");
        assertThat(config.getToTemplate()).isEqualTo("${userEmail}");
        assertThat(config.getSubjectTemplate()).isEqualTo("Device ${deviceType} temperature high");
        assertThat(config.getBodyTemplate()).isEqualTo("Device ${deviceName} has high temperature $[temperature]");
    }

    @ParameterizedTest
    @MethodSource("MailBodyTypeTestConfig")
    public void givenMailBodyTypeTestConfig_whenOnMsg_thenVerify(MailBodyTypeTestConfig testConfig) throws TbNodeException {
        // GIVEN
        String mailBodyType = testConfig.getMailBodyType();
        config.setMailBodyType(mailBodyType);
        if (DYNAMIC_MAIL_BODY_TYPE.equals(mailBodyType)) {
            config.setIsHtmlTemplate("${html}");
        }
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var md = new TbMsgMetaData();
        md.putValue("userEmail", EXPECTED_TO_EMAIL);
        md.putValue("deviceType", EXPECTED_DEVICE_TYPE);
        md.putValue("deviceName", EXPECTED_DEVICE_NAME);
        if (testConfig.getIsHtmlTemplateMdValue() != null) {
            md.putValue("html", testConfig.getIsHtmlTemplateMdValue());
        }

        var msgDataStr = "{\"temperature\": " + EXPECTED_TEMPERATURE + "}";
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .copyMetaData(md)
                .data(msgDataStr)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        var originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        var metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        var dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctxMock).transformMsg(
                msgCaptor.capture(),
                typeCaptor.capture(),
                originatorCaptor.capture(),
                metadataCaptor.capture(),
                dataCaptor.capture()
        );
        verify(ctxMock, never()).tellFailure(any(), any());

        Assertions.assertEquals(TbMsgType.SEND_EMAIL, typeCaptor.getValue());
        Assertions.assertEquals(originator, originatorCaptor.getValue());
        Assertions.assertNotSame(md, metadataCaptor.getValue());

        var actual = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), TbEmail.class);
        var expected = getExpectedTbEmail(testConfig.isExpectedHtmlValue());

        Assertions.assertEquals(expected, actual);
    }

    private TbEmail getExpectedTbEmail(boolean html) {
        return TbEmail.builder()
                .from(config.getFromTemplate())
                .to(EXPECTED_TO_EMAIL)
                .subject(EXPECTED_SUBJECT)
                .body(EXPECTED_BODY)
                .html(html)
                .build();
    }

    static Stream<MailBodyTypeTestConfig> MailBodyTypeTestConfig() {
        return Stream.of(
                new MailBodyTypeTestConfig(false, "false", null),
                new MailBodyTypeTestConfig(false, null, null),
                new MailBodyTypeTestConfig(false, DYNAMIC_MAIL_BODY_TYPE, "false"),
                new MailBodyTypeTestConfig(true, DYNAMIC_MAIL_BODY_TYPE, "true"),
                new MailBodyTypeTestConfig(true, "true", null)
        );
    }

    @Data
    @RequiredArgsConstructor
    static class MailBodyTypeTestConfig {
        private final boolean expectedHtmlValue;
        private final String mailBodyType;
        private final String isHtmlTemplateMdValue;
    }

}
