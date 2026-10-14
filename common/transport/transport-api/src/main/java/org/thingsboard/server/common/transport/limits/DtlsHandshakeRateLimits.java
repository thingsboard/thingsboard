// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.transport.limits;

import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;

/**
 * IP rate limits for repeatedly failed authentication applied to the DTLS handshakes of the CoAP and LwM2M transports.
 * Relies on Scandium behaviour that is not part of its API (Scandium 3.12, re-check on upgrade).
 */
public final class DtlsHandshakeRateLimits {

    private DtlsHandshakeRateLimits() {
    }

    /**
     * @throws HandshakeException with an ACCESS_DENIED alert if the address of the handshake is blocked
     */
    public static void checkAddress(TransportRateLimitService rateLimitService, Handshaker handshaker) throws HandshakeException {
        if (!rateLimitService.checkAddress(handshaker.getPeerAddress())) {
            throw new HandshakeException("IP address is blocked due to repeatedly failed authentication",
                    new AlertMessage(AlertLevel.FATAL, AlertDescription.ACCESS_DENIED));
        }
    }

    /**
     * The client could not prove that it holds the private key of its certificate (CertificateVerify, or a Finished
     * message that does not match the handshake). Alerts that the client sent, e.g. because it does not trust the server
     * certificate, reach the session listener as well and are not counted: Scandium reports them as "Received 'fatal alert/...'"
     * (DTLSConnector.processAlertRecord).
     * <p>
     * Abbreviated handshakes are not counted, since a ClientHello that resumes a known session may skip the cookie exchange
     * that verifies the address. A resumption that falls back to a full handshake is counted: the address of an unknown
     * session id was verified by the cookie exchange.
     */
    public static boolean isFailedProofOfPossession(Handshaker handshaker, Throwable error) {
        return handshaker.isFullHandshake()
                && error instanceof HandshakeException handshakeException
                && handshakeException.getAlert() != null
                && handshakeException.getAlert().getDescription() == AlertDescription.DECRYPT_ERROR
                && (handshakeException.getMessage() == null || !handshakeException.getMessage().startsWith("Received "));
    }

}
