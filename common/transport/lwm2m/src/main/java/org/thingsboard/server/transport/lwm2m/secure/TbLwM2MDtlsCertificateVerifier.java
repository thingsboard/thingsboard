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
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.RequiredArgsConstructor;
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
import org.eclipse.californium.scandium.dtls.x509.StaticCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredentials;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;

import javax.annotation.PostConstruct;
import javax.security.auth.x500.X500Principal;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class TbLwM2MDtlsCertificateVerifier implements NewAdvancedCertificateVerifier {

    private final TransportService transportService;
    private final TbLwM2MDtlsSessionStore sessionStorage;
    private final LwM2MTransportServerConfig config;

    @SuppressWarnings("deprecation")
    private StaticCertificateVerifier staticCertificateVerifier;

    @Value("${transport.lwm2m.server.security.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    @Override
    public List<CertificateType> getSupportedCertificateType() {
        return Arrays.asList(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY);
    }

    @PostConstruct
    public void init() {
        try {
            /* by default trust all */
            X509Certificate[] trustedCertificates = new X509Certificate[0];
            if (config.getKeyStoreValue() != null) {
                X509Certificate rootCAX509Cert = (X509Certificate) config.getKeyStoreValue().getCertificate(config.getRootCertificateAlias());
                if (rootCAX509Cert != null) {
                    trustedCertificates = new X509Certificate[1];
                    trustedCertificates[0] = rootCAX509Cert;
                }
            }
            staticCertificateVerifier = new StaticCertificateVerifier(trustedCertificates);
        } catch (Exception e) {
            log.info("Failed to initialize the ");
        }
    }

    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName, Boolean clientUsage, boolean truncateCertificatePath, CertificateMessage message, DTLSSession session) {
        CertPath certChain = message.getCertificateChain();
        if (certChain == null) {
            //We trust all RPK on this layer, and use TbLwM2MAuthorizer
            PublicKey publicKey = message.getPublicKey();
            return new CertificateVerificationResult(cid, publicKey, null);
        } else {
            try {
                boolean x509CredentialsFound = false;
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
                        transportService.process(TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(sha3Hash).build(),
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
                        if (latch.await(10, TimeUnit.SECONDS)) {
                            ValidateDeviceCredentialsResponse msg = deviceCredentialsResponse[0];
                            if (msg != null && org.thingsboard.server.common.data.StringUtils.isNotEmpty(msg.getCredentials())) {
                                LwM2MCredentials credentials = JacksonUtil.fromString(msg.getCredentials(), LwM2MCredentials.class);
                                if(!credentials.getClient().getSecurityConfigClientMode().equals(LwM2MSecurityMode.X509)){
                                    continue;
                                }
                                X509ClientCredentials config = (X509ClientCredentials) credentials.getClient();
                                String certBody = config.getCert();
                                String endpoint = config.getEndpoint();
                                if (strCert.equals(certBody)) {
                                    x509CredentialsFound = true;
                                    DeviceProfile deviceProfile = msg.getDeviceProfile();
                                    if (msg.hasDeviceInfo() && deviceProfile != null) {
                                        sessionStorage.put(endpoint, new TbX509DtlsSessionInfo(cert.getSubjectX500Principal().getName(), msg));
                                        break;
                                    }
                                } else {
                                    log.trace("[{}][{}] Certificate mismatch. Expected: {}, Actual: {}", endpoint, sha3Hash, strCert, certBody);
                                }
                            }
                        }
                    } catch (InterruptedException |
                            CertificateEncodingException |
                            CertificateExpiredException |
                            CertificateNotYetValidException e) {
                        log.error(e.getMessage(), e);
                    }
                }
                if (!x509CredentialsFound) {
                    if (staticCertificateVerifier != null) {
                        staticCertificateVerifier.verifyCertificate(message, session);
                    } else {
                        AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.INTERNAL_ERROR,
                                session.getPeer());
                        throw new HandshakeException("x509 verification not enabled!", alert);
                    }
                }
                return new CertificateVerificationResult(cid, certpath, null);
            } catch (HandshakeException e) {
                log.trace("Certificate validation failed!", e);
                return new CertificateVerificationResult(cid, e, null);
            }
        }
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return CertPathUtil.toSubjects(null);
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {

    }
}
