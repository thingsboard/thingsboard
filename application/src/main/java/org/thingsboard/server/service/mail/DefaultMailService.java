/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.NestedRuntimeException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageStateMailMessage;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Service
@Slf4j
public class DefaultMailService implements MailService {

    public static final String MAIL_PROP = "mail.";
    public static final String TARGET_EMAIL = "targetEmail";
    public static final String UTF_8 = "UTF-8";
    @Autowired
    private MessageSource messages;

    @Autowired
    private Configuration freemarkerConfig;

    private JavaMailSenderImpl mailSender;

    private String mailFrom;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @PostConstruct
    private void init() {
        updateMailConfiguration();
    }

    @Override
    public void updateMailConfiguration() {
        AdminSettings settings = adminSettingsService.findAdminSettingsByKey(new TenantId(EntityId.NULL_UUID), "mail");
        if (settings != null) {
            JsonNode jsonConfig = settings.getJsonValue();
            mailSender = createMailSender(jsonConfig);
            mailFrom = jsonConfig.get("mailFrom").asText();
        } else {
            throw new IncorrectParameterException("Failed to date mail configuration. Settings not found!");
        }
    }

    private JavaMailSenderImpl createMailSender(JsonNode jsonConfig) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(jsonConfig.get("smtpHost").asText());
        mailSender.setPort(parsePort(jsonConfig.get("smtpPort").asText()));
        mailSender.setUsername(jsonConfig.get("username").asText());
        mailSender.setPassword(jsonConfig.get("password").asText());
        mailSender.setJavaMailProperties(createJavaMailProperties(jsonConfig));
        return mailSender;
    }

    private Properties createJavaMailProperties(JsonNode jsonConfig) {
        Properties javaMailProperties = new Properties();
        String protocol = jsonConfig.get("smtpProtocol").asText();
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put(MAIL_PROP + protocol + ".host", jsonConfig.get("smtpHost").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".port", jsonConfig.get("smtpPort").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".timeout", jsonConfig.get("timeout").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".auth", String.valueOf(StringUtils.isNotEmpty(jsonConfig.get("username").asText())));
        boolean enableTls = false;
        if (jsonConfig.has("enableTls")) {
            if (jsonConfig.get("enableTls").isBoolean() && jsonConfig.get("enableTls").booleanValue()) {
                enableTls = true;
            } else if (jsonConfig.get("enableTls").isTextual()) {
                enableTls = "true".equalsIgnoreCase(jsonConfig.get("enableTls").asText());
            }
        }
        javaMailProperties.put(MAIL_PROP + protocol + ".starttls.enable", enableTls);
        if (enableTls && jsonConfig.has("tlsVersion") && !jsonConfig.get("tlsVersion").isNull()) {
            String tlsVersion = jsonConfig.get("tlsVersion").asText();
            if (StringUtils.isNoneEmpty(tlsVersion)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".ssl.protocols", tlsVersion);
            }
        }

        boolean enableProxy = jsonConfig.has("enableProxy") && jsonConfig.get("enableProxy").asBoolean();

        if (enableProxy) {
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.host", jsonConfig.get("proxyHost").asText());
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.port", jsonConfig.get("proxyPort").asText());
            String proxyUser = jsonConfig.get("proxyUser").asText();
            if (StringUtils.isNoneEmpty(proxyUser)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.user", proxyUser);
            }
            String proxyPassword = jsonConfig.get("proxyPassword").asText();
            if (StringUtils.isNoneEmpty(proxyPassword)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.password", proxyPassword);
            }
        }
        return javaMailProperties;
    }

    private int parsePort(String strPort) {
        try {
            return Integer.valueOf(strPort);
        } catch (NumberFormatException e) {
            throw new IncorrectParameterException(String.format("Invalid smtp port value: %s", strPort));
        }
    }

    @Override
    public void sendEmail(String email, String subject, String message) throws ThingsboardException {
        sendMail(mailSender, mailFrom, email, subject, message);
    }

    @Override
    public void sendTestMail(JsonNode jsonConfig, String email) throws ThingsboardException {
        JavaMailSenderImpl testMailSender = createMailSender(jsonConfig);
        String mailFrom = jsonConfig.get("mailFrom").asText();
        String subject = messages.getMessage("test.message.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("test.ftl", model);

        sendMail(testMailSender, mailFrom, email, subject, message);
    }

    @Override
    public void sendActivationEmail(String activationLink, String email) throws ThingsboardException {

        String subject = messages.getMessage("activation.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("activationLink", activationLink);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("activation.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message);
    }

    @Override
    public void sendAccountActivatedEmail(String loginLink, String email) throws ThingsboardException {

        String subject = messages.getMessage("account.activated.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("account.activated.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message);
    }

    @Override
    public void sendResetPasswordEmail(String passwordResetLink, String email) throws ThingsboardException {

        String subject = messages.getMessage("reset.password.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("passwordResetLink", passwordResetLink);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("reset.password.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message);
    }

    @Override
    public void sendPasswordWasResetEmail(String loginLink, String email) throws ThingsboardException {

        String subject = messages.getMessage("password.was.reset.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("password.was.reset.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message);
    }

    @Override
    public void send(String from, String to, String cc, String bcc, String subject, String body) throws MessagingException {
        MimeMessage mailMsg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mailMsg, "UTF-8");
        helper.setFrom(StringUtils.isBlank(from) ? mailFrom : from);
        helper.setTo(to.split("\\s*,\\s*"));
        if (!StringUtils.isBlank(cc)) {
            helper.setCc(cc.split("\\s*,\\s*"));
        }
        if (!StringUtils.isBlank(bcc)) {
            helper.setBcc(bcc.split("\\s*,\\s*"));
        }
        helper.setSubject(subject);
        helper.setText(body);
        mailSender.send(helper.getMimeMessage());
    }

    @Override
    public void sendAccountLockoutEmail(String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException {
        String subject = messages.getMessage("account.lockout.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("lockoutAccount", lockoutEmail);
        model.put("maxFailedLoginAttempts", maxFailedLoginAttempts);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("account.lockout.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message);
    }

    @Override
    public void sendApiFeatureStateEmail(ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageStateMailMessage msg) throws ThingsboardException {
        String subject = messages.getMessage("api.usage.state", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("apiFeature", apiFeature.getLabel());
        model.put(TARGET_EMAIL, email);

        String message = null;

        switch (stateValue) {
            case ENABLED:
                message = mergeTemplateIntoString("state.enabled.ftl", model);
                break;
            case WARNING:
                message = mergeTemplateIntoString("state.warning.ftl", model);
                break;
            case DISABLED:
                model.put("apiLimitValueLabel", toDisabledValueLabel(apiFeature) + " " + toDisabledValueLabel(msg));
                message = mergeTemplateIntoString("state.disabled.ftl", model);
                break;
        }
        sendMail(mailSender, mailFrom, email, subject, message);
    }

    private String toDisabledValueLabel(ApiFeature apiFeature) {
        switch (apiFeature) {
            case DB:
                return "saved";
            case TRANSPORT:
                return "received";
            case JS:
            case RE:
                return "invoked";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String toDisabledValueLabel(ApiUsageStateMailMessage msg) {
        switch (msg.getKey()) {
            case STORAGE_DP_COUNT:
            case TRANSPORT_DP_COUNT:
                return (msg.getThreshold() / 1000000) + "M data points";
            case TRANSPORT_MSG_COUNT:
                return (msg.getThreshold() / 1000000) + "M messages";
            case JS_EXEC_COUNT:
                return (msg.getThreshold() / 1000000) + "M JavaScript functions";
            case RE_EXEC_COUNT:
                return (msg.getThreshold() / 1000000) + "M Rule Engine nodes";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private void sendMail(JavaMailSenderImpl mailSender,
                          String mailFrom, String email,
                          String subject, String message) throws ThingsboardException {
        try {
            MimeMessage mimeMsg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, UTF_8);
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(message, true);
            mailSender.send(helper.getMimeMessage());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private String mergeTemplateIntoString(String templateLocation,
                                           Map<String, Object> model) throws ThingsboardException {
        try {
            Template template = freemarkerConfig.getTemplate(templateLocation);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    protected ThingsboardException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException) exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        log.warn("Unable to send mail: {}", message);
        return new ThingsboardException(String.format("Unable to send mail: %s", message),
                ThingsboardErrorCode.GENERAL);
    }

}
