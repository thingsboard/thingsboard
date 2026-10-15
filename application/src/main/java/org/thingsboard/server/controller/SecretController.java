// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.entity.BaseEntityService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.secret.TbSecretService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SECRET_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class SecretController extends BaseController {

    private final TbSecretService tbSecretService;

    @ApiOperation(value = "Save or Update Secret (saveSecret)",
            notes = "Create or update the Secret. When creating secret, platform generates Secret Id as " + UUID_WIKI_LINK +
                    "The newly created Secret Id will be present in the response. " +
                    "Specify existing Secret Id to update the secret. Secret name is not updatable, only value could be changed. " +
                    "Referencing non-existing Secret Id will cause 'Not Found' error." + NEW_LINE +
                    "Tenant Id and Customer Id are derived from the current user and cannot be overridden by the request body. " +
                    "Secret name is unique within the owner scope: per tenant for sysadmin/tenant-admin secrets, " +
                    "and per customer for customer-user secrets." + SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/secret")
    public SecretInfo saveSecret(
            @Parameter(description = "A JSON value representing the Secret.", required = true)
            @RequestBody @Valid Secret secret) throws Exception {
        SecurityUser securityUser = getCurrentUser();
        secret.setTenantId(securityUser.getTenantId());
        secret.setCustomerId(getCustomerId(securityUser));
        checkEntity(secret.getId(), secret, Resource.SECRET);
        return tbSecretService.save(secret, securityUser);
    }

    @ApiOperation(value = "Update Secret Description",
            notes = "Updates the description of the existing Secret by secretId. " +
                    "Only the description can be updated. " +
                    "Referencing a non-existing Secret Id will cause a 'Not Found' error." + SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping("/secret/{id}/description")
    public SecretInfo updateSecretDescription(
            @Parameter(description = "Unique identifier of the Secret to update", required = true)
            @PathVariable UUID id,
            @Parameter(description = "New description for the Secret", example = "Description")
            @RequestBody Optional<String> description) throws Exception {
        SecretId secretId = new SecretId(id);
        Secret secret = new Secret(checkSecretId(secretId, Operation.WRITE));
        secret.setDescription(description.orElse(null));
        return tbSecretService.save(secret, getCurrentUser());
    }

    @ApiOperation(value = "Update Secret value",
            notes = "Updates the value of the existing Secret by secretId. " +
                    "Referencing a non-existing Secret Id will cause a 'Not Found' error." + SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping("/secret/{id}/value")
    public SecretInfo updateSecretValue(
            @Parameter(description = "Unique identifier of the Secret to update", required = true)
            @PathVariable UUID id,
            @Parameter(description = "New value for the Secret", example = "Description", required = true)
            @RequestBody String value) throws Exception {
        SecretId secretId = new SecretId(id);
        Secret secret = new Secret(checkSecretId(secretId, Operation.WRITE));
        secret.setValue(value);
        return tbSecretService.save(secret, getCurrentUser());
    }

    @ApiOperation(value = "Delete secret by ID (deleteSecret)",
            notes = "Deletes the secret. Referencing non-existing Secret Id will cause an error." + SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/secret/{id}")
    public ResponseEntity<TbSecretDeleteResult> deleteSecret(@PathVariable UUID id) throws ThingsboardException {
        SecretId secretId = new SecretId(id);
        SecretInfo secretInfo = checkSecretId(secretId, Operation.DELETE);
        TbSecretDeleteResult result = tbSecretService.delete(secretInfo, getCurrentUser());
        return (result.isSuccess() ? ResponseEntity.ok() : ResponseEntity.badRequest()).body(result);
    }

    @ApiOperation(value = "Get Secret infos (getSecretInfos)",
            notes = "Returns a page of secret infos visible to the current user. " +
                    "Sysadmin and tenant-admin users see secrets owned by their tenant; " +
                    "customer users see only secrets owned by their customer. " + PAGE_DATA_PARAMETERS + SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/secrets", params = {"pageSize", "page"})
    public PageData<SecretInfo> getSecretInfos(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                               @RequestParam int pageSize,
                                               @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                               @RequestParam int page,
                                               @Parameter(description = SECRET_TEXT_SEARCH_DESCRIPTION)
                                               @RequestParam(required = false) String textSearch,
                                               @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"name"}))
                                               @RequestParam(required = false) String sortProperty,
                                               @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                               @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        accessControlService.checkPermission(securityUser, Resource.SECRET, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(secretService.findSecretInfosByTenantIdAndCustomerId(securityUser.getTenantId(), getCustomerId(securityUser), pageLink));
    }

    @ApiOperation(value = "Get Secret names (getSecretNames)",
            notes = "Returns the list of secret names visible to the current user. " +
                    "Sysadmin and tenant-admin users see secrets owned by their tenant; " +
                    "customer users see only secrets owned by their customer." + SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/secret/names")
    public List<String> getSecretNames() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        accessControlService.checkPermission(securityUser, Resource.SECRET, Operation.READ);
        return checkNotNull(secretService.findSecretNamesByTenantIdAndCustomerId(securityUser.getTenantId(), getCustomerId(securityUser)));
    }

    @ApiOperation(value = "Get Secret info by Id (getSecretInfoById)",
            notes = "Returns the secret info by id. " +
                    "Access is restricted to the owner scope: tenant-admin users cannot access customer-owned secrets, " +
                    "and customer users cannot access tenant-owned secrets." + SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/secret/{id}/info")
    public SecretInfo getSecretInfoById(@PathVariable UUID id) throws ThingsboardException {
        SecretId secretId = new SecretId(id);
        return checkSecretId(secretId, Operation.READ);
    }

    @ApiOperation(value = "Get Secret info by name (getSecretInfoByName)",
            notes = "Returns the secret info by name within the current user's owner scope. " +
                    "Sysadmin and tenant-admin users look up names among tenant-owned secrets; " +
                    "customer users look up names among their own customer-owned secrets." + SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/secret")
    public SecretInfo getSecretInfoByName(@RequestParam String name) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        checkParameter("name", name);
        accessControlService.checkPermission(securityUser, Resource.SECRET, Operation.READ);
        return checkNotNull(secretService.findSecretInfoByName(securityUser.getTenantId(), getCustomerId(securityUser), name));
    }

    private static CustomerId getCustomerId(SecurityUser user) {
        return user.isCustomerUser() ? user.getCustomerId() : BaseEntityService.NULL_CUSTOMER_ID;
    }

}
