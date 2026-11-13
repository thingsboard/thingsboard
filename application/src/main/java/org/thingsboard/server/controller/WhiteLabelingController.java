// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.mail.MailTemplates;
import org.thingsboard.server.service.security.model.SecurityUser;

import static org.thingsboard.server.common.data.permission.Operation.READ;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.WL_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.WL_WRITE_CHECK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class WhiteLabelingController extends BaseController {

    @ApiOperation(value = "Get White Labeling parameters",
            notes = "Returns white-labeling parameters for the current user.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/whiteLabel/whiteLabelParams", produces = "application/json")
    public WhiteLabelingParams getWhiteLabelParams() throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        WhiteLabelingParams whiteLabelingParams = null;
        boolean wlEnabled = subscriptionService.whiteLabelingEnabled(getCurrentUser().getTenantId());
        if (!wlEnabled) {
            whiteLabelingParams = new WhiteLabelingParams();
            whiteLabelingParams.setHelpLinkBaseUrl("https://thingsboard.io");
            whiteLabelingParams.setEnableHelpLinks(true);
        } else {
            if (Authority.SYS_ADMIN.equals(authority)) {
                whiteLabelingParams = whiteLabelingService.getSystemWhiteLabelingParams();
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                whiteLabelingParams = whiteLabelingService.getMergedTenantWhiteLabelingParams(getTenantId());
            } else if (Authority.CUSTOMER_USER.equals(authority)) {
                whiteLabelingParams = whiteLabelingService.getMergedCustomerWhiteLabelingParams(getTenantId(),
                        getCurrentUser().getCustomerId());
            }
        }
        whiteLabelingParams.setWhiteLabelingEnabled(wlEnabled);
        return whiteLabelingParams;
    }

    @ApiOperation(value = "Get Login White Labeling parameters",
            notes = "Returns login white-labeling parameters based on the hostname from request.")
    @GetMapping(value = "/noauth/whiteLabel/loginWhiteLabelParams", produces = "application/json")
    public LoginWhiteLabelingParams getLoginWhiteLabelParams(HttpServletRequest request) throws Exception {
        boolean wlEnabled = subscriptionService.whiteLabelingEnabled(TenantId.SYS_TENANT_ID);
        if (!wlEnabled) {
            return new LoginWhiteLabelingParams();
        } else {
            return whiteLabelingService.getMergedLoginWhiteLabelingParams(request.getServerName());
        }
    }

    @ApiOperation(value = "Get White Labeling configuration (getCurrentWhiteLabelParams)",
            notes = "Fetch the White Labeling configuration that corresponds to the authority of the user. " +
                    "The API call is designed to load the White Labeling configuration for edition. " +
                    "So, the result is NOT merged with the parent level White Labeling configuration. " +
                    "Let's assume there is a custom White Labeling  configured on a system level. " +
                    "And there is no custom White Labeling  items configured on a tenant level. " +
                    "In such a case, the API call will return default object for the tenant administrator. " +
                    WL_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/whiteLabel/currentWhiteLabelParams", produces = "application/json")
    public WhiteLabelingParams getCurrentWhiteLabelParams(@Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
                                                          @RequestParam(value = "customerId", required = false) String strCustomerId) throws ThingsboardException {
        boolean wlEnabled = subscriptionService.whiteLabelingEnabled(getCurrentUser().getTenantId());
        if (!wlEnabled) {
            WhiteLabelingParams whiteLabelingParams = new WhiteLabelingParams();
            whiteLabelingParams.setHelpLinkBaseUrl("https://thingsboard.io");
            whiteLabelingParams.setEnableHelpLinks(true);
            return whiteLabelingParams;
        }
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.READ);
        WhiteLabelingParams whiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            whiteLabelingParams = whiteLabelingService.getSystemWhiteLabelingParams();
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            if (StringUtils.isEmpty(strCustomerId)) {
                whiteLabelingParams = whiteLabelingService.getTenantWhiteLabelingParams(getTenantId());
            } else {
                CustomerId customerId = new CustomerId(toUUID(strCustomerId));
                checkCustomerId(customerId, Operation.READ);
                whiteLabelingParams = whiteLabelingService.getCustomerWhiteLabelingParams(getTenantId(), customerId);
            }
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            whiteLabelingParams = whiteLabelingService.getCustomerWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId());
        }
        return whiteLabelingParams;
    }

    @ApiOperation(value = "Get Login White Labeling configuration (getCurrentWhiteLabelParams)",
            notes = "Fetch the Login  White Labeling configuration that corresponds to the authority of the user. " +
                    "The API call is designed to load the Login White Labeling configuration for edition. " +
                    "So, the result is NOT merged with the parent level White Labeling configuration. " +
                    "Let's assume there is a custom White Labeling  configured on a system level. " +
                    "And there is no custom White Labeling  items configured on a tenant level. " +
                    "In such a case, the API call will return default object for the tenant administrator. " +
                    WL_READ_CHECK
    )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/whiteLabel/currentLoginWhiteLabelParams", produces = "application/json")
    public LoginWhiteLabelingParams getCurrentLoginWhiteLabelParams(@Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
                                                                    @RequestParam(value = "customerId", required = false) String strCustomerId) throws Exception {
        boolean wlEnabled = subscriptionService.whiteLabelingEnabled(getCurrentUser().getTenantId());
        if (!wlEnabled) {
            return new LoginWhiteLabelingParams();
        }
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.READ);
        LoginWhiteLabelingParams loginWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            loginWhiteLabelingParams = whiteLabelingService.getSystemLoginWhiteLabelingParams();
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            if (StringUtils.isEmpty(strCustomerId)) {
                loginWhiteLabelingParams = whiteLabelingService.getTenantLoginWhiteLabelingParams(getTenantId());
            } else {
                CustomerId customerId = new CustomerId(toUUID(strCustomerId));
                checkCustomerId(customerId, Operation.READ);
                loginWhiteLabelingParams = whiteLabelingService.getCustomerLoginWhiteLabelingParams(getTenantId(), customerId);
            }
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            loginWhiteLabelingParams = whiteLabelingService.getCustomerLoginWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId());
        }
        return loginWhiteLabelingParams;
    }

    @ApiOperation(value = "Create Or Update White Labeling configuration (saveWhiteLabelParams)",
            notes = "Creates or Updates the White Labeling configuration." +
                    WL_WRITE_CHECK,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/whiteLabel/whiteLabelParams")
    @ResponseStatus(value = HttpStatus.OK)
    public WhiteLabelingParams saveWhiteLabelParams(
            @Parameter(description = "A JSON value representing the white labeling configuration")
            @RequestBody WhiteLabelingParams whiteLabelingParams,
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @RequestParam(value = "customerId", required = false) String strCustomerId) throws ThingsboardException {
        subscriptionService.whiteLabelingAllowed(getCurrentUser().getTenantId());
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.WRITE);
        WhiteLabelingParams savedWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            savedWhiteLabelingParams = whiteLabelingService.saveSystemWhiteLabelingParams(whiteLabelingParams);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            if (StringUtils.isEmpty(strCustomerId)) {
                savedWhiteLabelingParams = whiteLabelingService.saveTenantWhiteLabelingParams(getTenantId(), whiteLabelingParams);
            } else {
                CustomerId customerId = new CustomerId(toUUID(strCustomerId));
                checkCustomerId(customerId, Operation.READ);
                savedWhiteLabelingParams = whiteLabelingService.saveCustomerWhiteLabelingParams(getTenantId(), customerId, whiteLabelingParams);
            }
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            savedWhiteLabelingParams = whiteLabelingService.saveCustomerWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId(), whiteLabelingParams);
        }
        return savedWhiteLabelingParams;
    }

    @ApiOperation(value = "Create Or Update Login White Labeling configuration (saveWhiteLabelParams)",
            notes = "Creates or Updates the White Labeling configuration." +
                    WL_WRITE_CHECK,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/whiteLabel/loginWhiteLabelParams")
    @ResponseStatus(value = HttpStatus.OK)
    public LoginWhiteLabelingParams saveLoginWhiteLabelParams(
            @Parameter(description = "A JSON value representing the login white labeling configuration")
            @RequestBody LoginWhiteLabelingParams loginWhiteLabelingParams,
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
            @RequestParam(name = "customerId", required = false) String strCustomerId) throws Exception {
        subscriptionService.whiteLabelingAllowed(getCurrentUser().getTenantId());
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.WRITE);
        if (loginWhiteLabelingParams.getDomainId() != null) {
            checkEntityId(loginWhiteLabelingParams.getDomainId(), domainService::findDomainById, READ);
        }
        LoginWhiteLabelingParams savedLoginWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            savedLoginWhiteLabelingParams = whiteLabelingService.saveSystemLoginWhiteLabelingParams(loginWhiteLabelingParams);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            if (StringUtils.isEmpty(strCustomerId)) {
                savedLoginWhiteLabelingParams = whiteLabelingService.saveTenantLoginWhiteLabelingParams(getTenantId(), loginWhiteLabelingParams);
            } else {
                CustomerId customerId = new CustomerId(toUUID(strCustomerId));
                checkCustomerId(customerId, Operation.READ);
                savedLoginWhiteLabelingParams = whiteLabelingService.saveCustomerLoginWhiteLabelingParams(getTenantId(), customerId, loginWhiteLabelingParams);
            }
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            savedLoginWhiteLabelingParams = whiteLabelingService.saveCustomerLoginWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId(), loginWhiteLabelingParams);
        }
        return savedLoginWhiteLabelingParams;
    }

    @ApiOperation(value = "Preview Login White Labeling configuration (saveWhiteLabelParams)",
            notes = "Merge the White Labeling configuration with the parent configuration and return the result." +
                    WL_WRITE_CHECK,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/whiteLabel/previewWhiteLabelParams")
    @ResponseStatus(value = HttpStatus.OK)
    public WhiteLabelingParams previewWhiteLabelParams(
            @Parameter(description = "A JSON value representing the white labeling configuration")
            @RequestBody WhiteLabelingParams whiteLabelingParams) throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.WRITE);
        WhiteLabelingParams mergedWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            mergedWhiteLabelingParams = whiteLabelingService.mergeSystemWhiteLabelingParams(whiteLabelingParams);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            mergedWhiteLabelingParams = whiteLabelingService.mergeTenantWhiteLabelingParams(whiteLabelingParams);
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            mergedWhiteLabelingParams = whiteLabelingService.mergeCustomerWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId(), whiteLabelingParams);
        }
        return mergedWhiteLabelingParams;
    }

    @ApiOperation(value = "Check White Labeling Allowed",
            notes = "Check if the White Labeling is enabled for the current user owner (tenant or customer)" +
                    WL_WRITE_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/whiteLabel/isWhiteLabelingAllowed")
    public Boolean isWhiteLabelingAllowed() throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        return whiteLabelingService.isWhiteLabelingAllowed(getTenantId(), getCurrentUser().getCustomerId());
    }

    @ApiOperation(value = "Check Customer White Labeling Allowed",
            notes = "Check if the White Labeling is enabled for the customers of the current tenant" +
                    WL_WRITE_CHECK + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/whiteLabel/isCustomerWhiteLabelingAllowed")
    public Boolean isCustomerWhiteLabelingAllowed() throws ThingsboardException {
        return whiteLabelingService.isCustomerWhiteLabelingAllowed(getTenantId());
    }

    @ApiOperation(value = "Save the Mail templates settings (saveMailTemplates)",
            notes = "Creates or Updates the Mail templates settings." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + WL_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/whiteLabel/mailTemplates")
    @ResponseStatus(value = HttpStatus.OK)
    public JsonNode saveMailTemplates(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the Administration Settings.")
            @RequestBody JsonNode mailTemplates) throws Exception {
        checkWhiteLabelingPermissions(Operation.WRITE);
        return whiteLabelingService.saveMailTemplates(getTenantId(), mailTemplates);
    }

    @ApiOperation(value = "Get the Mail templates settings (getMailTemplates)",
            notes = "Fetch Mail template settings. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + WL_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/whiteLabel/mailTemplates")
    @ResponseStatus(value = HttpStatus.OK)
    public JsonNode getMailTemplates(@Parameter(description = "Use system settings if settings are not defined on tenant level.")
                                     @RequestParam(required = false, defaultValue = "false") boolean systemByDefault) throws Exception {
        checkWhiteLabelingPermissions(Operation.READ);
        JsonNode mailTemplates = whiteLabelingService.getCurrentTenantMailTemplates(getTenantId(), systemByDefault);

        ((ObjectNode) mailTemplates).remove(MailTemplates.API_USAGE_STATE_ENABLED);
        ((ObjectNode) mailTemplates).remove(MailTemplates.API_USAGE_STATE_WARNING);
        ((ObjectNode) mailTemplates).remove(MailTemplates.API_USAGE_STATE_DISABLED);

        return mailTemplates;
    }

    @ApiOperation(value = "Delete Login White Labeling configuration (deleteCurrentLoginWhiteLabelParams)",
            notes = "Delete the Login White Labeling configuration that corresponds to the authority of the user. " +
                    WL_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/whiteLabel/currentLoginWhiteLabelParams")
    public void deleteCurrentLoginWhiteLabelParams(@Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
                                                   @RequestParam(value = "customerId", required = false) String strCustomerId) throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.WRITE);
        SecurityUser currentUser = getCurrentUser();
        CustomerId customerId;
        if (Authority.TENANT_ADMIN.equals(authority) && !StringUtils.isEmpty(strCustomerId)) {
            customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.WRITE);
        } else {
            customerId = currentUser.getCustomerId();
        }
        whiteLabelingService.deleteWhiteLabeling(currentUser.getTenantId(), customerId, WhiteLabelingType.LOGIN);
    }

    @ApiOperation(value = "Delete General White Labeling configuration (deleteCurrentWhiteLabelParams)",
            notes = "Delete the White Labeling configuration that corresponds to the authority of the user. " +
                    WL_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/whiteLabel/currentWhiteLabelParams")
    public void deleteCurrentWhiteLabelParams(@Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
                                              @RequestParam(value = "customerId", required = false) String strCustomerId) throws Exception {
        checkWhiteLabelingPermissions(Operation.WRITE);
        SecurityUser currentUser = getCurrentUser();
        CustomerId customerId;
        if (Authority.TENANT_ADMIN.equals(currentUser.getAuthority()) && !StringUtils.isEmpty(strCustomerId)) {
            customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.WRITE);
        } else {
            customerId = currentUser.getCustomerId();
        }
        whiteLabelingService.deleteWhiteLabeling(currentUser.getTenantId(), customerId, WhiteLabelingType.GENERAL);
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
