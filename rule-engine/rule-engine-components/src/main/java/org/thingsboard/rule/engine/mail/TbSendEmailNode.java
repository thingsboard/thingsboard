/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.Properties;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send email",
        configClazz = TbSendEmailNodeConfiguration.class,
        nodeDescription = "Sends email message via SMTP server.",
        nodeDetails = "Expects messages with <b>SEND_EMAIL</b> type. Node works only with messages that " +
                " where created using <code>to Email</code> transformation Node, please connect this Node " +
                "with <code>to Email</code> Node using <code>Successful</code> chain.",
        configDirective = "tbExternalNodeSendEmailConfig",
        icon = "send",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/external/send-email/"
)
public class TbSendEmailNode extends TbAbstractExternalNode {

    private static final String MAIL_PROP = "mail.";
    private TbSendEmailNodeConfiguration config;
    private JavaMailSenderImpl mailSender;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbSendEmailNodeConfiguration.class);
        try {
            if (!this.config.isUseSystemSmtpSettings()) {
                mailSender = createMailSender();
            }
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            validateType(msg);
            TbEmail email = getEmail(msg);
            var tbMsg = ackIfNeeded(ctx, msg);
            withCallback(ctx.getMailExecutor().executeAsync(() -> {
                        sendEmail(ctx, tbMsg, email);
                        return null;
                    }),
                    ok -> tellSuccess(ctx, tbMsg),
                    fail -> tellFailure(ctx, tbMsg, fail));
        } catch (Exception ex) {
            ctx.tellFailure(msg, ex);
        }
    }

    private void sendEmail(TbContext ctx, TbMsg msg, TbEmail email) throws Exception {
        if (this.config.isUseSystemSmtpSettings()) {
            ctx.getMailService(true).send(ctx.getTenantId(), msg.getCustomerId(), email);
        } else {
            ctx.getMailService(false).send(ctx.getTenantId(), msg.getCustomerId(), email, this.mailSender, config.getTimeout());
        }
    }

    private TbEmail getEmail(TbMsg msg) {
        TbEmail email = JacksonUtil.fromString(msg.getData(), TbEmail.class);
        if (StringUtils.isBlank(email.getTo())) {
            throw new IllegalStateException("Email destination can not be blank [" + email.getTo() + "]");
        }
        return email;
    }

    private void validateType(TbMsg msg) {
        if (!msg.isTypeOf(TbMsgType.SEND_EMAIL)) {
            String type = msg.getType();
            log.warn("Not expected msg type [{}] for SendEmail Node", type);
            throw new IllegalStateException("Not expected msg type " + type + " for SendEmail Node");
        }
    }

    private JavaMailSenderImpl createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(this.config.getSmtpHost());
        mailSender.setPort(this.config.getSmtpPort());
        mailSender.setUsername(this.config.getUsername());
        mailSender.setPassword(this.config.getPassword());
        mailSender.setJavaMailProperties(createJavaMailProperties());
        return mailSender;
    }

    private Properties createJavaMailProperties() {
        Properties javaMailProperties = new Properties();
        String protocol = this.config.getSmtpProtocol();
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put(MAIL_PROP + protocol + ".host", this.config.getSmtpHost());
        javaMailProperties.put(MAIL_PROP + protocol + ".port", this.config.getSmtpPort() + "");
        javaMailProperties.put(MAIL_PROP + protocol + ".timeout", this.config.getTimeout() + "");
        javaMailProperties.put(MAIL_PROP + protocol + ".auth", String.valueOf(StringUtils.isNotEmpty(this.config.getUsername())));
        javaMailProperties.put(MAIL_PROP + protocol + ".starttls.enable", Boolean.valueOf(this.config.isEnableTls()).toString());
        if (this.config.isEnableTls() && StringUtils.isNoneEmpty(this.config.getTlsVersion())) {
            javaMailProperties.put(MAIL_PROP + protocol + ".ssl.protocols", this.config.getTlsVersion());
        }
        if (this.config.isEnableProxy()) {
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.host", config.getProxyHost());
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.port", config.getProxyPort());
            if (StringUtils.isNoneEmpty(config.getProxyUser())) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.user", config.getProxyUser());
            }
            if (StringUtils.isNoneEmpty(config.getProxyPassword())) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.password", config.getProxyPassword());
            }
        }
        return javaMailProperties;
    }

}
