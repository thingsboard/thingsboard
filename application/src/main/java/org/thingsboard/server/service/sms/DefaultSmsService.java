// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sms;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.sms.SmsSender;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sms.config.SmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSmsService implements SmsService {

    private static final String SMS_SETTINGS_KEY = "sms";

    @Value("${actors.rule.allow_system_sms_service}")
    private boolean allowSystemSmsService;

    private final SmsSenderFactory smsSenderFactory;
    private final AdminSettingsService adminSettingsService;
    private final TbApiUsageStateService apiUsageStateService;
    private final TbApiUsageReportClient apiUsageClient;
    private final SecretConfigurationService secretConfigurationService;

    @Override
    public void sendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) throws ThingsboardException {
        ConfigEntry configEntry = getConfig(tenantId, allowSystemSmsService);
        SmsProviderConfiguration configuration = JacksonUtil.convertValue(configEntry.jsonConfig, SmsProviderConfiguration.class);
        SmsSender smsSender = this.smsSenderFactory.createSmsSender(configuration);
        if (!configEntry.isSystem || apiUsageStateService.getApiUsageState(tenantId).isSmsSendEnabled()) {
            int smsCount = 0;
            try {
                for (String numberTo : numbersTo) {
                    smsCount += this.sendSms(smsSender, numberTo, message);
                }
            } finally {
                if (configEntry.isSystem && smsCount > 0) {
                    apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.SMS_EXEC_COUNT, smsCount);
                }
            }
        } else {
            throw new RuntimeException("SMS sending is disabled due to API limits!");
        }
    }

    @Override
    public void sendTestSms(TenantId tenantId, TestSmsRequest testSmsRequest) throws ThingsboardException {
        SmsSender testSmsSender;
        try {
            SmsProviderConfiguration configuration = secretConfigurationService.replaceSecretUsages(tenantId, testSmsRequest.getProviderConfiguration(), SmsProviderConfiguration.class);
            testSmsSender = this.smsSenderFactory.createSmsSender(configuration);
        } catch (Exception e) {
            throw handleException(e);
        }
        this.sendSms(testSmsSender, testSmsRequest.getNumberTo(), testSmsRequest.getMessage());
        testSmsSender.destroy();
    }

    @Override
    public boolean isConfigured(TenantId tenantId) {
        try {
            ConfigEntry configEntry = getConfig(tenantId, allowSystemSmsService);
            JacksonUtil.convertValue(configEntry.jsonConfig, SmsProviderConfiguration.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected int sendSms(SmsSender smsSender, String numberTo, String message) throws ThingsboardException {
        try {
            int sentSms = smsSender.sendSms(numberTo, message);
            log.trace("Successfully sent sms to number: {}", numberTo);
            return sentSms;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private ConfigEntry getConfig(TenantId tenantId, boolean allowSystemSmsService) throws ThingsboardException {
        try {
            JsonNode jsonConfig = null;
            boolean isSystem = false;
            if (tenantId != null && !tenantId.isNullUid()) {
                AdminSettings adminSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SMS_SETTINGS_KEY);
                if (adminSettings != null) {
                    jsonConfig = adminSettings.getJsonValue();
                    JsonNode useSystemSmsSettingsNode = jsonConfig.get("useSystemSmsSettings");
                    if (useSystemSmsSettingsNode == null || useSystemSmsSettingsNode.asBoolean()) {
                        jsonConfig = null;
                    }
                }
            }
            if (jsonConfig == null) {
                if (!allowSystemSmsService) {
                    throw new RuntimeException("Access to System SMS Service is forbidden!");
                }
                AdminSettings settings = adminSettingsService.findAdminSettingsByKey(tenantId, SMS_SETTINGS_KEY);
                if (settings != null) {
                    jsonConfig = settings.getJsonValue();
                    isSystem = true;
                }
            }
            if (jsonConfig == null) {
                throw new IncorrectParameterException("Failed to get sms provider configuration. Settings not found!");
            }
            secretConfigurationService.replaceSecretUsages(isSystem ? TenantId.SYS_TENANT_ID : tenantId, jsonConfig);
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

    private ThingsboardException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException) exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        log.warn("Unable to send SMS: {}", message);
        return new ThingsboardException(String.format("Unable to send SMS: %s", message),
                ThingsboardErrorCode.GENERAL);
    }

}
