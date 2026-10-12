// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.device;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCertificateInfo;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.common.data.security.DeviceCredentials;

public interface TbDeviceCertificateRevocationService {

    /**
     * Revokes the X.509 certificate stored in the device credentials and disconnects the device.
     */
    DeviceCertificateRevocation revokeDeviceCertificate(Device device, String reason, User user) throws ThingsboardException;

    /**
     * Revokes a certificate by its PEM, e.g. a previous certificate of a device that chain provisioning would bind again,
     * or the certificate of a device that is not provisioned yet. Disconnects the device the certificate is bound to.
     */
    DeviceCertificateRevocation revokeCertificate(TenantId tenantId, String certificatePem, String reason, User user) throws ThingsboardException;

    /**
     * The certificate stored in the device credentials with its revocation, or null if the credentials hold no valid certificate.
     */
    DeviceCertificateInfo getDeviceCertificateInfo(Device device);

    void deleteRevocation(DeviceCertificateRevocation revocation, User user) throws ThingsboardException;

    /**
     * Rejects credentials holding a revoked certificate: they could never authenticate.
     */
    void validateNotRevoked(TenantId tenantId, DeviceCredentials credentials);

}
