/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.RepositorySettingsInfo;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.config.MailOAuth2Configuration;
import org.thingsboard.server.config.MailOauth2ProviderConfiguration;
import org.thingsboard.server.config.MailOauth2Provider;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.service.security.auth.oauth2.CookieUtils;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.sync.vc.autocommit.TbAutoCommitSettingsService;
import org.thingsboard.server.service.update.UpdateService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.controller.ControllerConstants.*;

@RestController
@TbCoreComponent
@Slf4j
@RequestMapping("/api/admin")
public class AdminController extends BaseController {
    private static final String PREV_URI_PATH_PARAMETER = "prevUri";
    private static final String PREV_URI_COOKIE_NAME = "prev_uri";
    private static final String STATE_COOKIE_NAME = "state";
    private static final String MAIL_SETTINGS_KEY = "mail";
    private final StringKeyGenerator secureKeyGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder());
    @Autowired
    private MailService mailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private SystemSecurityService systemSecurityService;

    @Lazy
    @Autowired
    private JwtSettingsService jwtSettingsService;

    @Lazy
    @Autowired
    private JwtTokenFactory tokenFactory;

    @Autowired
    private EntitiesVersionControlService versionControlService;

    @Autowired
    private TbAutoCommitSettingsService autoCommitSettingsService;

    @Autowired
    private UpdateService updateService;

    @Autowired
    private MailOAuth2Configuration mailOAuth2Configuration;

    @ApiOperation(value = "Get the Administration Settings object using key (getAdminSettings)",
            notes = "Get the Administration Settings object using specified string key. Referencing non-existing key will cause an error." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings/{key}", method = RequestMethod.GET)
    @ResponseBody
    public AdminSettings getAdminSettings(
            @ApiParam(value = "A string value of the key (e.g. 'general' or 'mail').")
            @PathVariable("key") String key) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            AdminSettings adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, key), "No Administration settings found for key: " + key);
            if (adminSettings.getKey().equals("mail")) {
                ((ObjectNode) adminSettings.getJsonValue()).remove("password");
                ((ObjectNode) adminSettings.getJsonValue()).remove("refreshToken");
            }
            return adminSettings;
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "Get the Administration Settings object using key (getAdminSettings)",
            notes = "Creates or Updates the Administration Settings. Platform generates random Administration Settings Id during settings creation. " +
                    "The Administration Settings Id will be present in the response. Specify the Administration Settings Id when you would like to update the Administration Settings. " +
                    "Referencing non-existing Administration Settings Id will cause an error." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings", method = RequestMethod.POST)
    @ResponseBody
    public AdminSettings saveAdminSettings(
            @ApiParam(value = "A JSON value representing the Administration Settings.")
            @RequestBody AdminSettings adminSettings) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.WRITE);
            adminSettings.setTenantId(getTenantId());
            if (adminSettings.getJsonValue().has("enableOauth2") && adminSettings.getJsonValue().get("enableOauth2").asBoolean()){
                updateSettingsWithOauth2ProviderTemplateInfo(adminSettings);
            }
            adminSettings = checkNotNull(adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings));
            if (adminSettings.getKey().equals("mail")) {
                mailService.updateMailConfiguration();
                ((ObjectNode) adminSettings.getJsonValue()).remove("password");
                ((ObjectNode) adminSettings.getJsonValue()).remove("refreshToken");
            } else if (adminSettings.getKey().equals("sms")) {
                smsService.updateSmsConfiguration();
            }
            return adminSettings;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get the Security Settings object",
            notes = "Get the Security Settings object that contains password policy, etc." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/securitySettings", method = RequestMethod.GET)
    @ResponseBody
    public SecuritySettings getSecuritySettings() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            return checkNotNull(systemSecurityService.getSecuritySettings(TenantId.SYS_TENANT_ID));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Update Security Settings (saveSecuritySettings)",
            notes = "Updates the Security Settings object that contains password policy, etc." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/securitySettings", method = RequestMethod.POST)
    @ResponseBody
    public SecuritySettings saveSecuritySettings(
            @ApiParam(value = "A JSON value representing the Security Settings.")
            @RequestBody SecuritySettings securitySettings) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.WRITE);
            securitySettings = checkNotNull(systemSecurityService.saveSecuritySettings(TenantId.SYS_TENANT_ID, securitySettings));
            return securitySettings;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get the JWT Settings object (getJwtSettings)",
            notes = "Get the JWT Settings object that contains JWT token policy, etc. " + SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/jwtSettings", method = RequestMethod.GET)
    @ResponseBody
    public JwtSettings getJwtSettings() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            return checkNotNull(jwtSettingsService.getJwtSettings());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Update JWT Settings (saveJwtSettings)",
            notes = "Updates the JWT Settings object that contains JWT token policy, etc. The tokenSigningKey field is a Base64 encoded string." + SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/jwtSettings", method = RequestMethod.POST)
    @ResponseBody
    public JwtPair saveJwtSettings(
            @ApiParam(value = "A JSON value representing the JWT Settings.")
            @RequestBody JwtSettings jwtSettings) throws ThingsboardException {
        try {
            SecurityUser securityUser = getCurrentUser();
            accessControlService.checkPermission(securityUser, Resource.ADMIN_SETTINGS, Operation.WRITE);
            checkNotNull(jwtSettingsService.saveJwtSettings(jwtSettings));
            return tokenFactory.createTokenPair(securityUser);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Send test email (sendTestMail)",
            notes = "Attempts to send test email to the System Administrator User using Mail Settings provided as a parameter. " +
                    "You may change the 'To' email in the user profile of the System Administrator. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings/testMail", method = RequestMethod.POST)
    public void sendTestMail(
            @ApiParam(value = "A JSON value representing the Mail Settings.")
            @RequestBody AdminSettings adminSettings) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        adminSettings = checkNotNull(adminSettings);
        if (adminSettings.getKey().equals("mail")) {
            if (adminSettings.getJsonValue().has("enableOauth2") && adminSettings.getJsonValue().get("enableOauth2").asBoolean()){
                AdminSettings mailSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail"));
                JsonNode refreshToken = checkNotNull(mailSettings.getJsonValue().get("refreshToken"),
                        "Refresh token was not generated. Please save settings and generate token.");
                ((ObjectNode) adminSettings.getJsonValue()).put("refreshToken", refreshToken.asText());
                updateSettingsWithOauth2ProviderTemplateInfo(adminSettings);
            }
            else {
                if (!adminSettings.getJsonValue().has("password")) {
                    AdminSettings mailSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail"));
                    ((ObjectNode) adminSettings.getJsonValue()).put("password", mailSettings.getJsonValue().get("password").asText());
                }
            }
            String email = getCurrentUser().getEmail();
            mailService.sendTestMail(adminSettings.getJsonValue(), email);
        }
    }

    @ApiOperation(value = "Send test sms (sendTestMail)",
            notes = "Attempts to send test sms to the System Administrator User using SMS Settings and phone number provided as a parameters of the request. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings/testSms", method = RequestMethod.POST)
    public void sendTestSms(
            @ApiParam(value = "A JSON value representing the Test SMS request.")
            @RequestBody TestSmsRequest testSmsRequest) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            smsService.sendTestSms(testSmsRequest);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get repository settings (getRepositorySettings)",
            notes = "Get the repository settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings")
    public RepositorySettings getRepositorySettings() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            RepositorySettings versionControlSettings = checkNotNull(versionControlService.getVersionControlSettings(getTenantId()));
            versionControlSettings.setPassword(null);
            versionControlSettings.setPrivateKey(null);
            versionControlSettings.setPrivateKeyPassword(null);
            return versionControlSettings;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check repository settings exists (repositorySettingsExists)",
            notes = "Check whether the repository settings exists. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings/exists")
    public Boolean repositorySettingsExists() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            return versionControlService.getVersionControlSettings(getTenantId()) != null;
        } catch (Exception e) {
            throw handleException(e);
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
        ListenableFuture<RepositorySettings> future = versionControlService.saveVersionControlSettings(getTenantId(), settings);
        return wrapFuture(Futures.transform(future, savedSettings -> {
            savedSettings.setPassword(null);
            savedSettings.setPrivateKey(null);
            savedSettings.setPrivateKeyPassword(null);
            return savedSettings;
        }, MoreExecutors.directExecutor()));
    }

    @ApiOperation(value = "Delete repository settings (deleteRepositorySettings)",
            notes = "Deletes the repository settings."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/repositorySettings", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<Void> deleteRepositorySettings() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.DELETE);
            return wrapFuture(versionControlService.deleteVersionControlSettings(getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "Check repository access (checkRepositoryAccess)",
            notes = "Attempts to check repository access. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/repositorySettings/checkAccess", method = RequestMethod.POST)
    public DeferredResult<Void> checkRepositoryAccess(
            @ApiParam(value = "A JSON value representing the Repository Settings.")
            @RequestBody RepositorySettings settings) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            settings = checkNotNull(settings);
            return wrapFuture(versionControlService.checkVersionControlAccess(getTenantId(), settings));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get auto commit settings (getAutoCommitSettings)",
            notes = "Get the auto commit settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/autoCommitSettings")
    public AutoCommitSettings getAutoCommitSettings() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            return checkNotNull(autoCommitSettingsService.get(getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check auto commit settings exists (autoCommitSettingsExists)",
            notes = "Check whether the auto commit settings exists. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/autoCommitSettings/exists")
    public Boolean autoCommitSettingsExists() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            return autoCommitSettingsService.get(getTenantId()) != null;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Creates or Updates the auto commit settings (saveAutoCommitSettings)",
            notes = "Creates or Updates the auto commit settings object. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/autoCommitSettings")
    public AutoCommitSettings saveAutoCommitSettings(@RequestBody AutoCommitSettings settings) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        return autoCommitSettingsService.save(getTenantId(), settings);
    }

    @ApiOperation(value = "Delete auto commit settings (deleteAutoCommitSettings)",
            notes = "Deletes the auto commit settings."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/autoCommitSettings", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAutoCommitSettings() throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.DELETE);
            autoCommitSettingsService.delete(getTenantId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check for new Platform Releases (checkUpdates)",
            notes = "Check notifications about new platform releases. "
                    + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/updates", method = RequestMethod.GET)
    @ResponseBody
    public UpdateMessage checkUpdates() throws ThingsboardException {
        try {
            return updateService.checkUpdates();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get OAuth2 log in processing URL (getMailProcessingUrl)", notes = "Returns the URL enclosed in " +
            "double quotes. After successful authentication with OAuth2 provider and user consent for requested scope, it makes a redirect to this path so that the platform can do " +
            "further log in processing and generatin access tokens. This URL may be configured as 'mail.oauth2.loginProcessingUrl' property in yml configuration file, or " +
            "as 'MAIL_OAUTH2_LOGIN_PROCESSING_URL' env variable. By default it is '/mail/oauth2/code/'" + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/mail/oauth2/loginProcessingUrl", method = RequestMethod.GET)
    @ResponseBody
    public String getMailProcessingUrl() throws ThingsboardException {
         accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
         return "\"" + mailOAuth2Configuration.getLoginProcessingUrl() + "\"";
    }

    @ApiOperation(value = "Redirect user to mail provider login page. ", notes = "After user logged in " +
            "provider sends authorization code to specified redirect uri.)" )
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/mail/oauth2/login", method = RequestMethod.GET)
    public void authorize(HttpServletRequest request, HttpServletResponse response) throws ThingsboardException, IOException {
        String state = this.secureKeyGenerator.generateKey();
        if (request.getParameter(PREV_URI_PATH_PARAMETER) != null) {
            CookieUtils.addCookie(response, PREV_URI_COOKIE_NAME, request.getParameter(PREV_URI_PATH_PARAMETER), 180);
        }
        CookieUtils.addCookie(response, STATE_COOKIE_NAME, state, 180);

        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        AdminSettings adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, MAIL_SETTINGS_KEY), "No Administration mail settings found");
        JsonNode jsonValue = adminSettings.getJsonValue();

        String clientId = checkNotNull(jsonValue.get("clientId"), "No clientId was configured").asText();
        String authUri = checkNotNull(jsonValue.get("authUri"), "No authorization uri was configured").asText();
        String redirectUri = checkNotNull(jsonValue.get("redirectUri"), "No Redirect uri was configured").asText();
        String scope = checkNotNull(jsonValue.get("scope"), "No scope was configured").asText();

        String url = new AuthorizationCodeRequestUrl(authUri, clientId)
                .setScopes(List.of(scope))
                .setState(state)
                .setRedirectUri(redirectUri)
                .build();
        response.sendRedirect(url);
    }
    @RequestMapping(value = "/mail/oauth2/code", params = {"code", "state"}, method = RequestMethod.GET)
    public void codeProcessingUrl(
            @RequestParam(value = "code") String code, @RequestParam(value = "state") String state,
            HttpServletRequest request, HttpServletResponse response) throws ThingsboardException, IOException {
        Optional<Cookie> prevUrlOpt = CookieUtils.getCookie(request, PREV_URI_COOKIE_NAME);
        Optional<Cookie> cookieState = CookieUtils.getCookie(request, STATE_COOKIE_NAME);

        String baseUrl = this.systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
        String prevUri = baseUrl + (prevUrlOpt.isPresent() ? prevUrlOpt.get().getValue(): "/settings/outgoing-mail");

        if (cookieState.isEmpty() || !cookieState.get().getValue().equals(state)) {
            CookieUtils.deleteCookie(request, response, STATE_COOKIE_NAME);
            throw new ThingsboardException("Access token was not generated, invalid state param", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        CookieUtils.deleteCookie(request, response, STATE_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, PREV_URI_COOKIE_NAME);

        AdminSettings adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, MAIL_SETTINGS_KEY), "No Administration mail settings found");
        JsonNode jsonValue = adminSettings.getJsonValue();

        String clientId = checkNotNull(jsonValue.get("clientId"), "No clientId was configured").asText();
        String clientSecret = checkNotNull(jsonValue.get("clientSecret"), "No client secret was configured").asText();
        String clientRedirectUri = checkNotNull(jsonValue.get("redirectUri"), "No Redirect uri was configured").asText();
        String tokenUri = checkNotNull(jsonValue.get("tokenUri"), "No authorization uri was configured").asText();

        TokenResponse tokenResponse;
        try{
            tokenResponse = new AuthorizationCodeTokenRequest(new NetHttpTransport(), new GsonFactory(), new GenericUrl(tokenUri), code)
                    .setRedirectUri(clientRedirectUri)
                    .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                    .execute();
        }catch (IOException e) {
            log.warn("Unable to retrieve refresh token: {}", e.getMessage());
            throw new ThingsboardException("Error while requesting refresh token: " + e.getMessage(), ThingsboardErrorCode.GENERAL);
        }

        ((ObjectNode)jsonValue).put("refreshToken", tokenResponse.getRefreshToken());
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings);
        response.sendRedirect(prevUri);
    }

    private void updateSettingsWithOauth2ProviderTemplateInfo(AdminSettings adminSettings) throws ThingsboardException {
        MailOauth2Provider providerId;
        try {
             providerId = MailOauth2Provider.valueOf(checkNotNull(adminSettings.getJsonValue().get("providerId"),
                    "ProviderId should be configured for oauth2 type of authentication").asText());
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException("Unsupported providerId for oauth2 type of authentication. Possible variants are: "
                    + Arrays.toString(MailOauth2Provider.values()), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        MailOauth2ProviderConfiguration providerConfig = mailOAuth2Configuration.getProviderConfig(providerId);
        if (providerConfig != null) {
            ObjectNode settings = (ObjectNode) adminSettings.getJsonValue();
            JsonNode providerTenantId = adminSettings.getJsonValue().get("providerTenantId");
            settings.put("authUri", String.format(providerConfig.getAuthUri(), providerTenantId == null ? null : providerTenantId.asText()));
            settings.put("tokenUri", String.format(providerConfig.getTokenUri(), providerTenantId == null ? null : providerTenantId.asText()));
            settings.put("scope", providerConfig.getScope());
        }
    }
}
