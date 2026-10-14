// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.coapserver;

import lombok.RequiredArgsConstructor;
import org.eclipse.californium.scandium.dtls.DTLSContext;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.SessionListener;
import org.thingsboard.server.common.transport.limits.DtlsHandshakeRateLimits;
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
        if (rateLimitService != null) {
            DtlsHandshakeRateLimits.checkAddress(rateLimitService, handshaker);
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
        if (rateLimitService != null && DtlsHandshakeRateLimits.isFailedProofOfPossession(handshaker, error)) {
            rateLimitService.onAuthFailure(handshaker.getPeerAddress());
        }
    }

    @Override
    public void handshakeFlightRetransmitted(Handshaker handshaker, int flight) {
    }

}
