// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import jakarta.activation.DataSource;
import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
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
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.exception.RateLimitExceededException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMailService implements MailService {

    private static final String MAIL_SETTINGS_KEY = "mail";
    private static final String TARGET_EMAIL = "targetEmail";
    private static final String UTF_8 = "UTF-8";
    private static final long DEFAULT_TIMEOUT = 10_000;

    private final ScheduledExecutorService timeoutScheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("mail-service-watchdog");

    private final AdminSettingsService adminSettingsService;
    private final BlobEntityService blobEntityService;
    private final TbApiUsageReportClient apiUsageClient;
    @Lazy
    private final TbApiUsageStateService apiUsageStateService;
    private final MailSenderInternalExecutorService mailExecutorService;
    private final PasswordResetExecutorService passwordResetExecutorService;
    private final TbMailContextComponent ctx;
    private final RateLimitService rateLimitService;
    private final ReportService reportService;
    private final WhiteLabelingService whiteLabelingService;

    @Value("${actors.rule.allow_system_mail_service}")
    private boolean allowSystemMailService;

    @Value("${mail.per_tenant_rate_limits:}")
    private String perTenantRateLimitConfig;

    @PreDestroy
    public void destroy() {
        timeoutScheduler.shutdownNow();
    }

    @Override
    public void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException {
        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendTestMail(TenantId tenantId, JsonNode jsonConfig, String email) throws ThingsboardException {
        ctx.getSecretConfigurationService().replaceSecretUsages(tenantId, jsonConfig);
        TbMailSender testMailSender = new TbMailSender(ctx, tenantId, jsonConfig);
        String mailFrom = getStringValue(jsonConfig, "mailFrom");

        JsonNode mailTemplates = whiteLabelingService.getMergedTenantMailTemplates(tenantId);
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.TEST);

        Map<String, Object> model = new HashMap<>();
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.TEST, model);

        sendMail(testMailSender, mailFrom, email, subject, message, getTimeout(jsonConfig));
    }

    @Override
    public void sendActivationEmail(TenantId tenantId, String activationLink, long ttlMs, String email) throws ThingsboardException {
        JsonNode mailTemplates = whiteLabelingService.getMergedTenantMailTemplates(tenantId);
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.ACTIVATION);

        Map<String, Object> model = new HashMap<>();
        model.put("activationLink", activationLink);
        model.put("activationLinkTtlInHours", (int) Math.ceil(ttlMs / 3600000.0));
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.ACTIVATION, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendAccountActivatedEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException {

        JsonNode mailTemplates = whiteLabelingService.getMergedTenantMailTemplates(tenantId);
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.ACCOUNT_ACTIVATED);

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.ACCOUNT_ACTIVATED, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendResetPasswordEmail(TenantId tenantId, String passwordResetLink, long ttlMs, String email) throws ThingsboardException {
        JsonNode mailTemplates = whiteLabelingService.getMergedTenantMailTemplates(tenantId);
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.RESET_PASSWORD);

        Map<String, Object> model = new HashMap<>();
        model.put("passwordResetLink", passwordResetLink);
        model.put("passwordResetLinkTtlInHours", (int) Math.ceil(ttlMs / 3600000.0));
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.RESET_PASSWORD, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendResetPasswordEmailAsync(TenantId tenantId, String passwordResetLink, long ttlMs, String email) {
        passwordResetExecutorService.execute(() -> {
            try {
                this.sendResetPasswordEmail(tenantId, passwordResetLink, ttlMs, email);
            } catch (Exception e) {
                log.error("Error occurred: {} ", e.getMessage());
            }
        });
    }

    @Override
    public void sendPasswordWasResetEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException {
        JsonNode mailTemplates = whiteLabelingService.getMergedTenantMailTemplates(tenantId);
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.PASSWORD_WAS_RESET);

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.PASSWORD_WAS_RESET, model);

        sendMail(tenantId, email, subject, message);
    }

    private void sendMail(TenantId tenantId, String email,
                          String subject, String message) throws ThingsboardException {
        JsonNode jsonConfig = getConfig(tenantId);
        TbMailSender mailSender = new TbMailSender(ctx, tenantId, jsonConfig);
        String mailFrom = getStringValue(jsonConfig, "mailFrom");
        sendMail(mailSender, mailFrom, email, subject, message, getTimeout(jsonConfig));
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws ThingsboardException {
        ConfigEntry configEntry = getConfig(tenantId, allowSystemMailService);
        JsonNode jsonConfig = configEntry.jsonConfig;
        TbMailSender mailSender = new TbMailSender(ctx, configEntry.isSystem ? TenantId.SYS_TENANT_ID : tenantId, jsonConfig);
        sendMail(tenantId, customerId, tbEmail, mailSender, false, getTimeout(jsonConfig));
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, long timeout, JavaMailSender javaMailSender) throws ThingsboardException {
        sendMail(tenantId, customerId, tbEmail, javaMailSender, true, timeout);
    }

    private void sendMail(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, boolean externalMailSender, long timeout) throws ThingsboardException {
        ConfigEntry configEntry = getConfig(tenantId, true);
        JsonNode jsonConfig = configEntry.jsonConfig;
        if (externalMailSender || !configEntry.isSystem || apiUsageStateService.getApiUsageState(tenantId).isEmailSendEnabled()) {
            if (tenantId != null && !tenantId.isSysTenantId() && StringUtils.isNotEmpty(perTenantRateLimitConfig) &&
                    !rateLimitService.checkRateLimit(LimitedApi.EMAILS, (Object) tenantId, perTenantRateLimitConfig)) {
                throw new RateLimitExceededException(LimitedApi.EMAILS);
            }
            String mailFrom = getStringValue(jsonConfig, "mailFrom");
            try {
                MimeMessage mailMsg = javaMailSender.createMimeMessage();
                boolean multipart = MapUtils.isNotEmpty(tbEmail.getImages())
                        || CollectionsUtil.isNotEmpty(tbEmail.getAttachments())
                        || CollectionsUtil.isNotEmpty(tbEmail.getReports());
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

                if (tbEmail.getAttachments() != null) {
                    for (BlobEntityId blobEntityId : tbEmail.getAttachments()) {
                        BlobEntity blobEntity = blobEntityService.findBlobEntityById(tenantId, blobEntityId);
                        if (blobEntity != null) {
                            DataSource dataSource = new ByteArrayDataSource(blobEntity.getData().array(), blobEntity.getContentType());
                            helper.addAttachment(blobEntity.getName(), dataSource);
                        }
                    }
                }
                if (tbEmail.getImages() != null) {
                    for (String imgId : tbEmail.getImages().keySet()) {
                        String imgValue = tbEmail.getImages().get(imgId);
                        String value = imgValue.replaceFirst("^data:image/[^;]*;base64,?", "");
                        byte[] bytes = jakarta.xml.bind.DatatypeConverter.parseBase64Binary(value);
                        String contentType = helper.getFileTypeMap().getContentType(imgId);
                        InputStreamSource iss = () -> new ByteArrayInputStream(bytes);
                        helper.addInline(imgId, iss, contentType);
                    }
                }
                if (tbEmail.getReports() != null) {
                    for (ReportId reportId : tbEmail.getReports()) {
                        ReportData reportData = reportService.getReportDataById(tenantId, reportId);
                        if (reportData != null) {
                            DataSource dataSource = new ByteArrayDataSource(reportData.getData(), reportData.getContentType());
                            helper.addAttachment(reportData.getName(), dataSource);
                        }
                    }
                }
                sendMailWithTimeout(javaMailSender, helper.getMimeMessage(), timeout);
                if (!externalMailSender && configEntry.isSystem) {
                    apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.EMAIL_EXEC_COUNT, 1);
                }
            } catch (Exception e) {
                throw handleException(e);
            }
        } else {
            throw new RuntimeException("Email sending is disabled due to API limits!");
        }
    }

    @Override
    public void sendAccountLockoutEmail(TenantId tenantId, String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException {
        JsonNode mailTemplates = whiteLabelingService.getMergedTenantMailTemplates(tenantId);
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.ACCOUNT_LOCKOUT);

        Map<String, Object> model = new HashMap<>();
        model.put("lockoutAccount", lockoutEmail);
        model.put("maxFailedLoginAttempts", maxFailedLoginAttempts);
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.ACCOUNT_LOCKOUT, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendTwoFaVerificationEmail(TenantId tenantId, String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException {
        sendTemplateEmail(tenantId, email, MailTemplates.TWO_FA_VERIFICATION, Map.of(
                TARGET_EMAIL, email,
                "code", verificationCode,
                "expirationTimeSeconds", expirationTimeSeconds
        ));
    }

    @Override
    public void sendApiFeatureStateEmail(TenantId tenantId, ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws ThingsboardException {
        JsonNode mailTemplates = whiteLabelingService.getMergedTenantMailTemplates(TenantId.SYS_TENANT_ID);
        String subject = null;

        Map<String, Object> model = new HashMap<>();
        model.put("apiFeature", apiFeature.getLabel());
        model.put(TARGET_EMAIL, email);

        String message = null;

        switch (stateValue) {
            case ENABLED:
                model.put("apiLabel", toEnabledValueLabel(apiFeature));
                message = body(mailTemplates, MailTemplates.API_USAGE_STATE_ENABLED, model);
                subject = MailTemplates.subject(mailTemplates, MailTemplates.API_USAGE_STATE_ENABLED);
                break;
            case WARNING:
                model.put("apiValueLabel", toDisabledValueLabel(apiFeature) + " " + toWarningValueLabel(recordState));
                message = body(mailTemplates, MailTemplates.API_USAGE_STATE_WARNING, model);
                subject = MailTemplates.subject(mailTemplates, MailTemplates.API_USAGE_STATE_WARNING);
                break;
            case DISABLED:
                model.put("apiLimitValueLabel", toDisabledValueLabel(apiFeature) + " " + toDisabledValueLabel(recordState));
                message = body(mailTemplates, MailTemplates.API_USAGE_STATE_DISABLED, model);
                subject = MailTemplates.subject(mailTemplates, MailTemplates.API_USAGE_STATE_DISABLED);
                break;
        }
        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void testConnection(TenantId tenantId) throws Exception {
        JsonNode jsonConfig = getConfig(tenantId);
        TbMailSender mailSender = new TbMailSender(ctx, tenantId, jsonConfig);
        mailSender.testConnection();
    }

    @Override
    public boolean isConfigured(TenantId tenantId) {
        try {
            ConfigEntry configEntry = getConfig(tenantId, allowSystemMailService);
            JsonNode jsonConfig = configEntry.jsonConfig;
            new TbMailSender(ctx, tenantId, jsonConfig);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendTemplateEmail(TenantId tenantId, String email, String template, Map<String, Object> templateModel) throws ThingsboardException {
        JsonNode mailTemplates = whiteLabelingService.getMergedTenantMailTemplates(tenantId);
        String subject = MailTemplates.subject(mailTemplates, template);
        String message = body(mailTemplates, template, templateModel);
        sendMail(tenantId, email, subject, message);
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

    private void sendMail(JavaMailSenderImpl mailSender,
                          String mailFrom, String email,
                          String subject, String message,
                          long timeout) throws ThingsboardException {
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


    private String getStringValue(JsonNode jsonNode, String key) {
        if (jsonNode.has(key)) {
            return jsonNode.get(key).asText();
        } else {
            return "";
        }
    }

    private long getTimeout(JsonNode jsonConfig) {
        if (jsonConfig.has("timeout")) {
            return jsonConfig.get("timeout").asLong(DEFAULT_TIMEOUT);
        } else {
            return DEFAULT_TIMEOUT;
        }
    }

    private JsonNode getConfig(TenantId tenantId) throws ThingsboardException {
        return getConfig(tenantId, true).jsonConfig;
    }

    private ConfigEntry getConfig(TenantId tenantId, boolean allowSystemMailService) throws ThingsboardException {
        try {
            JsonNode jsonConfig = null;
            boolean isSystem = false;
            if (tenantId != null && !tenantId.isNullUid()) {
                AdminSettings adminSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, MAIL_SETTINGS_KEY);
                if (adminSettings != null) {
                    jsonConfig = adminSettings.getJsonValue();
                    JsonNode useSystemMailSettingsNode = jsonConfig.get("useSystemMailSettings");
                    if (useSystemMailSettingsNode == null || useSystemMailSettingsNode.asBoolean()) {
                        jsonConfig = null;
                    }
                }
            }
            if (jsonConfig == null) {
                if (!allowSystemMailService) {
                    throw new RuntimeException("Access to System Mail Service is forbidden!");
                }
                AdminSettings settings = adminSettingsService.findAdminSettingsByKey(tenantId, MAIL_SETTINGS_KEY);
                if (settings != null) {
                    jsonConfig = settings.getJsonValue();
                    isSystem = true;
                }
            }
            if (jsonConfig == null) {
                throw new IncorrectParameterException("Failed to get mail configuration. Settings not found!");
            }
            ctx.getSecretConfigurationService().replaceSecretUsages(isSystem ? TenantId.SYS_TENANT_ID : tenantId, jsonConfig);
            return new ConfigEntry(jsonConfig, isSystem);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private static class ConfigEntry {

        JsonNode jsonConfig;
        boolean isSystem;

        ConfigEntry(JsonNode jsonConfig, boolean isSystem) {
            this.jsonConfig = jsonConfig;
            this.isSystem = isSystem;
        }

    }

    private String body(JsonNode mailTemplates, String template, Map<String, Object> model) throws ThingsboardException {
        try {
            return MailTemplates.body(mailTemplates, template, model);
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
