// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceCertificateRevocationId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocationRequest;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.device.DeviceCertificateRevocationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.device.TbDeviceCertificateRevocationService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

/**
 * Revoked device certificates of the tenant. Limited to tenant administrators: a revocation applies to every device of the
 * tenant, so revoking by PEM or removing a revocation is not something a customer user may do for other customers' devices.
 */
@RestController
@TbCoreComponent
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceCertificateRevocationController extends BaseController {

    private static final String REVOCATION_ID_PARAM_DESCRIPTION = "A string value representing the revocation id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String RBAC_WRITE_CREDENTIALS_CHECK = " Security check is performed to verify that the user has 'WRITE_CREDENTIALS' permission for devices.";

    private final DeviceCertificateRevocationService revocationService;
    private final TbDeviceCertificateRevocationService tbRevocationService;

    @ApiOperation(value = "Get revoked device certificates (getDeviceCertificateRevocations)",
            notes = "Returns a page of device certificates revoked in the tenant. The text search matches the certificate subject, " +
                    "issuer, serial number and the revocation reason. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/deviceCertificateRevocations")
    public PageData<DeviceCertificateRevocation> getDeviceCertificateRevocations(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "The case insensitive 'substring' filter based on the certificate subject, issuer, serial number or revocation reason.")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "subject", "issuer", "serialNumber", "notAfter", "reason"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.DEVICE, Operation.READ_CREDENTIALS);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(revocationService.findRevocationsByTenantId(user.getTenantId(), pageLink));
    }

    @ApiOperation(value = "Revoke certificate (revokeCertificate)",
            notes = "Revokes a single X.509 device certificate given by its PEM: it can no longer authenticate or be bound to a device " +
                    "by X.509 certificate chain provisioning, also when it is not the certificate a device currently uses. " +
                    "Active sessions of the device the certificate is bound to are closed. CA certificates can't be revoked." +
                    TENANT_AUTHORITY_PARAGRAPH + RBAC_WRITE_CREDENTIALS_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/deviceCertificateRevocation")
    public DeviceCertificateRevocation revokeCertificate(
            @Parameter(description = "Certificate PEM and optional reason of the revocation.", required = true)
            @RequestBody DeviceCertificateRevocationRequest request) throws ThingsboardException {
        checkNotNull(request);
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.DEVICE, Operation.WRITE_CREDENTIALS);
        return tbRevocationService.revokeCertificate(user.getTenantId(), request.getCertificate(), request.getReason(), user);
    }

    @ApiOperation(value = "Delete revocation of device certificate (deleteDeviceCertificateRevocation)",
            notes = "Removes the revocation: the certificate is accepted again." +
                    TENANT_AUTHORITY_PARAGRAPH + RBAC_WRITE_CREDENTIALS_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/deviceCertificateRevocation/{revocationId}")
    public void deleteDeviceCertificateRevocation(
            @Parameter(description = REVOCATION_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("revocationId") UUID revocationId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.DEVICE, Operation.WRITE_CREDENTIALS);
        DeviceCertificateRevocation revocation = revocationService.findRevocationById(user.getTenantId(), new DeviceCertificateRevocationId(revocationId));
        if (revocation == null || !user.getTenantId().equals(revocation.getTenantId())) {
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        tbRevocationService.deleteRevocation(revocation, user);
    }

}
