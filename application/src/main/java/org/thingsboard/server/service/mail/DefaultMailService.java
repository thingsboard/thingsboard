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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.RateLimitExceededException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMailService implements MailService {

    private static final String TARGET_EMAIL = "targetEmail";
    private static final String UTF_8 = "UTF-8";
    private static final long DEFAULT_TIMEOUT = 10_000;

    private final ScheduledExecutorService timeoutScheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("mail-service-watchdog");

    private final MessageSource messages;
    private final Configuration freemarkerConfig;
    private final AdminSettingsService adminSettingsService;
    private final TbApiUsageReportClient apiUsageClient;
    @Lazy
    private final TbApiUsageStateService apiUsageStateService;
    private final MailSenderInternalExecutorService mailExecutorService;
    private final PasswordResetExecutorService passwordResetExecutorService;
    private final TbMailContextComponent ctx;
    private final RateLimitService rateLimitService;

    @Value("${mail.per_tenant_rate_limits:}")
    private String perTenantRateLimitConfig;

    private TbMailSender mailSender;

    private String mailFrom;

    private long timeout;

    @PostConstruct
    private void init() {
        updateMailConfiguration();
    }

    @PreDestroy
    public void destroy() {
        timeoutScheduler.shutdownNow();
    }

    @Override
    public void updateMailConfiguration() {
        AdminSettings settings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
        if (settings != null) {
            JsonNode jsonConfig = settings.getJsonValue();
            mailSender = new TbMailSender(ctx, jsonConfig);
            mailFrom = jsonConfig.get("mailFrom").asText();
            timeout = jsonConfig.get("timeout").asLong(DEFAULT_TIMEOUT);
        } else {
            throw new IncorrectParameterException("Failed to update mail configuration. Settings not found!");
        }
    }

    @Override
    public void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException {
        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendTestMail(JsonNode jsonConfig, String email) throws ThingsboardException {
        TbMailSender testMailSender = new TbMailSender(ctx, jsonConfig);
        String mailFrom = jsonConfig.get("mailFrom").asText();
        String subject = messages.getMessage("test.message.subject", null, Locale.US);
        long timeout = jsonConfig.get("timeout").asLong(DEFAULT_TIMEOUT);

        Map<String, Object> model = new HashMap<>();
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("test.ftl", model);

        sendMail(testMailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendActivationEmail(String activationLink, long ttlMs, String email) throws ThingsboardException {
        String subject = messages.getMessage("activation.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("activationLink", activationLink);
        model.put("activationLinkTtlInHours", (int) Math.ceil(ttlMs / 3600000.0));
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("activation.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendAccountActivatedEmail(String loginLink, String email) throws ThingsboardException {

        String subject = messages.getMessage("account.activated.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("account.activated.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendResetPasswordEmail(String passwordResetLink, long ttlMs, String email) throws ThingsboardException {

        String subject = messages.getMessage("reset.password.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("passwordResetLink", passwordResetLink);
        model.put("passwordResetLinkTtlInHours", (int) Math.ceil(ttlMs / 3600000.0));
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("reset.password.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendResetPasswordEmailAsync(String passwordResetLink, long ttlMs, String email) {
        passwordResetExecutorService.execute(() -> {
            try {
                this.sendResetPasswordEmail(passwordResetLink, ttlMs, email);
            } catch (Exception e) {
                log.error("Error occurred: {} ", e.getMessage());
            }
        });
    }

    @Override
    public void sendPasswordWasResetEmail(String loginLink, String email) throws ThingsboardException {

        String subject = messages.getMessage("password.was.reset.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("password.was.reset.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws ThingsboardException {
        sendMail(tenantId, customerId, tbEmail, this.mailSender, timeout);
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws ThingsboardException {
        sendMail(tenantId, customerId, tbEmail, javaMailSender, timeout);
    }

    private void sendMail(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws ThingsboardException {
        if (apiUsageStateService.getApiUsageState(tenantId).isEmailSendEnabled()) {
            if (tenantId != null && !tenantId.isSysTenantId() && StringUtils.isNotEmpty(perTenantRateLimitConfig) &&
                    !rateLimitService.checkRateLimit(LimitedApi.EMAILS, (Object) tenantId, perTenantRateLimitConfig)) {
                throw new RateLimitExceededException(LimitedApi.EMAILS);
            }
            try {
                MimeMessage mailMsg = javaMailSender.createMimeMessage();
                boolean multipart = (tbEmail.getImages() != null && !tbEmail.getImages().isEmpty());
                MimeMessageHelper helper = new MimeMessageHelper(mailMsg, multipart, "UTF-8");
                helper.setFrom(StringUtils.isBlank(tbEmail.getFrom()) ? mailFrom : tbEmail.getFrom());
                helper.setTo(tbEmail.getTo().split("\\s*,\\s*"));
                if (!StringUtils.isBlank(tbEmail.getCc())) {
                    helper.setCc(tbEmail.getCc().split("\\s*,\\s*"));
                }
                if (!StringUtils.isBlank(tbEmail.getBcc())) {
                    helper.setBcc(tbEmail.getBcc().split("\\s*,\\s*"));
                }
                helper.setSubject(tbEmail.getSubject());
                helper.setText(tbEmail.getBody(), tbEmail.isHtml());

                if (multipart) {
                    for (String imgId : tbEmail.getImages().keySet()) {
                        String imgValue = tbEmail.getImages().get(imgId);
                        String value = imgValue.replaceFirst("^data:image/[^;]*;base64,?", "");
                        byte[] bytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(value);
                        String contentType = helper.getFileTypeMap().getContentType(imgId);
                        InputStreamSource iss = () -> new ByteArrayInputStream(bytes);
                        helper.addInline(imgId, iss, contentType);
                    }
                }
                sendMailWithTimeout(javaMailSender, helper.getMimeMessage(), timeout);
                apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.EMAIL_EXEC_COUNT, 1);
            } catch (Exception e) {
                throw handleException(e);
            }
        } else {
            throw new RuntimeException("Email sending is disabled due to API limits!");
        }
    }

    @Override
    public void sendAccountLockoutEmail(String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException {
        String subject = messages.getMessage("account.lockout.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("lockoutAccount", lockoutEmail);
        model.put("maxFailedLoginAttempts", maxFailedLoginAttempts);
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("account.lockout.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendTwoFaVerificationEmail(String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException {
        String subject = messages.getMessage("2fa.verification.code.subject", null, Locale.US);
        String message = mergeTemplateIntoString("2fa.verification.code.ftl", Map.of(
                TARGET_EMAIL, email,
                "code", verificationCode,
                "expirationTimeSeconds", expirationTimeSeconds
        ));

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendApiFeatureStateEmail(ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws ThingsboardException {
        String subject = messages.getMessage("api.usage.state", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("apiFeature", apiFeature.getLabel());
        model.put(TARGET_EMAIL, email);

        String message = switch (stateValue) {
            case ENABLED -> {
                model.put("apiLabel", toEnabledValueLabel(apiFeature));
                yield mergeTemplateIntoString("state.enabled.ftl", model);
            }
            case WARNING -> {
                model.put("apiValueLabel", toDisabledValueLabel(apiFeature) + " " + toWarningValueLabel(recordState));
                yield mergeTemplateIntoString("state.warning.ftl", model);
            }
            case DISABLED -> {
                model.put("apiLimitValueLabel", toDisabledValueLabel(apiFeature) + " " + toDisabledValueLabel(recordState));
                yield mergeTemplateIntoString("state.disabled.ftl", model);
            }
        };

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void testConnection(TenantId tenantId) throws Exception {
        mailSender.testConnection();
    }

    @Override
    public boolean isConfigured(TenantId tenantId) {
        return mailSender != null;
    }

    private String toEnabledValueLabel(ApiFeature apiFeature) {
        return switch (apiFeature) {
            case DB -> "save";
            case TRANSPORT -> "receive";
            case JS -> "invoke";
            case RE -> "process";
            case EMAIL, SMS -> "send";
            case ALARM -> "create";
            default -> throw new RuntimeException("Not implemented!");
        };
    }

    private String toDisabledValueLabel(ApiFeature apiFeature) {
        return switch (apiFeature) {
            case DB -> "saved";
            case TRANSPORT -> "received";
            case JS -> "invoked";
            case RE -> "processed";
            case EMAIL, SMS -> "sent";
            case ALARM -> "created";
            default -> throw new RuntimeException("Not implemented!");
        };
    }

    private String toWarningValueLabel(ApiUsageRecordState recordState) {
        String valueInM = recordState.getValueAsString();
        String thresholdInM = recordState.getThresholdAsString();
        return switch (recordState.getKey()) {
            case STORAGE_DP_COUNT, TRANSPORT_DP_COUNT -> valueInM + " out of " + thresholdInM + " allowed data points";
            case TRANSPORT_MSG_COUNT -> valueInM + " out of " + thresholdInM + " allowed messages";
            case JS_EXEC_COUNT -> valueInM + " out of " + thresholdInM + " allowed JavaScript functions";
            case TBEL_EXEC_COUNT -> valueInM + " out of " + thresholdInM + " allowed Tbel functions";
            case RE_EXEC_COUNT -> valueInM + " out of " + thresholdInM + " allowed Rule Engine messages";
            case EMAIL_EXEC_COUNT -> valueInM + " out of " + thresholdInM + " allowed Email messages";
            case SMS_EXEC_COUNT -> valueInM + " out of " + thresholdInM + " allowed SMS messages";
            default -> throw new RuntimeException("Not implemented!");
        };
    }

    private String toDisabledValueLabel(ApiUsageRecordState recordState) {
        return switch (recordState.getKey()) {
            case STORAGE_DP_COUNT, TRANSPORT_DP_COUNT -> recordState.getValueAsString() + " data points";
            case TRANSPORT_MSG_COUNT -> recordState.getValueAsString() + " messages";
            case JS_EXEC_COUNT -> "JavaScript functions " + recordState.getValueAsString() + " times";
            case TBEL_EXEC_COUNT -> "TBEL functions " + recordState.getValueAsString() + " times";
            case RE_EXEC_COUNT -> recordState.getValueAsString() + " Rule Engine messages";
            case EMAIL_EXEC_COUNT -> recordState.getValueAsString() + " Email messages";
            case SMS_EXEC_COUNT -> recordState.getValueAsString() + " SMS messages";
            default -> throw new RuntimeException("Not implemented!");
        };
    }

    private void sendMail(JavaMailSenderImpl mailSender, String mailFrom, String email,
                          String subject, String message, long timeout) throws ThingsboardException {
        try {
            MimeMessage mimeMsg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, UTF_8);
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(message, true);

            sendMailWithTimeout(mailSender, helper.getMimeMessage(), timeout);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void sendMailWithTimeout(JavaMailSender mailSender, MimeMessage msg, long timeout) throws ThingsboardException {
        var submittedMail = Futures.withTimeout(
                mailExecutorService.submit(() -> mailSender.send(msg)),
                timeout, TimeUnit.MILLISECONDS, timeoutScheduler);
        try {
            submittedMail.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout!");
        } catch (Exception e) {
            throw new ThingsboardException("Unable to send mail", ExceptionUtils.getRootCause(e), ThingsboardErrorCode.GENERAL);
        }
    }

    private String mergeTemplateIntoString(String templateLocation,
                                           Map<String, Object> model) throws ThingsboardException {
        try {
            Template template = freemarkerConfig.getTemplate(templateLocation);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (Exception e) {
            log.warn("Failed to process mail template: {}", ExceptionUtils.getRootCauseMessage(e));
            throw new ThingsboardException("Failed to process mail template: " + e.getMessage(), e, ThingsboardErrorCode.GENERAL);
        }
    }

    protected ThingsboardException handleException(Throwable exception) {
        if (exception instanceof ThingsboardException thingsboardException) {
            return thingsboardException;
        }
        if (exception instanceof NestedRuntimeException) {
            exception = ((NestedRuntimeException) exception).getMostSpecificCause();
        }
        log.warn("Unable to send mail: {}", exception.getMessage());
        return new ThingsboardException("Unable to send mail: " + exception.getMessage(), ThingsboardErrorCode.GENERAL);
    }

}
