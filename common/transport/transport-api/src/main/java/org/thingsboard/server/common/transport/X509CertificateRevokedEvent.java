// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.transport;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.discovery.event.TbApplicationEvent;

import java.io.Serial;

/**
 * For CoAP and LwM2M to drop the DTLS state they keep outside device sessions.
 */
public final class X509CertificateRevokedEvent extends TbApplicationEvent {

    @Serial
    private static final long serialVersionUID = 4790364519203868431L;

    @Getter
    private final TenantId tenantId;
    @Getter
    private final String certificateHash;

    public X509CertificateRevokedEvent(TenantId tenantId, String certificateHash) {
        super(new Object());
        this.tenantId = tenantId;
        this.certificateHash = certificateHash;
    }

}
