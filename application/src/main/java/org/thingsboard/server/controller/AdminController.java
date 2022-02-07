/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.service.update.UpdateService;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api/admin")
public class AdminController extends BaseController {

    @Autowired
    private MailService mailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private SystemSecurityService systemSecurityService;

    @Autowired
    private UpdateService updateService;

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
            adminSettings = checkNotNull(adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings));
            if (adminSettings.getKey().equals("mail")) {
                mailService.updateMailConfiguration();
                ((ObjectNode) adminSettings.getJsonValue()).remove("password");
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

    @ApiOperation(value = "Send test email (sendTestMail)",
            notes = "Attempts to send test email to the System Administrator User using Mail Settings provided as a parameter. " +
                    "You may change the 'To' email in the user profile of the System Administrator. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings/testMail", method = RequestMethod.POST)
    public void sendTestMail(
            @ApiParam(value = "A JSON value representing the Mail Settings.")
            @RequestBody AdminSettings adminSettings) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            adminSettings = checkNotNull(adminSettings);
            if (adminSettings.getKey().equals("mail")) {
                if (!adminSettings.getJsonValue().has("password")) {
                    AdminSettings mailSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail"));
                    ((ObjectNode) adminSettings.getJsonValue()).put("password", mailSettings.getJsonValue().get("password").asText());
                }
                String email = getCurrentUser().getEmail();
                mailService.sendTestMail(adminSettings.getJsonValue(), email);
            }
        } catch (Exception e) {
            throw handleException(e);
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

}
