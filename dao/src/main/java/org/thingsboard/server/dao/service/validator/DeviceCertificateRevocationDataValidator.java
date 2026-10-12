// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
@RequiredArgsConstructor
public class DeviceCertificateRevocationDataValidator extends DataValidator<DeviceCertificateRevocation> {

    private final TenantService tenantService;

    @Override
    protected void validateDataImpl(TenantId tenantId, DeviceCertificateRevocation revocation) {
        if (revocation.getTenantId() == null || revocation.getTenantId().isSysTenantId()) {
            throw new DataValidationException("Revoked certificate should be assigned to tenant!");
        }
        if (!tenantService.tenantExists(revocation.getTenantId())) {
            throw new DataValidationException("Revoked certificate is referencing a non-existent tenant!");
        }
        if (StringUtils.isEmpty(revocation.getCertificateHash())) {
            throw new DataValidationException("Revoked certificate hash should be specified!");
        }
    }

    @Override
    protected DeviceCertificateRevocation validateUpdate(TenantId tenantId, DeviceCertificateRevocation revocation) {
        throw new DataValidationException("Revoked certificate can't be updated!");
    }

}
