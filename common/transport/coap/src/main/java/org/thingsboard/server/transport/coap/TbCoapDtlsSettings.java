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
package org.thingsboard.server.transport.coap;

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@ConditionalOnProperty(prefix = "transport.coap.dtls", value = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnExpression("'${transport.type:null}'=='null' || ('${transport.type}'=='local' && '${transport.coap.enabled}'=='true')")
@Component
public class TbCoapDtlsSettings {

    @Value("${transport.coap.dtls.bind_address}")
    private String host;

    @Value("${transport.coap.dtls.bind_port}")
    private Integer port;

    @Value("${transport.coap.dtls.mode}")
    private String mode;

    @Value("${transport.coap.dtls.key_store}")
    private String keyStoreFile;

    @Value("${transport.coap.dtls.key_store_password}")
    private String keyStorePassword;

    @Value("${transport.coap.dtls.key_password}")
    private String keyPassword;

    @Value("${transport.coap.dtls.key_alias}")
    private String keyAlias;

    @Autowired
    private TransportService transportService;

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    public DtlsConnectorConfig dtlsConnectorConfig() throws UnknownHostException {
        Optional<SecurityMode> securityModeOpt = SecurityMode.parse(mode);
        if (securityModeOpt.isEmpty()) {
            log.warn("Incorrect configuration of securityMode {}", mode);
            throw new RuntimeException("Failed to parse mode property: " + mode + "!");
        } else {
            DtlsConnectorConfig.Builder configBuilder = new DtlsConnectorConfig.Builder();
            configBuilder.setAddress(getInetSocketAddress());
            SecurityMode securityMode = securityModeOpt.get();
            if (securityMode.equals(SecurityMode.NO_AUTH)) {
                configBuilder.setClientAuthenticationRequired(false);
                String keyStoreFilePath = Resources.getResource(keyStoreFile).getPath();
                SslContextUtil.Credentials serverCredentials = loadServerCredentials(keyStoreFilePath);
                loadTrustedCertificates(configBuilder, keyStoreFilePath);
                configBuilder.setIdentity(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(),
                                Collections.singletonList(CertificateType.X_509));
            } else {
                String keyStoreFilePath = Resources.getResource(keyStoreFile).getPath();
                SslContextUtil.Credentials serverCredentials = loadServerCredentials(keyStoreFilePath);
                configBuilder.setAdvancedCertificateVerifier(new TbCoapDtlsCertificateVerifier(transportService, serviceInfoProvider));
                configBuilder.setIdentity(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(),
                        Collections.singletonList(CertificateType.X_509));
            }
            return configBuilder.build();
        }
    }

    private SslContextUtil.Credentials loadServerCredentials(String keyStoreFilePath) {
        try {
            return SslContextUtil.loadCredentials(keyStoreFilePath, keyAlias, keyStorePassword.toCharArray(),
                    keyPassword.toCharArray());
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to load serverCredentials due to: ", e);
        }
    }

    private void loadTrustedCertificates(DtlsConnectorConfig.Builder config, String keyStoreFilePath) {
        StaticNewAdvancedCertificateVerifier.Builder trustBuilder = StaticNewAdvancedCertificateVerifier.builder();
        try {
            Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(
                    keyStoreFilePath, keyAlias,
                    keyStorePassword.toCharArray());
            trustBuilder.setTrustedCertificates(trustedCertificates);
            if (trustBuilder.hasTrusts()) {
                config.setAdvancedCertificateVerifier(trustBuilder.build());
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to load trusted certificates due to: ", e);
        }
    }

    private InetSocketAddress getInetSocketAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(host);
        return new InetSocketAddress(addr, port);
    }

    private enum SecurityMode {
        X509,
        NO_AUTH;

        static Optional<SecurityMode> parse(String name) {
            SecurityMode mode = null;
            if (name != null) {
                for (SecurityMode securityMode : SecurityMode.values()) {
                    if (securityMode.name().equalsIgnoreCase(name)) {
                        mode = securityMode;
                        break;
                    }
                }
            }
            return Optional.ofNullable(mode);
        }

    }

}