// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.system;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.settings.SecuritySettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.user.UserServiceImpl;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.exception.UserPasswordExpiredException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.utils.MiscUtils;
import ua_parser.Client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultSystemSecurityService implements SystemSecurityService {

    private final AdminSettingsService adminSettingsService;
    private final BCryptPasswordEncoder encoder;
    private final JwtTokenFactory tokenFactory;
    private final UserService userService;
    private final UserPermissionsService userPermissionsService;
    private final CustomerService customerService;
    private final MailService mailService;
    private final AuditLogService auditLogService;
    private final WhiteLabelingService whiteLabelingService;
    private final SecuritySettingsService securitySettingsService;

    @Override
    public void validateUserCredentials(TenantId tenantId, UserCredentials userCredentials, String username, String password) throws AuthenticationException {
        if (!userCredentials.isEnabled()) {
            throw new DisabledException("User is not active");
        }

        if (!encoder.matches(password, userCredentials.getPassword())) {
            int failedLoginAttempts = userService.increaseFailedLoginAttempts(tenantId, userCredentials.getUserId());
            SecuritySettings securitySettings = securitySettingsService.getSecuritySettings();
            if (securitySettings.getMaxFailedLoginAttempts() != null && securitySettings.getMaxFailedLoginAttempts() > 0) {
                if (failedLoginAttempts > securitySettings.getMaxFailedLoginAttempts()) {
                    lockAccount(tenantId, userCredentials.getUserId(), username, securitySettings.getUserLockoutNotificationEmail(), securitySettings.getMaxFailedLoginAttempts());
                    throw new LockedException("Authentication Failed. Username was locked due to security policy.");
                }
            }
            throw new BadCredentialsException("Authentication Failed. Username or Password not valid.");
        }

        userService.resetFailedLoginAttempts(tenantId, userCredentials.getUserId());

        SecuritySettings securitySettings = securitySettingsService.getSecuritySettings();
        if (isPositiveInteger(securitySettings.getPasswordPolicy().getPasswordExpirationPeriodDays())) {
            if ((userCredentials.getCreatedTime()
                    + TimeUnit.DAYS.toMillis(securitySettings.getPasswordPolicy().getPasswordExpirationPeriodDays()))
                    < System.currentTimeMillis()) {
                userCredentials = userService.requestExpiredPasswordReset(tenantId, userCredentials.getId());
                throw new UserPasswordExpiredException("User password expired!", userCredentials.getResetToken());
            }
        }
    }

    @Override
    public void validateTwoFaVerification(SecurityUser securityUser, boolean verificationSuccess, PlatformTwoFaSettings twoFaSettings) {
        TenantId tenantId = securityUser.getTenantId();
        UserId userId = securityUser.getId();

        int failedVerificationAttempts;
        if (!verificationSuccess) {
            failedVerificationAttempts = userService.increaseFailedLoginAttempts(tenantId, userId);
        } else {
            userService.resetFailedLoginAttempts(tenantId, userId);
            return;
        }

        Integer maxVerificationFailures = twoFaSettings.getMaxVerificationFailuresBeforeUserLockout();
        if (maxVerificationFailures != null && maxVerificationFailures > 0
                && failedVerificationAttempts >= maxVerificationFailures) {
            userService.setUserCredentialsEnabled(TenantId.SYS_TENANT_ID, userId, false);
            SecuritySettings securitySettings = securitySettingsService.getSecuritySettings();
            lockAccount(tenantId, userId, securityUser.getEmail(), securitySettings.getUserLockoutNotificationEmail(), maxVerificationFailures);
            throw new LockedException("User account was locked due to exceeded 2FA verification attempts");
        }
    }

    private void lockAccount(TenantId tenantId, UserId userId, String username, String userLockoutNotificationEmail, Integer maxFailedLoginAttempts) {
        userService.setUserCredentialsEnabled(TenantId.SYS_TENANT_ID, userId, false);
        if (StringUtils.isNotBlank(userLockoutNotificationEmail)) {
            try {
                mailService.sendAccountLockoutEmail(tenantId, username, userLockoutNotificationEmail, maxFailedLoginAttempts);
            } catch (ThingsboardException e) {
                log.warn("Can't send email regarding user account [{}] lockout to provided email [{}]", username, userLockoutNotificationEmail, e);
            }
        }
    }

    @Override
    public void validatePassword(String password, UserCredentials userCredentials) throws DataValidationException {
        SecuritySettings securitySettings = securitySettingsService.getSecuritySettings();
        UserPasswordPolicy passwordPolicy = securitySettings.getPasswordPolicy();

        validatePasswordByPolicy(password, passwordPolicy);

        if (userCredentials != null && isPositiveInteger(passwordPolicy.getPasswordReuseFrequencyDays())) {
            long passwordReuseFrequencyTs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(passwordPolicy.getPasswordReuseFrequencyDays());
            JsonNode additionalInfo = userCredentials.getAdditionalInfo();
            if (additionalInfo instanceof ObjectNode && additionalInfo.has(UserServiceImpl.USER_PASSWORD_HISTORY)) {
                JsonNode userPasswordHistoryJson = additionalInfo.get(UserServiceImpl.USER_PASSWORD_HISTORY);
                Map<String, String> userPasswordHistoryMap = JacksonUtil.convertValue(userPasswordHistoryJson, new TypeReference<>() {});
                for (Map.Entry<String, String> entry : userPasswordHistoryMap.entrySet()) {
                    if (encoder.matches(password, entry.getValue()) && Long.parseLong(entry.getKey()) > passwordReuseFrequencyTs) {
                        throw new DataValidationException("Password was already used for the last " + passwordPolicy.getPasswordReuseFrequencyDays() + " days");
                    }
                }
            }
        }
    }

    @Override
    public void validatePasswordByPolicy(String password, UserPasswordPolicy passwordPolicy) {
        List<Rule> passwordRules = new ArrayList<>();

        Integer maximumLength = passwordPolicy.getMaximumLength();
        Integer minLengthBound = passwordPolicy.getMinimumLength();
        int maxLengthBound = (maximumLength != null && maximumLength > passwordPolicy.getMinimumLength()) ? maximumLength : Integer.MAX_VALUE;

        passwordRules.add(new LengthRule(minLengthBound, maxLengthBound));
        if (isPositiveInteger(passwordPolicy.getMinimumUppercaseLetters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.UpperCase, passwordPolicy.getMinimumUppercaseLetters()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumLowercaseLetters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.LowerCase, passwordPolicy.getMinimumLowercaseLetters()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumDigits())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.Digit, passwordPolicy.getMinimumDigits()));
        }
        if (isPositiveInteger(passwordPolicy.getMinimumSpecialCharacters())) {
            passwordRules.add(new CharacterRule(EnglishCharacterData.Special, passwordPolicy.getMinimumSpecialCharacters()));
        }
        if (passwordPolicy.getAllowWhitespaces() != null && !passwordPolicy.getAllowWhitespaces()) {
            passwordRules.add(new WhitespaceRule());
        }
        PasswordValidator validator = new PasswordValidator(passwordRules);
        PasswordData passwordData = new PasswordData(password);
        RuleResult result = validator.validate(passwordData);
        if (!result.isValid()) {
            String message = String.join("\n", validator.getMessages(result));
            throw new DataValidationException(message);
        }
    }

    @Override
    public String getBaseUrl(Authority authority, TenantId tenantId, CustomerId customerId, HttpServletRequest httpServletRequest) {
        String baseUrl;
        LoginWhiteLabelingParams loginWhiteLabelingParams = null;
        if (Authority.CUSTOMER_USER.equals(authority)) {
            try {
                loginWhiteLabelingParams = whiteLabelingService.getCustomerLoginWhiteLabelingParams(tenantId, customerId);
            } catch (Exception e) {
                log.warn("Failed to fetch CustomerLoginWhiteLabelingParams.");
            }
        }

        if ((!isBaseUrlSet(loginWhiteLabelingParams) && Authority.CUSTOMER_USER.equals(authority)) || Authority.TENANT_ADMIN.equals(authority)) {
            try {
                loginWhiteLabelingParams = whiteLabelingService.getTenantLoginWhiteLabelingParams(tenantId);
            } catch (Exception e) {
                log.warn("Failed to fetch TenantLoginWhiteLabelingParams.");
            }
        }

        if (!isBaseUrlSet(loginWhiteLabelingParams)) {
            try {
                loginWhiteLabelingParams = whiteLabelingService.getSystemLoginWhiteLabelingParams();
            } catch (Exception e) {
                log.warn("Failed to fetch TenantLoginWhiteLabelingParams.");
            }
        }

        if (isBaseUrlSet(loginWhiteLabelingParams)) {
            baseUrl = loginWhiteLabelingParams.getBaseUrl();
        } else if (loginWhiteLabelingParams != null && loginWhiteLabelingParams.isProhibitDifferentUrl()) {
            return getBaseUrl(tenantId, customerId, null);
        } else {
            return getBaseUrl(tenantId, customerId, httpServletRequest);
        }
        return formatBaseUrl(baseUrl);
    }

    private boolean isBaseUrlSet(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        return loginWhiteLabelingParams != null && StringUtils.isNoneEmpty(loginWhiteLabelingParams.getBaseUrl());
    }

    @Override
    public String getBaseUrl(TenantId tenantId, CustomerId customerId, HttpServletRequest httpServletRequest) {
        String baseUrl = null;
        AdminSettings generalSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "general");

        JsonNode prohibitDifferentUrl = generalSettings.getJsonValue().get("prohibitDifferentUrl");

        if ((prohibitDifferentUrl != null && prohibitDifferentUrl.asBoolean()) || httpServletRequest == null) {
            JsonNode baseUrlNode = generalSettings.getJsonValue().get("baseUrl");
            if (baseUrlNode != null) {
                baseUrl = baseUrlNode.asText();
            }
        }

        if (StringUtils.isEmpty(baseUrl) && httpServletRequest != null) {
            baseUrl = MiscUtils.constructBaseUrl(httpServletRequest);
        }

        return formatBaseUrl(baseUrl);
    }

    @Override
    public void logLoginAction(User user, Object authenticationDetails, ActionType actionType, Exception e) {
        logLoginAction(user, authenticationDetails, actionType, null, e);
    }

    @Override
    public void logLoginAction(User user, Object authenticationDetails, ActionType actionType, String provider, Exception e) {
        String clientAddress = "Unknown";
        String browser = "Unknown";
        String os = "Unknown";
        String device = "Unknown";
        if (authenticationDetails instanceof RestAuthenticationDetails) {
            RestAuthenticationDetails details = (RestAuthenticationDetails) authenticationDetails;
            clientAddress = details.getClientAddress();
            if (details.getUserAgent() != null) {
                Client userAgent = details.getUserAgent();
                if (userAgent.userAgent != null) {
                    browser = userAgent.userAgent.family;
                    if (userAgent.userAgent.major != null) {
                        browser += " " + userAgent.userAgent.major;
                        if (userAgent.userAgent.minor != null) {
                            browser += "." + userAgent.userAgent.minor;
                            if (userAgent.userAgent.patch != null) {
                                browser += "." + userAgent.userAgent.patch;
                            }
                        }
                    }
                }
                if (userAgent.os != null) {
                    os = userAgent.os.family;
                    if (userAgent.os.major != null) {
                        os += " " + userAgent.os.major;
                        if (userAgent.os.minor != null) {
                            os += "." + userAgent.os.minor;
                            if (userAgent.os.patch != null) {
                                os += "." + userAgent.os.patch;
                                if (userAgent.os.patchMinor != null) {
                                    os += "." + userAgent.os.patchMinor;
                                }
                            }
                        }
                    }
                }
                if (userAgent.device != null) {
                    device = userAgent.device.family;
                }
            }
        }
        if (actionType == ActionType.LOGIN && e == null) {
            userService.updateLastLoginTs(user.getTenantId(), user.getId());
        }
        auditLogService.logEntityAction(
                user.getTenantId(), user.getCustomerId(), user.getId(),
                user.getName(), user.getId(), null, actionType, e, clientAddress, browser, os, device, provider);
    }

    @Override
    public AccessJwtToken createUserAccessToken(TenantId tenantId, UserId userId) throws ThingsboardException {
        User user = userService.findUserById(tenantId, userId);
        if (user == null) {
            throw new ThingsboardException("Configured user [id: " + userId + "] was not found in system. Please use other user credentials.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        UserCredentials credentials = userService.findUserCredentialsByUserId(tenantId, userId);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        MergedUserPermissions mergedUserPermissions;
        try {
            mergedUserPermissions = userPermissionsService.getMergedPermissions(user, false);
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to get user permissions", e);
        }

        SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal, mergedUserPermissions);
        return tokenFactory.createAccessJwtToken(securityUser);
    }

    @Override
    public AccessJwtToken createUserAccessTokenFromPublicId(TenantId tenantId, String publicId) throws ThingsboardException {
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException("Authentication Failed. Public Id is not valid.");
        }
        Customer publicCustomer = customerService.findCustomerById(tenantId, customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found: " + publicId);
        }
        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException("Authentication Failed. Public Id is not valid.");
        }
        User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        SecurityUser securityUser = new SecurityUser(user, true, new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, publicId), new MergedUserPermissions(new HashMap<>(), new HashMap<>()));
        return tokenFactory.createAccessJwtToken(securityUser);
    }

    private static boolean isPositiveInteger(Integer val) {
        return val != null && val > 0;
    }

    private static String formatBaseUrl(String baseUrl) {
        if (!org.apache.commons.lang3.StringUtils.startsWithAny(baseUrl, "http://", "https://")) {
            baseUrl = "https://" + baseUrl;
        }
        return baseUrl;
    }

}
