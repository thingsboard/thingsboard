/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
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
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeSendEmailConfig",
        icon = "send"
)
public class TbSendEmailNode implements TbNode {

    private static final String MAIL_PROP = "mail.";
    static final String SEND_EMAIL_TYPE = "SEND_EMAIL";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TbSendEmailNodeConfiguration config;
    private JavaMailSenderImpl mailSender;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        try {
            this.config = TbNodeUtils.convert(configuration, TbSendEmailNodeConfiguration.class);
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
            validateType(msg.getType());
            EmailPojo email = getEmail(msg);
            withCallback(ctx.getMailExecutor().executeAsync(() -> {
                        sendEmail(ctx, email);
                        return null;
                    }),
                    ok -> ctx.tellSuccess(msg),
                    fail -> ctx.tellFailure(msg, fail));
        } catch (Exception ex) {
            ctx.tellFailure(msg, ex);
        }
    }

    private void sendEmail(TbContext ctx, EmailPojo email) throws Exception {
        if (this.config.isUseSystemSmtpSettings()) {
            ctx.getMailService().send(ctx.getTenantId(), email.getFrom(), email.getTo(), email.getCc(),
                    email.getBcc(), email.getSubject(), email.getBody(), email.isHtml());
        } else {
            MimeMessage mailMsg = mailSender.createMimeMessage();
            boolean multipart = email.getImages() != null && !email.getImages().isEmpty();
            MimeMessageHelper helper = new MimeMessageHelper(mailMsg, multipart, "UTF-8");
            helper.setFrom(email.getFrom());
            helper.setTo(email.getTo().split("\\s*,\\s*"));
            if (!StringUtils.isBlank(email.getCc())) {
                helper.setCc(email.getCc().split("\\s*,\\s*"));
            }
            if (!StringUtils.isBlank(email.getBcc())) {
                helper.setBcc(email.getBcc().split("\\s*,\\s*"));
            }
            helper.setSubject(email.getSubject());
            helper.setText(email.getBody(), email.isHtml());

            if (email.getImages() != null && email.getImages().size() > 0) {
                Map<String, String> images = email.getImages();
                for (String imgId : images.keySet()) {
                    String imgValue = images.get(imgId);
                    String value = imgValue.replaceFirst("^data:image/[^;]*;base64,?", "");
                    byte[] bytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(value);
                    String contentType = helper.getFileTypeMap().getContentType(imgId);
                    InputStreamSource iss = () -> new ByteArrayInputStream(bytes);
                    helper.addInline(imgId, iss, contentType);
                }
            }

            mailSender.send(helper.getMimeMessage());
        }
    }

    private EmailPojo getEmail(TbMsg msg) throws IOException {
        EmailPojo email = MAPPER.readValue(msg.getData(), EmailPojo.class);
        if (StringUtils.isBlank(email.getTo())) {
            throw new IllegalStateException("Email destination can not be blank [" + email.getTo() + "]");
        }
        return email;
    }

    private void validateType(String type) {
        if (!SEND_EMAIL_TYPE.equals(type)) {
            log.warn("Not expected msg type [{}] for SendEmail Node", type);
            throw new IllegalStateException("Not expected msg type " + type + " for SendEmail Node");
        }
    }

    @Override
    public void destroy() {
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
