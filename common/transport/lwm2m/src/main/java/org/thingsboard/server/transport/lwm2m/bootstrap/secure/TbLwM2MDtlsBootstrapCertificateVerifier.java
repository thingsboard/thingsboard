// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.DatagramWriter;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.queue.util.TbLwM2mBootstrapTransportComponent;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MTransportUnavailableException;

import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@TbLwM2mBootstrapTransportComponent
@RequiredArgsConstructor
public class TbLwM2MDtlsBootstrapCertificateVerifier implements NewAdvancedCertificateVerifier {

    // Length prefixes of the certificate list and of each certificate in a TLS/DTLS Certificate message (RFC 5246, 7.4.2)
    private static final int CERTIFICATE_LENGTH_BITS = 24;

    private final LwM2MTransportServerConfig config;
    private final LwM2MBootstrapSecurityStore bsSecurityStore;
    private final TransportRateLimitService rateLimitService;

    private StaticNewAdvancedCertificateVerifier staticCertificateVerifier;

    @Value("${transport.lwm2m.server.security.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return Arrays.asList(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY);
    }

    @SuppressWarnings("deprecation")
    @PostConstruct
    public void init() {
        try {
            /* by default trust all */
            if (config.getTrustSslCredentials() != null) {
                X509Certificate[] trustedCertificates = config.getTrustSslCredentials().getTrustedCertificates();
                staticCertificateVerifier = new StaticNewAdvancedCertificateVerifier(trustedCertificates, new RawPublicKeyIdentity[0], null);
            }
        } catch (Exception e) {
            log.warn("ailed to initialize the LwM2M certificate verifier", e);
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
                boolean coreUnavailable = false;
                try {
                    x509CredentialsFound = isRegistered(cid, serverName, remotePeer, clientUsage, verifySubject, truncateCertificatePath, message);
                } catch (LwM2MTransportUnavailableException e) {
                    coreUnavailable = true;
                }
                if (!x509CredentialsFound) {
                    if (!coreUnavailable) {
                        rateLimitService.onAuthFailure(remotePeer);
                    }
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

    /**
     * Checks the certificate of an established DTLS session the way a handshake does: a kept or resumed session skips the
     * handshake, and its certificate may have been revoked or removed from the device credentials since.
     *
     * @throws LwM2MTransportUnavailableException if the core did not answer
     */
    public boolean isRegistered(X509CertPath certPath, InetSocketAddress remotePeer) {
        CertificateMessage message;
        try {
            message = toCertificateMessage(certPath.getPath());
        } catch (CertificateEncodingException | HandshakeException e) {
            log.trace("[{}] Failed to encode the certificate chain of the DTLS session", remotePeer, e);
            return false;
        }
        return isRegistered(ConnectionId.EMPTY, null, remotePeer, true, false, true, message);
    }

    /**
     * The Certificate message of the chain as the client sent it, decoded the way the handshake decoded it:
     * {@code new CertificateMessage(List)} rejects a chain whose certificates are not in issuer order, which the handshake accepts.
     */
    private static CertificateMessage toCertificateMessage(CertPath certPath) throws CertificateEncodingException, HandshakeException {
        DatagramWriter certificates = new DatagramWriter();
        for (Certificate certificate : certPath.getCertificates()) {
            byte[] encoded = certificate.getEncoded();
            certificates.write(encoded.length, CERTIFICATE_LENGTH_BITS);
            certificates.writeBytes(encoded);
        }
        byte[] certificateList = certificates.toByteArray();
        DatagramWriter message = new DatagramWriter();
        message.write(certificateList.length, CERTIFICATE_LENGTH_BITS);
        message.writeBytes(certificateList);
        return CertificateMessage.fromReader(new DatagramReader(message.toByteArray()), CertificateType.X_509);
    }

    /**
     * @throws LwM2MTransportUnavailableException if the credentials were not found and the core did not answer
     */
    private boolean isRegistered(ConnectionId cid, ServerNames serverName, InetSocketAddress remotePeer, boolean clientUsage,
                                 boolean verifySubject, boolean truncateCertificatePath, CertificateMessage message) {
        boolean x509CredentialsFound = false;
        boolean coreUnavailable = false;
        // Only the leaf: the client proves that it holds the private key of the leaf, not of the other certificates
        // in the chain, which are not validated in X.509 mode and are never compared with the endpoint
        List<? extends Certificate> chain = message.getCertificateChain().getCertificates();
        if (!chain.isEmpty() && chain.get(0) instanceof X509Certificate cert) {
            try {
                if (!skipValidityCheckForClientCert) {
                    cert.checkValidity();
                }
                // Sent along with every lookup, so the core rejects a revoked certificate also in trust mode
                String strCert = SslUtil.getCertificateString(cert);
                String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                TbLwM2MSecurityInfo securityInfo = null;
                // verify if trust
                if (staticCertificateVerifier != null) {
                    HandshakeException exception = staticCertificateVerifier.verifyCertificate(cid, serverName, remotePeer, clientUsage, verifySubject, truncateCertificatePath, message).getException();
                    if (exception == null) {
                        String endpoint = config.getTrustSslCredentials().getValueFromSubjectNameByKey(cert.getSubjectX500Principal().getName(), "CN");
                        if (StringUtils.isNotEmpty(endpoint)) {
                            try {
                                securityInfo = bsSecurityStore.getX509ByEndpoint(endpoint, sha3Hash);
                            } catch (LwM2MTransportUnavailableException e) {
                                coreUnavailable = true;
                            } catch (LwM2MAuthException e) {
                                log.trace("Failed to find security info: [{}]", endpoint, e);
                            }
                        }
                    } else {
                        log.trace("Certificate validation failed.", exception);
                    }
                }
                // if not trust or cert trust securityInfo == null
                if (securityInfo == null || securityInfo.getMsg() == null) {
                    try {
                        securityInfo = bsSecurityStore.getX509ByEndpoint(sha3Hash, sha3Hash);
                    } catch (LwM2MTransportUnavailableException e) {
                        coreUnavailable = true;
                    } catch (LwM2MAuthException e) {
                        log.trace("Failed to find security info: [{}]", sha3Hash, e);
                    }
                }
                ValidateDeviceCredentialsResponse msg = securityInfo != null ? securityInfo.getMsg() : null;
                if (msg != null && StringUtils.isNotEmpty(msg.getCredentials())) {
                    x509CredentialsFound = true;
                }
            } catch (CertificateEncodingException |
                    CertificateExpiredException |
                    CertificateNotYetValidException e) {
                log.trace("Failed to find security info: [{}]", cert.getSubjectX500Principal().getName(), e);
            }
        }
        if (!x509CredentialsFound && coreUnavailable) {
            throw new LwM2MTransportUnavailableException();
        }
        return x509CredentialsFound;
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return CertPathUtil.toSubjects(null);
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {

    }
}
