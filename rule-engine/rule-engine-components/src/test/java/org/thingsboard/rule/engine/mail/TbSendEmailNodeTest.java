// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.mail;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TbSendEmailNodeTest {

    @Test
    void serverIdentityCheckIsDisabled() throws TbNodeException {
        TbSendEmailNodeConfiguration config = new TbSendEmailNodeConfiguration().defaultConfiguration();
        config.setUseSystemSmtpSettings(false);
        config.setSmtpProtocol("smtps");
        config.setSmtpHost("10.0.0.1");
        config.setEnableTls(true);
        TbSendEmailNode node = new TbSendEmailNode();

        node.init(mock(TbContext.class), new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        JavaMailSenderImpl mailSender = (JavaMailSenderImpl) ReflectionTestUtils.getField(node, "mailSender");
        assertThat(mailSender.getJavaMailProperties()).containsEntry("mail.smtps.ssl.checkserveridentity", "false");
    }

}
