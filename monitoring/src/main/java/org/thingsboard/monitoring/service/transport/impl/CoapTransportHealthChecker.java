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
package org.thingsboard.monitoring.service.transport.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateIdentityResult;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.SignatureAndHashAlgorithm;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.cipher.XECDHECryptography;
import org.eclipse.californium.scandium.dtls.x509.CertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.config.transport.CoapTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.CoapsTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TlsConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;
import org.thingsboard.monitoring.util.SslUtil;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CoapTransportHealthChecker extends TransportHealthChecker<CoapTransportMonitoringConfig> {

    static {
        SystemConfig.register();
        CoapConfig.register();
    }

    @Value("${monitoring.domain}")
    private String domain;

    private CoapClient coapClient;

    protected CoapTransportHealthChecker(CoapTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        String accessToken = target.getDevice().getCredentials().getCredentialsId();
        String uri = target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry";
        coapClient = new CoapClient(uri);
        if (getTransportType() == TransportType.COAPS) {
            TlsConfig tlsConfig = ((CoapsTransportMonitoringConfig) config).getTls();
            Configuration dtlsConfiguration = new Configuration();

            dtlsConfiguration.set(DtlsConfig.DTLS_ROLE, DtlsConfig.DtlsRole.CLIENT_ONLY);
            dtlsConfiguration.set(DtlsConfig.DTLS_USE_SERVER_NAME_INDICATION, true);
            dtlsConfiguration.set(DtlsConfig.DTLS_VERIFY_SERVER_CERTIFICATES_SUBJECT, false);
            dtlsConfiguration.setTransient(DtlsConfig.DTLS_SIGNATURE_AND_HASH_ALGORITHMS);
            dtlsConfiguration.set(DtlsConfig.DTLS_SIGNATURE_AND_HASH_ALGORITHMS, Arrays.asList(
                    SignatureAndHashAlgorithm.SHA256_WITH_ECDSA,
                    SignatureAndHashAlgorithm.SHA384_WITH_ECDSA,
                    SignatureAndHashAlgorithm.SHA256_WITH_RSA));

            DtlsConnectorConfig.Builder dtlsConfigBuilder = DtlsConnectorConfig.builder(dtlsConfiguration);

            boolean hasClientKeystore = tlsConfig != null && tlsConfig.getKeystore().isPresent();
            if (hasClientKeystore) {
                KeyStore keyStore = SslUtil.loadKeyStore(tlsConfig.getKeystore().get(), tlsConfig.getKeystorePassword());
                PrivateKey privateKey = (PrivateKey) keyStore.getKey(tlsConfig.getKeystoreKeyAlias(), tlsConfig.getKeystorePassword().toCharArray());
                Certificate[] certificateChain = keyStore.getCertificateChain(tlsConfig.getKeystoreKeyAlias());
                dtlsConfigBuilder.setCertificateIdentityProvider(new SingleCertificateProvider(privateKey, certificateChain));
            } else {
                // Server uses WANTED mode — no client cert required.
                // AnonymousCertificateProvider signals EC support (for ECDSA cipher suites)
                // but sends an empty Certificate message during the handshake.
                dtlsConfigBuilder.setCertificateIdentityProvider(new AnonymousCertificateProvider());
            }

            Certificate[] fetchedCerts = null;
            if (tlsConfig != null && tlsConfig.isFetchCertificateChain()) {
                try {
                    fetchedCerts = SslUtil.getCertificatesFromUrl(getTransportType(), domain);
                } catch (Exception e) {
                    log.warn("Failed to fetch certificate chain for {}, falling back to truststore: {}", getTransportType(), e.getMessage());
                }
            }
            if (fetchedCerts != null && fetchedCerts.length > 0) {
                dtlsConfigBuilder.setAdvancedCertificateVerifier(
                        StaticNewAdvancedCertificateVerifier.builder()
                                .setTrustedCertificates(fetchedCerts)
                                .build());
            } else if (tlsConfig != null && tlsConfig.getTruststore().isPresent()) {
                KeyStore trustStore = SslUtil.loadKeyStore(tlsConfig.getTruststore().get(), tlsConfig.getTruststorePassword());
                List<Certificate> trustCerts = new ArrayList<>();
                for (String alias : Collections.list(trustStore.aliases())) {
                    if (trustStore.isCertificateEntry(alias)) {
                        trustCerts.add(trustStore.getCertificate(alias));
                    }
                }
                dtlsConfigBuilder.setAdvancedCertificateVerifier(
                        StaticNewAdvancedCertificateVerifier.builder()
                                .setTrustedCertificates(trustCerts.toArray(new Certificate[0]))
                                .build());
            } else {
                dtlsConfigBuilder.setAdvancedCertificateVerifier(
                        StaticNewAdvancedCertificateVerifier.builder()
                                .setTrustAllCertificates()
                                .build());
            }
            coapClient.setEndpoint(new CoapEndpoint.Builder().setConnector(new DTLSConnector(dtlsConfigBuilder.build())).build());
        }
        coapClient.setTimeout((long) config.getRequestTimeoutMs());
        log.debug("Connecting {} client to {}", getTransportType(), target.getBaseUrl());
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        CoapResponse response = coapClient.post(payload, MediaTypeRegistry.APPLICATION_JSON);
        CoAP.ResponseCode code = response.getCode();
        if (code.codeClass != CoAP.CodeClass.SUCCESS_RESPONSE.value) {
            throw new IOException(getTransportType() + " client didn't receive success response from transport");
        }
    }

    @Override
    protected void destroyClient() throws Exception {
        if (coapClient != null) {
            coapClient.shutdown();
            coapClient = null;
            log.debug("Disconnected {} client", getTransportType());
        }
    }

    @Override
    protected TransportType getTransportType() {
        return config.getTransportType();
    }

    private static class AnonymousCertificateProvider implements CertificateProvider {

        @Override
        public List<CipherSuite.CertificateKeyAlgorithm> getSupportedCertificateKeyAlgorithms() {
            return Collections.singletonList(CipherSuite.CertificateKeyAlgorithm.EC);
        }

        @Override
        public List<CertificateType> getSupportedCertificateTypes() {
            return Collections.singletonList(CertificateType.X_509);
        }

        @Override
        public CertificateIdentityResult requestCertificateIdentity(ConnectionId cid, boolean client,
                List<X500Principal> issuers, ServerNames serverNames,
                List<CipherSuite.CertificateKeyAlgorithm> keyAlgorithms,
                List<SignatureAndHashAlgorithm> signatureAndHashAlgorithms,
                List<XECDHECryptography.SupportedGroup> curves) {
            return new CertificateIdentityResult(cid);
        }

        @Override
        public void setResultHandler(HandshakeResultHandler handler) {}
    }

}