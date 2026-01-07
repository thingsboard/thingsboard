/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MClientCredentials;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;

import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.CLIENT;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class TbLwM2MDtlsCertificateVerifier implements NewAdvancedCertificateVerifier {

    private final TbLwM2MDtlsSessionStore sessionStorage;
    private final LwM2MTransportServerConfig config;
    private final LwM2mCredentialsSecurityInfoValidator securityInfoValidator;
    private final TbMainSecurityStore securityStore;

    private StaticNewAdvancedCertificateVerifier staticCertificateVerifier;

    @Value("${transport.lwm2m.server.security.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return Arrays.asList(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY);
    }

    @PostConstruct
    public void init() {
        try {
            /* by default trust all */
            if (config.getTrustSslCredentials() != null) {
                X509Certificate[] trustedCertificates = config.getTrustSslCredentials().getTrustedCertificates();
                staticCertificateVerifier = new StaticNewAdvancedCertificateVerifier(trustedCertificates, new RawPublicKeyIdentity[0], null);
            }
        } catch (Exception e) {
            log.warn("Failed to initialize the LwM2M certificate verifier", e);
        }
    }

    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName, InetSocketAddress remotePeer,
                                                           boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath,
                                                           CertificateMessage message) {
        CertPath certChain = message.getCertificateChain();
        if (certChain == null) {
            //We trust all RPK on this layer, and use TbLwM2MAuthorizer
            PublicKey publicKey = message.getPublicKey();
            return new CertificateVerificationResult(cid, publicKey, null);
        } else {
            try {
                boolean x509CredentialsFound = false;
                X509Certificate[] chain = certChain.getCertificates().toArray(new X509Certificate[0]);
                for (X509Certificate cert : chain) {
                    try {
                        if (!skipValidityCheckForClientCert) {
                            cert.checkValidity();
                        }
                        TbLwM2MSecurityInfo securityInfo = null;
                        if (staticCertificateVerifier != null) {
                            HandshakeException exception = staticCertificateVerifier.verifyCertificate(cid, serverName, remotePeer, clientUsage, verifySubject, truncateCertificatePath, message).getException();
                            if (exception == null) {
                                try {
                                    String endpoint = config.getTrustSslCredentials().getValueFromSubjectNameByKey(cert.getSubjectX500Principal().getName(), "CN");
                                    if (StringUtils.isNotEmpty(endpoint)) {
                                        securityInfo = securityInfoValidator.getEndpointSecurityInfoByCredentialsId(endpoint, CLIENT);
                                    }
                                } catch (LwM2MAuthException e) {
                                    log.trace("Certificate trust validation failed.", e);
                                }
                            } else {
                                log.trace("Certificate trust validation failed.", exception);
                            }
                        }
                        // if not trust or cert trust securityInfo == null
                        String strCert = SslUtil.getCertificateString(cert);
                        String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                        if (securityInfo == null || securityInfo.getMsg() == null) {
                            try {
                                securityInfo = securityInfoValidator.getEndpointSecurityInfoByCredentialsId(sha3Hash, CLIENT);
                            } catch (LwM2MAuthException e) {
                                log.trace("Failed find security info: {}", sha3Hash, e);
                            }
                        }
                        ValidateDeviceCredentialsResponse msg = securityInfo != null ? securityInfo.getMsg() : null;
                        if (msg != null && StringUtils.isNotEmpty(msg.getCredentials())) {
                            LwM2MClientCredentials credentials = JacksonUtil.fromString(msg.getCredentials(), LwM2MClientCredentials.class);
                            if (!credentials.getClient().getSecurityConfigClientMode().equals(LwM2MSecurityMode.X509)) {
                                continue;
                            }
                            X509ClientCredential config = (X509ClientCredential) credentials.getClient();
                            String certBody = config.getCert();
                            String endpoint = config.getEndpoint();
                            if (StringUtils.isBlank(certBody) || strCert.equals(certBody)) {
                                x509CredentialsFound = true;
                                DeviceProfile deviceProfile = msg.getDeviceProfile();
                                if (msg.hasDeviceInfo() && deviceProfile != null) {
                                    sessionStorage.put(endpoint, new TbX509DtlsSessionInfo(cert.getSubjectX500Principal().getName(), msg));
                                    try {
                                        securityStore.putX509(securityInfo);
                                    } catch (NonUniqueSecurityInfoException e) {
                                        log.trace("Failed to add security info: {}", securityInfo, e);
                                    }
                                    break;
                                }
                            } else {
                                log.trace("[{}][{}] Certificate mismatch. Expected: {}, Actual: {}", endpoint, sha3Hash, strCert, certBody);
                            }
                        }
                    } catch (CertificateEncodingException |
                            CertificateExpiredException |
                            CertificateNotYetValidException e) {
                        log.error(e.getMessage(), e);
                    }
                }
                if (!x509CredentialsFound) {
                    AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.INTERNAL_ERROR);
                    throw new HandshakeException("x509 verification not enabled!", alert);
                }
                return new CertificateVerificationResult(cid, certChain, null);
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
