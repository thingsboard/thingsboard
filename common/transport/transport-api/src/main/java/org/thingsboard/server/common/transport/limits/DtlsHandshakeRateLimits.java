// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.transport.limits;

import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;

public final class DtlsHandshakeRateLimits {

    private DtlsHandshakeRateLimits() {
    }

    public static void checkAddress(TransportRateLimitService rateLimitService, Handshaker handshaker) throws HandshakeException {
        if (!rateLimitService.checkAddress(handshaker.getPeerAddress())) {
            throw new HandshakeException("IP address is blocked due to repeatedly failed authentication",
                    new AlertMessage(AlertLevel.FATAL, AlertDescription.ACCESS_DENIED));
        }
    }

    /**
     * Relies on Scandium 3.12 internals (re-check on upgrade): alerts sent by the client come as "Received ..."
     * (DTLSConnector.processAlertRecord), and abbreviated handshakes may skip the cookie exchange that verifies the address.
     */
    public static boolean isFailedProofOfPossession(Handshaker handshaker, Throwable error) {
        return handshaker.isFullHandshake()
                && error instanceof HandshakeException handshakeException
                && handshakeException.getAlert() != null
                && handshakeException.getAlert().getDescription() == AlertDescription.DECRYPT_ERROR
                && (handshakeException.getMessage() == null || !handshakeException.getMessage().startsWith("Received "));
    }

}
