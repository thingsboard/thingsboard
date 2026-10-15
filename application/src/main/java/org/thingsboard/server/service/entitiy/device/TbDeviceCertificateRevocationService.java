// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.device;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCertificateInfo;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.common.data.security.DeviceCredentials;

public interface TbDeviceCertificateRevocationService {

    DeviceCertificateRevocation revokeDeviceCertificate(Device device, String reason, User user) throws ThingsboardException;

    DeviceCertificateRevocation revokeCertificate(TenantId tenantId, String certificatePem, String reason, User user) throws ThingsboardException;

    DeviceCertificateInfo getDeviceCertificateInfo(Device device);

    void deleteRevocation(DeviceCertificateRevocation revocation, User user) throws ThingsboardException;

    /**
     * Rejects a revoked certificate, except the device's own unchanged one, so that the device can still be saved.
     */
    void validateNotRevoked(TenantId tenantId, DeviceId deviceId, DeviceCredentials credentials);

}
