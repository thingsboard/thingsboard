// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.coapserver;

import lombok.RequiredArgsConstructor;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.DTLSContext;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.ResumingServerHandshaker;
import org.eclipse.californium.scandium.dtls.SessionListener;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;

/**
 * Stores the DTLS session of a device once the handshake proved that the client holds the private key of its certificate,
 * and applies the IP rate limits for repeatedly failed authentication to DTLS handshakes.
 */
@RequiredArgsConstructor
public class TbCoapDtlsHandshakeListener implements SessionListener {

    private final TbCoapDtlsCertificateVerifier certificateVerifier;
    private final TransportRateLimitService rateLimitService;

    @Override
    public void handshakeStarted(Handshaker handshaker) throws HandshakeException {
        if (rateLimitService != null && !rateLimitService.checkAddress(handshaker.getPeerAddress())) {
            throw new HandshakeException("IP address is blocked due to repeatedly failed authentication",
                    new AlertMessage(AlertLevel.FATAL, AlertDescription.ACCESS_DENIED));
        }
    }

    @Override
    public void contextEstablished(Handshaker handshaker, DTLSContext establishedContext) {
        // Called after the client's Finished message is verified, so after its CertificateVerify
        if (certificateVerifier.commitSession(handshaker.getConnection().getConnectionId()) && rateLimitService != null) {
            rateLimitService.onAuthSuccess(handshaker.getPeerAddress());
        }
    }

    @Override
    public void handshakeCompleted(Handshaker handshaker) {
    }

    @Override
    public void handshakeFailed(Handshaker handshaker, Throwable error) {
        certificateVerifier.dropSession(handshaker.getConnection().getConnectionId());
        if (rateLimitService != null && isFailedProofOfPossession(handshaker, error)) {
            rateLimitService.onAuthFailure(handshaker.getPeerAddress());
        }
    }

    @Override
    public void handshakeFlightRetransmitted(Handshaker handshaker, int flight) {
    }

    /**
     * The client could not prove that it holds the private key of its certificate (CertificateVerify, or a Finished
     * message that does not match the handshake). Alerts that the client sent, e.g. because it does not trust the server
     * certificate, reach this listener as well and are not counted: Scandium reports them as "Received 'fatal alert/...'"
     * (DTLSConnector.processAlertRecord in Scandium 3.12, re-check on upgrade).
     * Resumption handshakes are not counted, since their ClientHello may skip the cookie exchange that verifies the address.
     */
    static boolean isFailedProofOfPossession(Handshaker handshaker, Throwable error) {
        return !(handshaker instanceof ResumingServerHandshaker)
                && error instanceof HandshakeException handshakeException
                && handshakeException.getAlert() != null
                && handshakeException.getAlert().getDescription() == AlertDescription.DECRYPT_ERROR
                && (handshakeException.getMessage() == null || !handshakeException.getMessage().startsWith("Received "));
    }

}
