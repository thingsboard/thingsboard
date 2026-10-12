// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.transport;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.discovery.event.TbApplicationEvent;

import java.io.Serial;

/**
 * A tenant revoked a device certificate. Transports that keep authenticated DTLS state outside device sessions
 * (CoAP, LwM2M) must drop it, otherwise the device keeps working until that state times out.
 */
public final class X509CertificateRevokedEvent extends TbApplicationEvent {

    @Serial
    private static final long serialVersionUID = 4790364519203868431L;

    @Getter
    private final TenantId tenantId;
    /**
     * SHA3-256 of the Base64-encoded DER of the certificate, as computed by {@code EncryptionUtil.getSha3Hash}.
     */
    @Getter
    private final String certificateHash;

    public X509CertificateRevokedEvent(TenantId tenantId, String certificateHash) {
        super(new Object());
        this.tenantId = tenantId;
        this.certificateHash = certificateHash;
    }

}
