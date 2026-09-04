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
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.security.model.EmailVerificationCode;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultEmailChangeService implements EmailChangeService {

    // Deliberately does not suggest requesting a new code: while capped, that request is refused too,
    // and only waiting out the current code's expiry actually unblocks the user.
    private static final String TOO_MANY_FAILURES_MESSAGE =
            "Too many failed attempts. Please wait for the pending verification code to expire, then request a new one.";

    private final TbTenantProfileCache tenantProfileCache;
    private final UserService userService;
    private final MailService mailService;
    private final AuditLogService auditLogService;
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
        // A public dashboard session has no real account behind it (synthetic user, NULL_UUID id) and must
        // not be able to trigger a mail send or reach applyEmailChange.
        if (UserPrincipal.Type.PUBLIC_ID.equals(securityUser.getUserPrincipal().getType())) {
            throw new ThingsboardException("Public users are not allowed to change email", ThingsboardErrorCode.PERMISSION_DENIED);
        }
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
            // Reject before touching the cache: writing a fresh entry here would reset its timestamp,
            // which both restarts the lockout window (the entry never reaches natural expiry if the user
            // keeps resending) and mails a new code that verifyEmailChange will still refuse outright.
            if (existing.failedAttempts() >= maxVerificationFailures) {
                throw new ThingsboardException(TOO_MANY_FAILURES_MESSAGE, ThingsboardErrorCode.TOO_MANY_REQUESTS);
            }
            if (System.currentTimeMillis() - existing.timestamp() < TimeUnit.SECONDS.toMillis(minResendPeriodSeconds)) {
                throw new ThingsboardException("A verification code has already been sent, please wait before requesting another",
                        ThingsboardErrorCode.TOO_MANY_REQUESTS);
            }
            failedAttempts = existing.failedAttempts();
        }

        String code = StringUtils.randomNumeric(6);
        // Sent before caching, so a failed send does not leave a pending code the user never learned about.
        // Depends on the templated mail path staying exempt from RecipientValidator: the new address is by
        // definition not yet an activated user of this tenant, so extending the allow-list to templated
        // sends would break every email change.
        mailService.sendTwoFaVerificationEmail(securityUser.getTenantId(), newEmail, code, codeLifetimeSeconds);
        cache.put(securityUser.getId(), new EmailVerificationCode(code, newEmail, System.currentTimeMillis(), failedAttempts));
        return EmailChangeResult.verificationRequired(codeLifetimeSeconds);
    }

    @Override
    public void verifyEmailChange(SecurityUser securityUser, String verificationCode) throws ThingsboardException {
        // Harmless today (only requestEmailChange writes the cache, and it already refuses public
        // principals, so the NULL_UUID key can never hold an entry) - guarded anyway for defence in depth.
        if (UserPrincipal.Type.PUBLIC_ID.equals(securityUser.getUserPrincipal().getType())) {
            throw new ThingsboardException("Public users are not allowed to change email", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        TbCacheValueWrapper<EmailVerificationCode> wrapper = cache.get(securityUser.getId());
        EmailVerificationCode pending = wrapper == null ? null : wrapper.get();
        if (pending == null) {
            throw new ThingsboardException("No pending email change", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (isExpired(pending)) {
            cache.evict(securityUser.getId());
            throw new ThingsboardException("Verification code is expired", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        // Keep the entry rather than evict it once the cap is hit: evicting would make the next
        // requestEmailChange see no pending entry, skip the resend throttle and reset the failure
        // counter to zero, turning the cap into an unlimited retry loop instead of a limit.
        if (pending.failedAttempts() >= maxVerificationFailures) {
            throw new ThingsboardException(TOO_MANY_FAILURES_MESSAGE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (!codeMatches(pending.code(), verificationCode)) {
            cache.put(securityUser.getId(), pending.withFailedAttempt());
            throw new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        try {
            applyEmailChange(securityUser, pending.newEmail());
        } finally {
            // Also on failure: a pending entry that outlives its request would keep the resend throttle
            // and the failure cap armed until natural expiry, blocking a retry of a change that never applied.
            cache.evict(securityUser.getId());
        }
    }

    private boolean codeMatches(String expected, String actual) {
        return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isExpired(EmailVerificationCode code) {
        return System.currentTimeMillis() - code.timestamp() > TimeUnit.SECONDS.toMillis(codeLifetimeSeconds);
    }

    private void applyEmailChange(SecurityUser securityUser, String newEmail) throws ThingsboardException {
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        if (user == null) {
            throw new ThingsboardException("User not found", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        // Validation is bypassed deliberately: UserDataValidator forbids email changes for restricted
        // tenants, and this flow is the verified route around it. Format and uniqueness were checked above.
        userService.saveUser(securityUser.getTenantId(), user, false);
        // Logged here rather than by tbUserService, which this flow bypasses along with the validator.
        auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), securityUser.getId(),
                securityUser.getName(), user.getId(), user, ActionType.EMAIL_CHANGED, null, oldEmail, newEmail);
        // Signs the user out: DefaultTokenOutdatingService caches the outdating timestamp from this event and
        // JwtAuthenticationProvider then rejects every token issued before it.
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
        notifyPreviousAddress(user.getTenantId(), oldEmail, newEmail);
    }

    // Without this notice a stolen session is a silent takeover: the attacker owns the new address, so the
    // verification code is no obstacle and the account owner never learns the account left their hands.
    private void notifyPreviousAddress(TenantId tenantId, String oldEmail, String newEmail) {
        try {
            mailService.sendEmailChangedEmail(tenantId, newEmail, oldEmail);
        } catch (Exception e) {
            // The change is already committed and the session is gone, so the notice cannot fail the request.
            log.warn("[{}] Failed to notify {} that the account email changed", tenantId, oldEmail, e);
        }
    }

}
