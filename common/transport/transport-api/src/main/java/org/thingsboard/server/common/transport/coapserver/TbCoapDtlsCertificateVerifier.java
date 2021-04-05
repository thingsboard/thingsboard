/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.transport.coapserver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import javax.security.auth.x500.X500Principal;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class TbCoapDtlsCertificateVerifier implements NewAdvancedCertificateVerifier {

    private final TbCoapDtlsSessionInMemoryStorage tbCoapDtlsSessionInMemoryStorage;

    private TransportService transportService;
    private TbServiceInfoProvider serviceInfoProvider;
    private boolean skipValidityCheckForClientCert;

    public TbCoapDtlsCertificateVerifier(TransportService transportService, TbServiceInfoProvider serviceInfoProvider, long dtlsSessionInactivityTimeout, long dtlsSessionReportTimeout, boolean skipValidityCheckForClientCert) {
        this.transportService = transportService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.skipValidityCheckForClientCert = skipValidityCheckForClientCert;
        this.tbCoapDtlsSessionInMemoryStorage = new TbCoapDtlsSessionInMemoryStorage(dtlsSessionInactivityTimeout, dtlsSessionReportTimeout);
    }

    @Override
    public List<CertificateType> getSupportedCertificateType() {
        return Collections.singletonList(CertificateType.X_509);
    }

    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName, Boolean clientUsage, boolean truncateCertificatePath, CertificateMessage message, DTLSSession session) {
        try {
            String credentialsBody = null;
            CertPath certpath = message.getCertificateChain();
            X509Certificate[] chain = certpath.getCertificates().toArray(new X509Certificate[0]);
            for (X509Certificate cert : chain) {
                try {
                    if (!skipValidityCheckForClientCert) {
                        cert.checkValidity();
                    }
                    String strCert = SslUtil.getCertificateString(cert);
                    String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                    final ValidateDeviceCredentialsResponse[] deviceCredentialsResponse = new ValidateDeviceCredentialsResponse[1];
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
                                    latch.countDown();
                                }
                            });
                    latch.await(10, TimeUnit.SECONDS);
                    ValidateDeviceCredentialsResponse msg = deviceCredentialsResponse[0];
                    if (msg != null && strCert.equals(msg.getCredentials())) {
                        credentialsBody = msg.getCredentials();
                        DeviceProfile deviceProfile = msg.getDeviceProfile();
                        if (msg.hasDeviceInfo() && deviceProfile != null) {
                            TransportProtos.SessionInfoProto sessionInfoProto = SessionInfoCreator.create(msg, serviceInfoProvider.getServiceId(), UUID.randomUUID());
                            tbCoapDtlsSessionInMemoryStorage.put(session.getSessionIdentifier().toString(), new TbCoapDtlsSessionInfo(sessionInfoProto, deviceProfile));
                        }
                        break;
                    }
                } catch (InterruptedException |
                        CertificateEncodingException |
                        CertificateExpiredException |
                        CertificateNotYetValidException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (credentialsBody == null) {
                AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.BAD_CERTIFICATE,
                        session.getPeer());
                throw new HandshakeException("Certificate chain could not be validated", alert);
            } else {
                return new CertificateVerificationResult(cid, certpath, null);
            }
        } catch (HandshakeException e) {
            log.trace("Certificate validation failed!", e);
            return new CertificateVerificationResult(cid, e, null);
        }
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return CertPathUtil.toSubjects(null);
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
        // empty implementation
    }

    public ConcurrentMap<String, TbCoapDtlsSessionInfo> getTbCoapDtlsSessionIdsMap() {
        return tbCoapDtlsSessionInMemoryStorage.getDtlsSessionIdMap();
    }

    public void evictTimeoutSessions() {
        tbCoapDtlsSessionInMemoryStorage.evictTimeoutSessions();
    }

    public long getDtlsSessionReportTimeout() {
        return tbCoapDtlsSessionInMemoryStorage.getDtlsSessionReportTimeout();
    }
}