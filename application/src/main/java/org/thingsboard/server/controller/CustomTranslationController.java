// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.translation.TbCustomTranslationService;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.service.translation.DefaultTranslationService.DEFAULT_LOCALE_KEYS;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class CustomTranslationController extends BaseController {

    private static final String CUSTOM_TRANSLATION_EXAMPLE = "\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\"home\":\"MyHome\"}" +
            MARKDOWN_CODE_BLOCK_END;

    private static final String CUSTOM_TRANSLATION_PATCH_EXAMPLE = "\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\"notification.active\":\"active\"}" +
            MARKDOWN_CODE_BLOCK_END;

    @Autowired
    private TbCustomTranslationService tbCustomTranslationService;

    @Autowired
    private CustomTranslationService customTranslationService;

    @ApiOperation(value = "Get end-user Custom Translation configuration (getMergedCustomTranslation)",
            notes = "Fetch end-user Custom Translation for specified locale. The custom translation is configured in the white labeling parameters. " +
                    "If custom translation translation is defined on the tenant level, it overrides the custom translation of the system level. " +
                    "Similar, if the custom translation is defined on the customer level, it overrides the translation configuration of the tenant level."
    )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/custom/merged/{localeCode}")
    public JsonNode getMergedCustomTranslation(@Parameter(description = "Locale code (e.g. 'en_US').")
                                               @PathVariable("localeCode") String localeCode) throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        if (Authority.SYS_ADMIN.equals(authority)) {
            return customTranslationService.getCurrentCustomTranslation(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), localeCode);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            return customTranslationService.getMergedTenantCustomTranslation(getCurrentUser().getTenantId(), localeCode);
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            return customTranslationService.getMergedCustomerCustomTranslation(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), localeCode);
        }
        return JacksonUtil.newObjectNode();
    }

    @ApiOperation(value = "Get Custom Translation configuration (getCustomTranslation)",
            notes = "Fetch the Custom Translation for specified locale that corresponds to the authority of the user. " +
                    "The API call is designed to load the custom translation items for edition. " +
                    "So, the result is NOT merged with the parent level configuration. " +
                    "Let's assume there is a custom translation configured on a system level. " +
                    "And there is no custom translation items configured on a tenant level. " +
                    "In such a case, the API call will return empty object for the tenant administrator. " +
                    "\n\n Response example: " + CUSTOM_TRANSLATION_EXAMPLE +
                    ControllerConstants.WL_READ_CHECK
    )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/custom/{localeCode}")
    public JsonNode getCustomTranslation(@Parameter(description = "Locale code (e.g. 'en_US').")
                                         @PathVariable("localeCode") String localeCode) throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.READ);
        SecurityUser currentUser = getCurrentUser();
        return customTranslationService.getCurrentCustomTranslation(currentUser.getTenantId(), getCurrentUser().getCustomerId(), localeCode);
    }

    @ApiOperation(value = "Create Or Update Custom Translation (saveCustomTranslation)",
            notes = "Creates or Updates the Custom Translation for specified locale." +
                    "\n\n Request example: " + CUSTOM_TRANSLATION_EXAMPLE +
                    ControllerConstants.WL_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/translation/custom/{localeCode}")
    @ResponseStatus(value = HttpStatus.OK)
    public void saveCustomTranslation(@Parameter(description = "Locale code (e.g. 'en_US').")
                                      @PathVariable("localeCode") String localeCode,
                                      @Parameter(description = "A JSON value representing the custom translation. See API call notes above for valid example.")
                                      @RequestBody JsonNode customTranslationValue) throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.WRITE);
        DataValidator.validateLocaleCode(localeCode);
        DataValidator.validateCustomTranslationKeys(DEFAULT_LOCALE_KEYS, customTranslationValue);
        CustomTranslation customTranslation = CustomTranslation.builder()
                .tenantId(getCurrentUser().getTenantId())
                .customerId(getCurrentUser().getCustomerId())
                .localeCode(localeCode)
                .value(customTranslationValue)
                .build();
        tbCustomTranslationService.saveCustomTranslation(customTranslation);
    }

    @ApiOperation(value = "Update Custom Translation for specified translation keys only (patchCustomTranslation)",
            notes = "The API call is designed to update the custom translation for specified key only. " +
                    "\n\n Request example: " + CUSTOM_TRANSLATION_PATCH_EXAMPLE +
                    ControllerConstants.WL_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PatchMapping(value = "/translation/custom/{localeCode}")
    @ResponseStatus(value = HttpStatus.OK)
    public void patchCustomTranslation(@Parameter(description = "Locale code (e.g. 'en_US').")
                                       @PathVariable("localeCode") String localeCode,
                                       @Parameter(description = "A JSON value representing the custom translation. See API call notes above for valid example.")
                                       @RequestBody JsonNode newCustomTranslation) throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.WRITE);
        DataValidator.validateLocaleCode(localeCode);
        DataValidator.validateCustomTranslationPatch(newCustomTranslation);
        SecurityUser currentUser = getCurrentUser();
        tbCustomTranslationService.patchCustomTranslation(currentUser.getTenantId(), currentUser.getCustomerId(),
                localeCode, newCustomTranslation);
    }

    @ApiOperation(value = "Delete specified key of Custom Translation (deleteCustomTranslationKey) ",
            notes = "The API call is designed to delete specified key of the custom translation and return as a result parent translation." +
                    "(e.g. if tenant translation for key is 'value1' and customer translation is 'value2' then by deletinf key onn customer level you will get 'value1' in response) " +
                    ControllerConstants.WL_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/translation/custom/{localeCode}/{keyPath}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteCustomTranslationKey(@Parameter(description = "Locale code (e.g. 'en_US').")
                                           @PathVariable("localeCode") String localeCode,
                                           @Parameter(description = "A string value representing key of the custom translation (e.g. 'notification.active').")
                                           @PathVariable String keyPath) throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.WRITE);
        DataValidator.validateLocaleCode(localeCode);
        SecurityUser currentUser = getCurrentUser();
        tbCustomTranslationService.deleteCustomTranslationKey(currentUser.getTenantId(), currentUser.getCustomerId(),
                localeCode, keyPath);
    }

    @ApiOperation(value = "Upload Custom Translation (uploadCustomTranslation)",
            notes = "Upload the Custom Translation for specified locale." +
                    "\n\n Request example: " + CUSTOM_TRANSLATION_EXAMPLE +
                    ControllerConstants.WL_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/translation/custom/{localeCode}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public void uploadCustomTranslation(@Parameter(description = "Locale code (e.g. 'en_US').")
                                        @PathVariable("localeCode") String localeCode,
                                        @RequestPart MultipartFile file) throws Exception {
        checkWhiteLabelingPermissions(Operation.WRITE);
        DataValidator.validateLocaleCode(localeCode);

        SecurityUser user = getCurrentUser();
        CustomTranslation customTranslation = new CustomTranslation();
        customTranslation.setTenantId(user.getTenantId());
        customTranslation.setCustomerId(user.getCustomerId());
        customTranslation.setLocaleCode(localeCode);
        customTranslation.setValueBytes(file.getBytes());

        tbCustomTranslationService.saveCustomTranslation(customTranslation);
    }

    @ApiOperation(value = "Delete Custom Translation for specified locale (deleteCustomTranslation)",
            notes = "Delete entire custom translation settings for end-user" +
                    ControllerConstants.WL_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/translation/custom/{localeCode}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteCustomTranslation(@Parameter(description = "Locale code (e.g. 'en_US').")
                                        @PathVariable("localeCode") String localeCode) throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.WRITE);
        DataValidator.validateLocaleCode(localeCode);
        SecurityUser currentUser = getCurrentUser();
        tbCustomTranslationService.deleteCustomTranslation(currentUser.getTenantId(), currentUser.getCustomerId(),
                localeCode);
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
