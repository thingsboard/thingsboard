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
package org.thingsboard.server.service.user.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EmailChangeResult;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.security.model.EmailVerificationCode;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultEmailChangeService implements EmailChangeService {

    private final TbTenantProfileCache tenantProfileCache;
    private final UserService userService;
    private final MailService mailService;
    private final ApplicationEventPublisher eventPublisher;

    // Field injection, not constructor injection: this repo's lombok.config copies only @Lazy onto
    // generated constructor parameters, so a @Qualifier on a @RequiredArgsConstructor field is dropped.
    @Autowired
    @Qualifier("EmailVerificationCache")
    private TbTransactionalCache<UserId, EmailVerificationCode> cache;

    @Value("${security.email_verification.code_lifetime_seconds:900}")
    private int codeLifetimeSeconds;
    @Value("${security.email_verification.max_verification_failures:5}")
    private int maxVerificationFailures;
    @Value("${security.email_verification.min_resend_period_seconds:60}")
    private int minResendPeriodSeconds;

    @Override
    public EmailChangeResult requestEmailChange(SecurityUser securityUser, String newEmail) throws ThingsboardException {
        if (StringUtils.isBlank(newEmail)) {
            throw new ThingsboardException("Email should be specified", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        DataValidator.validateEmail(newEmail);
        if (newEmail.equals(securityUser.getEmail())) {
            throw new ThingsboardException("New email is the same as the current one", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (userService.findUserByEmail(TenantId.SYS_TENANT_ID, newEmail) != null) {
            throw new ThingsboardException("User with email '" + newEmail + "' already present in database!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        if (!tenantProfileCache.isRestricted(securityUser.getTenantId())) {
            applyEmailChange(securityUser, newEmail);
            return EmailChangeResult.success();
        }

        // Carry the failure count forward and throttle re-requests. Without both, re-requesting a code
        // resets the counter and the six-digit space can be searched indefinitely, and the endpoint
        // becomes an unmetered mail channel to an arbitrary address.
        TbCacheValueWrapper<EmailVerificationCode> existingWrapper = cache.get(securityUser.getId());
        EmailVerificationCode existing = existingWrapper == null ? null : existingWrapper.get();
        int failedAttempts = 0;
        if (existing != null && !isExpired(existing)) {
            if (System.currentTimeMillis() - existing.timestamp() < TimeUnit.SECONDS.toMillis(minResendPeriodSeconds)) {
                throw new ThingsboardException("A verification code has already been sent, please wait before requesting another",
                        ThingsboardErrorCode.TOO_MANY_REQUESTS);
            }
            failedAttempts = existing.failedAttempts();
        }

        String code = StringUtils.randomNumeric(6);
        // Sent before caching, so a failed send does not leave a pending code the user never learned about.
        mailService.sendTwoFaVerificationEmail(securityUser.getTenantId(), newEmail, code, codeLifetimeSeconds);
        cache.put(securityUser.getId(), new EmailVerificationCode(code, newEmail, System.currentTimeMillis(), failedAttempts));
        return EmailChangeResult.verificationRequired(codeLifetimeSeconds);
    }

    @Override
    public void verifyEmailChange(SecurityUser securityUser, String verificationCode) throws ThingsboardException {
        TbCacheValueWrapper<EmailVerificationCode> wrapper = cache.get(securityUser.getId());
        EmailVerificationCode pending = wrapper == null ? null : wrapper.get();
        if (pending == null) {
            throw new ThingsboardException("No pending email change", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (isExpired(pending)) {
            cache.evict(securityUser.getId());
            throw new ThingsboardException("Verification code is expired", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (!pending.code().equals(verificationCode)) {
            EmailVerificationCode updated = pending.withFailedAttempt();
            if (updated.failedAttempts() >= maxVerificationFailures) {
                cache.evict(securityUser.getId());
            } else {
                cache.put(securityUser.getId(), updated);
            }
            throw new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        applyEmailChange(securityUser, pending.newEmail());
        cache.evict(securityUser.getId());
    }

    private boolean isExpired(EmailVerificationCode code) {
        return System.currentTimeMillis() - code.timestamp() > TimeUnit.SECONDS.toMillis(codeLifetimeSeconds);
    }

    private void applyEmailChange(SecurityUser securityUser, String newEmail) {
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        user.setEmail(newEmail);
        // Validation is bypassed deliberately: UserDataValidator forbids email changes for restricted
        // tenants, and this flow is the verified route around it. Format and uniqueness were checked above.
        userService.saveUser(securityUser.getTenantId(), user, false);
        // The JWT subject is the email, so every existing token for this user now names an address that no longer resolves.
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
    }

}
