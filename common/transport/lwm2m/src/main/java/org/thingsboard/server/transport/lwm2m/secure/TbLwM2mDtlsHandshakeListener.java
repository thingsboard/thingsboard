// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.RequiredArgsConstructor;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.DTLSContext;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.ResumingServerHandshaker;
import org.eclipse.californium.scandium.dtls.SessionListener;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies the IP rate limits for repeatedly failed authentication to the DTLS handshakes of LwM2M clients, and registers
 * a client's certificate once the handshake proved that the client holds its private key.
 * <p>
 * Raw public keys are trusted during the handshake and checked when the client registers, see {@link TbLwM2MAuthorizer}.
 */
@RequiredArgsConstructor
public class TbLwM2mDtlsHandshakeListener implements SessionListener {

    private final TransportRateLimitService rateLimitService;
    // Null for the bootstrap server
    private final LwM2mClientCertificateRegistry certificateRegistry;
    // The pre-shared key store decides on the identity of a handshake, but does not know the client's address
    private final Map<ConnectionId, Handshaker> handshakes = new ConcurrentHashMap<>();

    @Override
    public void handshakeStarted(Handshaker handshaker) throws HandshakeException {
        if (!rateLimitService.checkAddress(handshaker.getPeerAddress())) {
            throw new HandshakeException("IP address is blocked due to repeatedly failed authentication",
                    new AlertMessage(AlertLevel.FATAL, AlertDescription.ACCESS_DENIED));
        }
        handshakes.put(handshaker.getConnection().getConnectionId(), handshaker);
    }

    @Override
    public void contextEstablished(Handshaker handshaker, DTLSContext establishedContext) {
        // Called after the client's Finished message is verified, so after its CertificateVerify or with the right pre-shared key
        ConnectionId cid = handshaker.getConnection().getConnectionId();
        handshakes.remove(cid, handshaker);
        if (certificateRegistry != null) {
            certificateRegistry.commitPending(cid);
        }
        Principal peerIdentity = establishedContext.getSession().getPeerIdentity();
        if (peerIdentity instanceof X509CertPath || peerIdentity instanceof PreSharedKeyIdentity) {
            rateLimitService.onAuthSuccess(handshaker.getPeerAddress());
        }
    }

    @Override
    public void handshakeCompleted(Handshaker handshaker) {
    }

    @Override
    public void handshakeFailed(Handshaker handshaker, Throwable error) {
        ConnectionId cid = handshaker.getConnection().getConnectionId();
        handshakes.remove(cid, handshaker);
        if (certificateRegistry != null) {
            certificateRegistry.dropPending(cid);
        }
        if (isFailedProofOfPossession(handshaker, error)) {
            rateLimitService.onAuthFailure(handshaker.getPeerAddress());
        }
    }

    @Override
    public void handshakeFlightRetransmitted(Handshaker handshaker, int flight) {
    }

    public void onUnknownPskIdentity(ConnectionId cid) {
        // Counted once per handshake: the store is asked again for every retransmission of the client's key exchange
        Handshaker handshaker = handshakes.remove(cid);
        if (handshaker != null) {
            rateLimitService.onAuthFailure(handshaker.getPeerAddress());
        }
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
