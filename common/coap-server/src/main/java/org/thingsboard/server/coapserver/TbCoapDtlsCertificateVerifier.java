// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.coapserver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class TbCoapDtlsCertificateVerifier implements NewAdvancedCertificateVerifier {

    private final TbCoapDtlsSessionInMemoryStorage tbCoapDtlsSessionInMemoryStorage;
    // Sessions of handshakes in progress, by connection: stored once the client proved that it holds the private key
    private final ConcurrentMap<ConnectionId, PendingSession> pendingSessions = new ConcurrentHashMap<>();

    private TransportService transportService;
    private TbServiceInfoProvider serviceInfoProvider;
    private TransportRateLimitService rateLimitService;
    private boolean skipValidityCheckForClientCert;

    public TbCoapDtlsCertificateVerifier(TransportService transportService, TbServiceInfoProvider serviceInfoProvider, TransportRateLimitService rateLimitService,
                                         long dtlsSessionInactivityTimeout, long dtlsSessionReportTimeout, boolean skipValidityCheckForClientCert) {
        this.transportService = transportService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.rateLimitService = rateLimitService;
        this.skipValidityCheckForClientCert = skipValidityCheckForClientCert;
        this.tbCoapDtlsSessionInMemoryStorage = new TbCoapDtlsSessionInMemoryStorage(dtlsSessionInactivityTimeout, dtlsSessionReportTimeout);
    }

    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return Collections.singletonList(CertificateType.X_509);
    }

    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName, InetSocketAddress remotePeer, boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath, CertificateMessage message) {
        try {
            CertPath certpath = message.getCertificateChain();
            X509Certificate[] chain = certpath.getCertificates().toArray(new X509Certificate[0]);
            boolean found = false;
            boolean coreUnavailable = false;
            for (X509Certificate cert : chain) {
                try {
                    if (!skipValidityCheckForClientCert) {
                        cert.checkValidity();
                    }

                    String strCert = SslUtil.getCertificateString(cert);
                    String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                    final ValidateDeviceCredentialsResponse[] deviceCredentialsResponse = new ValidateDeviceCredentialsResponse[1];
                    final boolean[] failed = new boolean[1];
                    CountDownLatch latch = new CountDownLatch(1);
                    transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceX509CertRequestMsg.newBuilder().setHash(sha3Hash).build(),
                            new TransportServiceCallback<>() {
                                @Override
                                public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                                    if (!StringUtils.isEmpty(msg.getCredentials())) {
                                        deviceCredentialsResponse[0] = msg;
                                    }
                                    latch.countDown();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    log.error(e.getMessage(), e);
                                    failed[0] = true;
                                    latch.countDown();
                                }
                            });
                    if (!latch.await(10, TimeUnit.SECONDS) || failed[0]) {
                        coreUnavailable = true;
                        continue;
                    }
                    ValidateDeviceCredentialsResponse msg = deviceCredentialsResponse[0];
                    if (msg != null && strCert.equals(msg.getCredentials())) {
                        found = true;
                        DeviceProfile deviceProfile = msg.getDeviceProfile();
                        if (msg.hasDeviceInfo() && deviceProfile != null) {
                            TbCoapDtlsSessionKey tbCoapDtlsSessionKey = new TbCoapDtlsSessionKey(remotePeer, msg.getCredentials());
                            pendingSessions.put(cid, new PendingSession(tbCoapDtlsSessionKey, new TbCoapDtlsSessionInfo(msg, deviceProfile, sha3Hash), System.currentTimeMillis()));
                        }
                        break;
                    }
                } catch (InterruptedException |
                        CertificateEncodingException |
                        CertificateExpiredException |
                        CertificateNotYetValidException e) {
                    log.error(e.getMessage(), e);
                    if (!(e instanceof InterruptedException)) {
                        onAuthFailure(remotePeer);
                    }
                    AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.BAD_CERTIFICATE);
                    throw new HandshakeException("Certificate chain could not be validated", alert);
                }
            }
            if (!found && !coreUnavailable) {
                // The handshake still succeeds: the client may authenticate its requests with an access token
                onAuthFailure(remotePeer);
            }
            return new CertificateVerificationResult(cid, certpath, null);
        } catch (HandshakeException e) {
            log.trace("Certificate validation failed!", e);
            return new CertificateVerificationResult(cid, e, null);
        }
    }

    /**
     * Stores the session found for the certificate of the handshake, once the client proved that it holds its private key.
     *
     * @return whether the client authenticated with the certificate of a device
     */
    public boolean commitSession(ConnectionId cid) {
        PendingSession pendingSession = pendingSessions.remove(cid);
        if (pendingSession == null) {
            return false;
        }
        tbCoapDtlsSessionInMemoryStorage.put(pendingSession.key(), pendingSession.info());
        return true;
    }

    public void dropSession(ConnectionId cid) {
        pendingSessions.remove(cid);
    }

    private void onAuthFailure(InetSocketAddress remotePeer) {
        if (rateLimitService != null) {
            rateLimitService.onAuthFailure(remotePeer);
        }
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return CertPathUtil.toSubjects(null);
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
    }

    public ConcurrentMap<TbCoapDtlsSessionKey, TbCoapDtlsSessionInfo> getTbCoapDtlsSessionsMap() {
        return tbCoapDtlsSessionInMemoryStorage.getDtlsSessionsMap();
    }

    public void evictTimeoutSessions() {
        tbCoapDtlsSessionInMemoryStorage.evictTimeoutSessions();
        // Handshakes end by completing or failing, this only guards against a handshake that is never reported
        long expirationTime = System.currentTimeMillis() - getDtlsSessionReportTimeout();
        pendingSessions.values().removeIf(pendingSession -> pendingSession.createdTime() < expirationTime);
    }

    public void evictSessions(TenantId tenantId, String certificateHash) {
        tbCoapDtlsSessionInMemoryStorage.evictSessions(tenantId, certificateHash);
    }

    public long getDtlsSessionReportTimeout() {
        return tbCoapDtlsSessionInMemoryStorage.getDtlsSessionReportTimeout();
    }

    private record PendingSession(TbCoapDtlsSessionKey key, TbCoapDtlsSessionInfo info, long createdTime) {}

}
