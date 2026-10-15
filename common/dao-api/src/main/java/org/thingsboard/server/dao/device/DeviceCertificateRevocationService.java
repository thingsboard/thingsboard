// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.id.DeviceCertificateRevocationId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;

public interface DeviceCertificateRevocationService {

    /**
     * @param deviceId the device whose credentials hold the certificate, or null to look it up; a supplied id skips the CA check
     */
    DeviceCertificateRevocation revokeCertificate(TenantId tenantId, String certificatePem, DeviceId deviceId, String reason, UserId revokedBy);

    boolean isRevoked(TenantId tenantId, String certificateHash);

    DeviceCertificateRevocation findRevocationByCertificatePem(TenantId tenantId, String certificatePem);

    DeviceCertificateRevocation findRevocationById(TenantId tenantId, DeviceCertificateRevocationId revocationId);

    PageData<DeviceCertificateRevocation> findRevocationsByTenantId(TenantId tenantId, PageLink pageLink);

    void deleteRevocation(TenantId tenantId, DeviceCertificateRevocationId revocationId);

    void deleteRevocationsByTenantId(TenantId tenantId);

    int deleteRevocationsExpiredBefore(long ts);

}
