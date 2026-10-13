// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.LicenseInfo;
import org.thingsboard.server.common.data.LicenseUsageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.RepositorySettingsInfo;
import org.thingsboard.server.common.data.sync.vc.VcUtils;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.settings.SecuritySettingsService;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.service.security.auth.oauth2.CookieUtils;
import org.thingsboard.server.service.security.auth.oauth2.PrevUriValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.sync.vc.autocommit.TbAutoCommitSettingsService;
import org.thingsboard.server.service.system.SystemInfoService;
import org.thingsboard.server.service.update.UpdateService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_COOKIE_NAME;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_PARAMETER;

@RestController
@TbCoreComponent
@Slf4j
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController extends BaseController {

    private final MailService mailService;
    private final SmsService smsService;
    private final AdminSettingsService adminSettingsService;
    private final SystemSecurityService systemSecurityService;
    private final SecuritySettingsService securitySettingsService;
    private final JwtSettingsService jwtSettingsService;
    private final JwtTokenFactory tokenFactory;
    private final EntitiesVersionControlService versionControlService;
    private final TbAutoCommitSettingsService autoCommitSettingsService;
    private final UpdateService updateService;
    private final SystemInfoService systemInfoService;
    private final SubscriptionService subscriptionService;
    private final AuditLogService auditLogService;
    private final SecretConfigurationService secretConfigurationService;
    private final TbTransactionalCache<String, TenantId> oauth2StateCache;

    private static final String DEFAULT_PREV_URI = "/settings/outgoing-mail";
    private static final String STATE_COOKIE_NAME = "state";
    private static final String MAIL_SETTINGS_KEY = "mail";

    protected static final String RESOURCE_READ_CHECK = "\n\nSecurity check is performed to verify that " +
            "the user has 'READ' permission for the 'ADMIN_SETTINGS' (for 'SYS_ADMIN' authority) or 'WHITE_LABELING' (for 'TENANT_ADMIN' authority) resource.";
    protected static final String RESOURCE_WRITE_CHECK = "\n\nSecurity check is performed to verify that " +
            "the user has 'WRITE' permission for the 'ADMIN_SETTINGS' (for 'SYS_ADMIN' authority) or 'WHITE_LABELING' (for 'TENANT_ADMIN' authority) resource.";

    @Value("${queue.vc.request-timeout:180000}")
    private int vcRequestTimeout;

    @ApiOperation(value = "Get the Administration Settings object using key (getAdminSettings)",
            notes = "Get the Administration Settings object using specified string key. " +
                    "Referencing non-existing key will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + RESOURCE_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/settings/{key}")
    public AdminSettings getAdminSettings(
            @Parameter(description = "A string value of the key (e.g. 'general' or 'mail').")
            @PathVariable("key") String key,
            @Parameter(description = "Use system settings if settings are not defined on tenant level.")
            @RequestParam(required = false, defaultValue = "false") boolean systemByDefault) throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        AdminSettings adminSettings;
        if (Authority.SYS_ADMIN.equals(authority)) {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, key), "No Administration settings found for key: " + key);
        } else {
            accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.READ);
            adminSettings = getTenantAdminSettings(getTenantId(), key, systemByDefault);
        }
        if (adminSettings.getKey().equals(MAIL_SETTINGS_KEY)) {
            ((ObjectNode) adminSettings.getJsonValue()).remove("refreshToken");
        }
        return adminSettings;
    }

    @ApiOperation(value = "Creates or Updates the Administration Settings (saveAdminSettings)",
            notes = "Creates or Updates the Administration Settings. Platform generates random Administration Settings Id during settings creation. " +
                    "The Administration Settings Id will be present in the response. Specify the Administration Settings Id when you would like to update the Administration Settings. " +
                    "Referencing non-existing Administration Settings Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + RESOURCE_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/settings")
    public AdminSettings saveAdminSettings(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the Administration Settings.")
            @RequestBody AdminSettings adminSettings) throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        TenantId tenantId = getTenantId();
        adminSettings.setTenantId(tenantId);
        if (Authority.SYS_ADMIN.equals(authority)) {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.WRITE);
        } else {
            accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE);
        }
        adminSettings = checkNotNull(adminSettingsService.saveAdminSettings(tenantId, adminSettings));
        if (adminSettings.getKey().equals(MAIL_SETTINGS_KEY)) {
            ((ObjectNode) adminSettings.getJsonValue()).remove("refreshToken");
        }
        return adminSettings;
    }

    @ApiOperation(value = "Get the Security Settings object (getSecuritySettings)",
            notes = "Get the Security Settings object that contains password policy, etc." + SYSTEM_AUTHORITY_PARAGRAPH + RESOURCE_READ_CHECK)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/securitySettings")
    public SecuritySettings getSecuritySettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        return checkNotNull(securitySettingsService.getSecuritySettings());
    }

    @ApiOperation(value = "Update Security Settings (saveSecuritySettings)",
            notes = "Updates the Security Settings object that contains password policy, etc." + SYSTEM_AUTHORITY_PARAGRAPH + RESOURCE_WRITE_CHECK)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping(value = "/securitySettings")
    public SecuritySettings saveSecuritySettings(
            @Parameter(description = "A JSON value representing the Security Settings.")
            @RequestBody SecuritySettings securitySettings) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.WRITE);
        securitySettings = checkNotNull(securitySettingsService.saveSecuritySettings(securitySettings));
        return securitySettings;
    }

    @ApiOperation(value = "Get the JWT Settings object (getJwtSettings)",
            notes = "Get the JWT Settings object that contains JWT token policy, etc. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/jwtSettings")
    public JwtSettings getJwtSettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        return checkNotNull(jwtSettingsService.getJwtSettings());
    }

    @ApiOperation(value = "Update JWT Settings (saveJwtSettings)",
            notes = "Updates the JWT Settings object that contains JWT token policy, etc. The tokenSigningKey field is a Base64 encoded string." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping(value = "/jwtSettings")
    public JwtPair saveJwtSettings(
            @Parameter(description = "A JSON value representing the JWT Settings.")
            @RequestBody JwtSettings jwtSettings) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        accessControlService.checkPermission(securityUser, Resource.ADMIN_SETTINGS, Operation.WRITE);
        checkNotNull(jwtSettingsService.saveJwtSettings(jwtSettings));
        return tokenFactory.createTokenPair(securityUser);
    }

    @ApiOperation(value = "Send test email (sendTestMail)",
            notes = "Attempts to send test email using Mail Settings provided as a parameter. " +
                    "Email is sent to the address specified in the profile of user who is performing the request" +
                    "You may change the 'To' email in the user profile of the System/Tenant Administrator. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + RESOURCE_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/settings/testMail")
    public void sendTestMail(
            @Parameter(description = "A JSON value representing the Mail Settings.")
            @RequestBody AdminSettings adminSettings) throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        if (Authority.SYS_ADMIN.equals(authority)) {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        } else {
            accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.READ);
        }
        adminSettings = checkNotNull(adminSettings);
        if (adminSettings.getKey().equals(MAIL_SETTINGS_KEY)) {
            AdminSettings mailSettings;
            if (Authority.SYS_ADMIN.equals(authority)) {
                mailSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, MAIL_SETTINGS_KEY));
            } else {
                mailSettings = getTenantAdminSettings(getTenantId(), MAIL_SETTINGS_KEY, false);
            }
            if (adminSettings.getJsonValue().has("enableOauth2") && adminSettings.getJsonValue().get("enableOauth2").asBoolean()) {
                JsonNode refreshToken = mailSettings.getJsonValue().get("refreshToken");
                if (refreshToken == null) {
                    throw new ThingsboardException("Refresh token was not generated. Please, generate refresh token.", ThingsboardErrorCode.GENERAL);
                }
                ((ObjectNode) adminSettings.getJsonValue()).put("refreshToken", refreshToken.asText());
            } else {
                if (!adminSettings.getJsonValue().has("password")) {
                    ((ObjectNode) adminSettings.getJsonValue()).put("password", mailSettings.getJsonValue().get("password").asText());
                }
            }
            String email = getCurrentUser().getEmail();
            try {
                mailService.sendTestMail(getTenantId(), adminSettings.getJsonValue(), email);
            } catch (ThingsboardException e) {
                String error = e.getMessage();
                if (e.getCause() != null) {
                    error += ": " + e.getCause().getMessage(); // showing actual underlying error for testing purposes
                }
                throw new ThingsboardException(error, e.getErrorCode());
            }
        }
    }

    @ApiOperation(value = "Send test sms (sendTestSms)",
            notes = "Attempts to send test sms to the System Administrator User using SMS Settings and phone number provided as a parameters of the request. "
                    + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + RESOURCE_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/settings/testSms")
    public void sendTestSms(
            @Parameter(description = "A JSON value representing the Test SMS request.")
            @RequestBody TestSmsRequest testSmsRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        if (Authority.SYS_ADMIN.equals(user.getAuthority())) {
            accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.READ);
        } else {
            accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.READ);
        }
        try {
            smsService.sendTestSms(user.getTenantId(), testSmsRequest);
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(), user.getId(), user, ActionType.SMS_SENT, null, testSmsRequest.getNumberTo());
        } catch (ThingsboardException e) {
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(), user.getId(), user, ActionType.SMS_SENT, e, testSmsRequest.getNumberTo());
            throw e;
        }
    }

    @ApiOperation(value = "Get repository settings (getRepositorySettings)",
            notes = "Get the repository settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings")
    public RepositorySettings getRepositorySettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return checkNotNull(versionControlService.getVersionControlSettings(getTenantId()));
    }

    @ApiOperation(value = "Check repository settings exists (repositorySettingsExists)",
            notes = "Check whether the repository settings exists. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings/exists")
    public Boolean repositorySettingsExists() throws ThingsboardException {
        if (accessControlService.hasPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ)) {
            return versionControlService.getVersionControlSettings(getTenantId()) != null;
        } else {
            return false;
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings/info")
    public RepositorySettingsInfo getRepositorySettingsInfo() throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        RepositorySettings repositorySettings = versionControlService.getVersionControlSettings(getTenantId());
        if (repositorySettings != null) {
            return RepositorySettingsInfo.builder()
                    .configured(true)
                    .readOnly(repositorySettings.isReadOnly())
                    .build();
        } else {
            return RepositorySettingsInfo.builder()
                    .configured(false)
                    .build();
        }
    }

    @ApiOperation(value = "Creates or Updates the repository settings (saveRepositorySettings)",
            notes = "Creates or Updates the repository settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/repositorySettings")
    public DeferredResult<RepositorySettings> saveRepositorySettings(@RequestBody RepositorySettings settings) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        settings.setLocalOnly(false); // only to be used in tests
        return wrapFuture(versionControlService.saveVersionControlSettings(getTenantId(), settings), vcRequestTimeout);
    }

    @ApiOperation(value = "Delete repository settings (deleteRepositorySettings)",
            notes = "Deletes the repository settings."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/repositorySettings")
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<Void> deleteRepositorySettings() throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.DELETE);
        return wrapFuture(versionControlService.deleteVersionControlSettings(getTenantId()), vcRequestTimeout);
    }

    @ApiOperation(value = "Check repository access (checkRepositoryAccess)",
            notes = "Attempts to check repository access. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/repositorySettings/checkAccess")
    public DeferredResult<Void> checkRepositoryAccess(
            @Parameter(description = "A JSON value representing the Repository Settings.")
            @RequestBody RepositorySettings settings) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        settings.setLocalOnly(false); // only to be used in tests
        return wrapFuture(versionControlService.checkVersionControlAccess(getTenantId(), settings), vcRequestTimeout);
    }

    @ApiOperation(value = "Get auto commit settings (getAutoCommitSettings)",
            notes = "Get the auto commit settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/autoCommitSettings")
    public AutoCommitSettings getAutoCommitSettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        return checkNotNull(autoCommitSettingsService.get(getTenantId()));
    }

    @ApiOperation(value = "Check auto commit settings exists (autoCommitSettingsExists)",
            notes = "Check whether the auto commit settings exists. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/autoCommitSettings/exists")
    public Boolean autoCommitSettingsExists() throws ThingsboardException {
        if (accessControlService.hasPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ)) {
            return autoCommitSettingsService.get(getTenantId()) != null;
        } else {
            return false;
        }
    }

    @ApiOperation(value = "Creates or Updates the auto commit settings (saveAutoCommitSettings)",
            notes = "Creates or Updates the auto commit settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/autoCommitSettings")
    public AutoCommitSettings saveAutoCommitSettings(@RequestBody AutoCommitSettings settings) throws ThingsboardException {
        settings.values().forEach(config -> VcUtils.checkBranchName(config.getBranch()));
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        return autoCommitSettingsService.save(getTenantId(), settings);
    }

    @ApiOperation(value = "Delete auto commit settings (deleteAutoCommitSettings)",
            notes = "Deletes the auto commit settings."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/autoCommitSettings")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAutoCommitSettings() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.DELETE);
        autoCommitSettingsService.delete(getTenantId());
    }

    @ApiOperation(value = "Check for new Platform Releases (checkUpdates)",
            notes = "Check notifications about new platform releases. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/updates")
    public UpdateMessage checkUpdates() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        return updateService.checkUpdates();
    }

    @ApiOperation(value = "Get license usage info (getLicenseUsageInfo)",
            notes = "Get license usage info. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/licenseUsageInfo")
    public LicenseUsageInfo getLicenseUsageInfo() throws ThingsboardException {
        LicenseInfo licenseInfo = subscriptionService.getLicenseInfo();
        LicenseUsageInfo licenseUsageInfo = new LicenseUsageInfo(licenseInfo);
        licenseUsageInfo.setDevicesCount(deviceService.countDevices());
        licenseUsageInfo.setAssetsCount(assetService.countAssets());
        licenseUsageInfo.setEdgesCount(edgeService.countEdges());
        licenseUsageInfo.setDashboardsCount(dashboardService.countDashboards());
        licenseUsageInfo.setIntegrationsCount(integrationService.countCoreIntegrations());
        return licenseUsageInfo;
    }

    @ApiOperation(value = "Get subscription info (getSubscriptionInfo)",
            notes = "Get subscription info. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/subscriptionInfo")
    public SubscriptionInfo getSubscriptionInfo() throws ThingsboardException {
        SubscriptionInfo subscriptionInfo = subscriptionService.getSubscriptionInfo();
        enrichSubscriptionInfo(subscriptionInfo);
        return subscriptionInfo;
    }

    @ApiOperation(value = "Refresh license (refreshLicense)",
            notes = "Refresh license info. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping(value = "/refreshLicense")
    public SubscriptionInfo refreshLicense() throws ThingsboardException {
        SubscriptionInfo subscriptionInfo = subscriptionService.refreshLicense();
        enrichSubscriptionInfo(subscriptionInfo);
        return subscriptionInfo;
    }

    @ApiOperation(value = "Get system info (getSystemInfo)",
            notes = "Get main information about system. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/systemInfo")
    public SystemInfo getSystemInfo() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        return systemInfoService.getSystemInfo();
    }

    @ApiOperation(value = "Get features info (getFeaturesInfo)",
            notes = "Get information about enabled/disabled features. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/featuresInfo")
    public FeaturesInfo getFeaturesInfo() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        return systemInfoService.getFeaturesInfo();
    }

    @ApiOperation(value = "Get OAuth2 log in processing URL (getMailProcessingUrl)", notes = "Returns the URL enclosed in " +
            "double quotes. After successful authentication with OAuth2 provider and user consent for requested scope, it makes a redirect to this path so that the platform can do " +
            "further log in processing and generating access tokens. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/mail/oauth2/loginProcessingUrl")
    public String getMailProcessingUrl() {
        return "\"/api/admin/mail/oauth2/code\"";
    }

    @ApiOperation(value = "Redirect user to mail provider login page. ", notes = "After user logged in and provided access" +
            "provider sends authorization code to specified redirect uri.)")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/mail/oauth2/authorize", produces = "application/text")
    public String getMailOAuth2AuthorizationUrl(HttpServletRequest request, HttpServletResponse response) throws Exception {
        SecurityUser currentUser = getCurrentUser();
        String state = StringUtils.generateSafeToken();
        String prevUriParam = request.getParameter(PREV_URI_PARAMETER);
        if (PrevUriValidator.isValid(prevUriParam)) {
            CookieUtils.addCookie(response, PREV_URI_COOKIE_NAME, prevUriParam, 180);
        }
        CookieUtils.addCookie(response, STATE_COOKIE_NAME, state, 180);
        oauth2StateCache.put(state, currentUser.getTenantId());

        AdminSettings adminSettings;
        if (Authority.SYS_ADMIN.equals(currentUser.getAuthority())) {
            accessControlService.checkPermission(currentUser, Resource.ADMIN_SETTINGS, Operation.READ);
            adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, MAIL_SETTINGS_KEY), "No Administration settings found for key: " + MAIL_SETTINGS_KEY);
        } else {
            accessControlService.checkPermission(currentUser, Resource.WHITE_LABELING, Operation.READ);
            adminSettings = getTenantAdminSettings(currentUser.getTenantId(), MAIL_SETTINGS_KEY, true);
        }
        JsonNode jsonValue = adminSettings.getJsonValue();
        String clientId = checkNotNull(jsonValue.get("clientId"), "No clientId was configured").asText();
        String authUri = checkNotNull(jsonValue.get("authUri"), "No authorization uri was configured").asText();
        String redirectUri = checkNotNull(jsonValue.get("redirectUri"), "No Redirect uri was configured").asText();
        List<String> scope = JacksonUtil.convertValue(checkNotNull(jsonValue.get("scope"), "No scope was configured"), new TypeReference<>() {
        });

        return "\"" + new AuthorizationCodeRequestUrl(authUri, clientId)
                .setScopes(scope)
                .setState(state)
                .setRedirectUri(redirectUri)
                .build() + "\"";
    }

    @GetMapping(value = "/mail/oauth2/code",  params = {"code", "state"})
    public void handleMailOAuth2Callback(@RequestParam(value = "code") String code, @RequestParam(value = "state") String state,
                                  HttpServletRequest request, HttpServletResponse response) throws Exception {
        String redirectUrl = getMailOAuth2RedirectUrl(request);
        Optional<Cookie> cookieState = CookieUtils.getCookie(request, STATE_COOKIE_NAME);

        if (cookieState.isEmpty() || !cookieState.get().getValue().equals(state)) {
            CookieUtils.deleteCookie(request, response, STATE_COOKIE_NAME);
            throw new ThingsboardException("Refresh token was not generated, invalid state param", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        var tenantWrapper = oauth2StateCache.get(state);
        if (tenantWrapper == null) {
            throw new ThingsboardException("State parameter is not valid", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        TenantId tenantId = tenantWrapper.get();
        oauth2StateCache.evict(state);

        CookieUtils.deleteCookie(request, response, STATE_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, PREV_URI_COOKIE_NAME);

        AdminSettings adminSettings;
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(tenantId, MAIL_SETTINGS_KEY), "No Administration mail settings found");
        } else {
            adminSettings = getTenantAdminSettings(tenantId, MAIL_SETTINGS_KEY, false);
        }
        JsonNode jsonValue = adminSettings.getJsonValue().deepCopy();
        secretConfigurationService.replaceSecretUsages(tenantId, jsonValue);

        String clientId = checkNotNull(jsonValue.get("clientId"), "No clientId was configured").asText();
        String clientSecret = checkNotNull(jsonValue.get("clientSecret"), "No client secret was configured").asText();
        String clientRedirectUri = checkNotNull(jsonValue.get("redirectUri"), "No Redirect uri was configured").asText();
        String tokenUri = checkNotNull(jsonValue.get("tokenUri"), "No authorization uri was configured").asText();

        TokenResponse tokenResponse;
        try {
            tokenResponse = new AuthorizationCodeTokenRequest(new NetHttpTransport(), new GsonFactory(), new GenericUrl(tokenUri), code)
                    .setRedirectUri(clientRedirectUri)
                    .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                    .execute();
        } catch (IOException e) {
            log.warn("Unable to retrieve refresh token: {}", e.getMessage());
            throw new ThingsboardException("Error while requesting access token: " + e.getMessage(), ThingsboardErrorCode.GENERAL);
        }
        ((ObjectNode) adminSettings.getJsonValue()).put("refreshToken", tokenResponse.getRefreshToken());
        ((ObjectNode) adminSettings.getJsonValue()).put("tokenGenerated", true);

        adminSettingsService.saveAdminSettings(tenantId, adminSettings);
        response.sendRedirect(redirectUrl);
    }

    String getMailOAuth2RedirectUrl(HttpServletRequest request) {
        String baseUrl = this.systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
        String prevUri = CookieUtils.getCookie(request, PREV_URI_COOKIE_NAME)
                .map(Cookie::getValue)
                .filter(PrevUriValidator::isValid)
                .orElse(DEFAULT_PREV_URI);
        return baseUrl + prevUri;
    }

    private AdminSettings getTenantAdminSettings(TenantId tenantId, String key, boolean systemByDefault) throws Exception {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, key);
        if (adminSettings == null) {
            if (systemByDefault) {
                return checkNotNull(adminSettingsService.findAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, key));
            } else {
                adminSettings = new AdminSettings();
                adminSettings.setTenantId(tenantId);
                adminSettings.setKey(key);
                adminSettings.setJsonValue(JacksonUtil.newObjectNode());
                return adminSettings;
            }
        }
        return adminSettings;
    }

}
