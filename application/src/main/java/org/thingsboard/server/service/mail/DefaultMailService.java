/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.NestedRuntimeException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.velocity.VelocityEngineUtils;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardException;

import javax.annotation.PostConstruct;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Service
@Slf4j
public class DefaultMailService implements MailService {

    @Autowired
    private MessageSource messages;
    
    @Autowired
    @Qualifier("velocityEngine")
    private VelocityEngine engine;
    
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
        AdminSettings settings = adminSettingsService.findAdminSettingsByKey("mail");
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
        javaMailProperties.put("mail." + protocol + ".host", jsonConfig.get("smtpHost").asText());
        javaMailProperties.put("mail." + protocol + ".port", jsonConfig.get("smtpPort").asText());
        javaMailProperties.put("mail." + protocol + ".timeout", jsonConfig.get("timeout").asText());
        javaMailProperties.put("mail." + protocol + ".auth", String.valueOf(StringUtils.isNotEmpty(jsonConfig.get("username").asText())));
        javaMailProperties.put("mail." + protocol + ".starttls.enable", jsonConfig.get("enableTls"));
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
        
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("targetEmail", email);
        
        String message = VelocityEngineUtils.mergeTemplateIntoString(this.engine,
                "test.vm", "UTF-8", model);
        
        sendMail(testMailSender, mailFrom, email, subject, message); 
    }

    @Override
    public void sendActivationEmail(String activationLink, String email) throws ThingsboardException {
        
        String subject = messages.getMessage("activation.subject", null, Locale.US);
        
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("activationLink", activationLink);
        model.put("targetEmail", email);
        
        String message = VelocityEngineUtils.mergeTemplateIntoString(this.engine,
                "activation.vm", "UTF-8", model);
        
        sendMail(mailSender, mailFrom, email, subject, message); 
    }
    
    @Override
    public void sendAccountActivatedEmail(String loginLink, String email) throws ThingsboardException {
        
        String subject = messages.getMessage("account.activated.subject", null, Locale.US);
        
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("loginLink", loginLink);
        model.put("targetEmail", email);
        
        String message = VelocityEngineUtils.mergeTemplateIntoString(this.engine,
                "account.activated.vm", "UTF-8", model);
        
        sendMail(mailSender, mailFrom, email, subject, message); 
    }

    @Override
    public void sendResetPasswordEmail(String passwordResetLink, String email) throws ThingsboardException {
        
        String subject = messages.getMessage("reset.password.subject", null, Locale.US);
        
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("passwordResetLink", passwordResetLink);
        model.put("targetEmail", email);
        
        String message = VelocityEngineUtils.mergeTemplateIntoString(this.engine,
                "reset.password.vm", "UTF-8", model);
        
        sendMail(mailSender, mailFrom, email, subject, message); 
    }
    
    @Override
    public void sendPasswordWasResetEmail(String loginLink, String email) throws ThingsboardException {
        
        String subject = messages.getMessage("password.was.reset.subject", null, Locale.US);
        
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("loginLink", loginLink);
        model.put("targetEmail", email);
        
        String message = VelocityEngineUtils.mergeTemplateIntoString(this.engine,
                "password.was.reset.vm", "UTF-8", model);
        
        sendMail(mailSender, mailFrom, email, subject, message); 
    }


    private void sendMail(JavaMailSenderImpl mailSender, 
            String mailFrom, String email, 
            String subject, String message) throws ThingsboardException {
        try {
            MimeMessage mimeMsg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(message, true);
            mailSender.send(helper.getMimeMessage());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    protected ThingsboardException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException)exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        return new ThingsboardException(String.format("Unable to send mail: %s", message),
                ThingsboardErrorCode.GENERAL);
    }

}
