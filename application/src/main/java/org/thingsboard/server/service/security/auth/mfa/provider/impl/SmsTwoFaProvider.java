// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.mfa.provider.impl;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.SmsTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

@Service
@TbCoreComponent
public class SmsTwoFaProvider extends OtpBasedTwoFaProvider<SmsTwoFaProviderConfig, SmsTwoFaAccountConfig> {

    private final SmsService smsService;
    private final AuditLogService auditLogService;

    public SmsTwoFaProvider(CacheManager cacheManager, SmsService smsService, AuditLogService auditLogService) {
        super(cacheManager);
        this.smsService = smsService;
        this.auditLogService = auditLogService;
    }


    @Override
    public SmsTwoFaAccountConfig generateNewAccountConfig(User user, SmsTwoFaProviderConfig providerConfig) {
        return new SmsTwoFaAccountConfig();
    }

    @Override
    protected void sendVerificationCode(SecurityUser user, String verificationCode, SmsTwoFaProviderConfig providerConfig, SmsTwoFaAccountConfig accountConfig) throws ThingsboardException {
        Map<String, String> messageData = Map.of(
                "code", verificationCode,
                "userEmail", user.getEmail()
        );
        String message = TbNodeUtils.processTemplate(providerConfig.getSmsVerificationMessageTemplate(), messageData);
        String phoneNumber = accountConfig.getPhoneNumber();
        try {
            smsService.sendSms(user.getTenantId(), user.getCustomerId(), new String[]{phoneNumber}, message);
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(), user.getId(), user, ActionType.SMS_SENT, null, phoneNumber);
        } catch (ThingsboardException e) {
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(), user.getId(), user, ActionType.SMS_SENT, e, phoneNumber);
            throw e;
        }
    }

    @Override
    public void check(TenantId tenantId) throws ThingsboardException {
        if (!smsService.isConfigured(tenantId)) {
            throw new ThingsboardException("SMS service is not configured", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }


    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.SMS;
    }

}
