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
     * Revokes a single X.509 device certificate for the tenant.
     *
     * @param certificatePem PEM of exactly one certificate; CA certificates are rejected
     * @param deviceId       device the certificate is bound to at the moment of revocation, if any
     */
    DeviceCertificateRevocation revokeCertificate(TenantId tenantId, String certificatePem, DeviceId deviceId, String reason, UserId revokedBy);

    /**
     * Hot path of device authentication: the hash is the SHA3-256 of the DER-encoded certificate,
     * the same value transports compute and X.509 device credentials use as credentials id.
     */
    boolean isRevoked(TenantId tenantId, String certificateHash);

    DeviceCertificateRevocation findRevocationByCertificatePem(TenantId tenantId, String certificatePem);

    DeviceCertificateRevocation findRevocationById(TenantId tenantId, DeviceCertificateRevocationId revocationId);

    PageData<DeviceCertificateRevocation> findRevocationsByTenantId(TenantId tenantId, PageLink pageLink);

    void deleteRevocation(TenantId tenantId, DeviceCertificateRevocationId revocationId);

    void deleteRevocationsByTenantId(TenantId tenantId);

    int deleteRevocationsExpiredBefore(long ts);

}
