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
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.config.transport.CoapTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CoapTransportHealthChecker extends TransportHealthChecker<CoapTransportMonitoringConfig> {

    static {
        SystemConfig.register();
        CoapConfig.register();
    }

    private CoapClient coapClient;
    private CoapEndpoint coapEndpoint;

    protected CoapTransportHealthChecker(CoapTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (coapClient != null) {
            if (isSessionExpired()) {
                log.info("Reconnecting {} client to {}", getTransportType(), target.getBaseUrl());
                shutdownCoapClient();
            } else {
                return;
            }
        }

        String accessToken = target.getDevice().getCredentials().getCredentialsId();
        String uri = target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry";
        coapClient = new CoapClient(uri);
        if (target.getBaseUrl().startsWith("coaps")) {
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
            dtlsConfigBuilder.setCertificateIdentityProvider(new AnonymousCertificateProvider());
            dtlsConfigBuilder.setAdvancedCertificateVerifier(
                    StaticNewAdvancedCertificateVerifier.builder().setTrustAllCertificates().build());

            coapEndpoint = new CoapEndpoint.Builder().setConnector(new DTLSConnector(dtlsConfigBuilder.build())).build();
            coapClient.setEndpoint(coapEndpoint);
        }
        coapClient.setTimeout((long) config.getRequestTimeoutMs());
        recordSessionStart();
        log.debug("Connecting {} client to {}", getTransportType(), target.getBaseUrl());
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        CoapResponse response = coapClient.post(payload, MediaTypeRegistry.APPLICATION_JSON);
        if (response == null) {
            throw new IOException(getTransportType() + " request timed out");
        }
        if (response.getCode().codeClass != CoAP.CodeClass.SUCCESS_RESPONSE.value) {
            throw new IOException(getTransportType() + " client didn't receive success response from transport");
        }
    }

    @Override
    protected void destroyClient() {
        if (coapClient != null) {
            shutdownCoapClient();
        }
    }

    private void shutdownCoapClient() {
        try {
            coapClient.shutdown();
        } catch (Exception e) {
            log.warn("Failed to shutdown CoAP client: {}", e.getMessage());
        } finally {
            if (coapEndpoint != null) {
                try {
                    coapEndpoint.destroy();
                } catch (Exception e) {
                    log.warn("Failed to destroy CoAP endpoint: {}", e.getMessage());
                }
                coapEndpoint = null;
            }
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
            return List.of(CipherSuite.CertificateKeyAlgorithm.EC, CipherSuite.CertificateKeyAlgorithm.RSA);
        }

        @Override
        public List<CertificateType> getSupportedCertificateTypes() {
            return List.of(CertificateType.X_509);
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