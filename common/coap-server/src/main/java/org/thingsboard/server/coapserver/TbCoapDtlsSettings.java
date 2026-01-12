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
package org.thingsboard.server.coapserver;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.MaxFragmentLengthExtension.Length;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.californium.elements.config.CertificateAuthenticationMode.WANTED;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_LENGTH;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_NODE_ID;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_MAX_FRAGMENT_LENGTH;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_MAX_TRANSMISSION_UNIT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole.SERVER_ONLY;

@Getter
@Slf4j
@ConditionalOnProperty(prefix = "coap.dtls", value = "enabled", havingValue = "true")
@Component
public class TbCoapDtlsSettings {

    @Value("${coap.dtls.bind_address}")
    private String host;

    @Value("${coap.dtls.bind_port}")
    private Integer port;

    @Value("${coap.dtls.retransmission_timeout:9000}")
    private int dtlsRetransmissionTimeout;

    @Value("${coap.dtls.connection_id_length:}")
    private Integer cIdLength;

    @Value("${coap.dtls.max_transmission_unit:1024}")
    private Integer maxTransmissionUnit;

    @Value("${coap.dtls.max_fragment_length:1024}")
    private Integer maxFragmentLength;

    @Bean
    @ConfigurationProperties(prefix = "coap.dtls.credentials")
    public SslCredentialsConfig coapDtlsCredentials() {
        return new SslCredentialsConfig("COAP DTLS Credentials", false);
    }

    @Autowired
    @Qualifier("coapDtlsCredentials")
    private SslCredentialsConfig coapDtlsCredentialsConfig;

    @Value("${coap.dtls.x509.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    @Value("${coap.dtls.x509.dtls_session_inactivity_timeout:86400000}")
    private long dtlsSessionInactivityTimeout;

    @Value("${coap.dtls.x509.dtls_session_report_timeout:1800000}")
    private long dtlsSessionReportTimeout;

    @Autowired(required = false)
    private TransportService transportService;

    @Autowired(required = false)
    private TbServiceInfoProvider serviceInfoProvider;

    public DtlsConnectorConfig dtlsConnectorConfig(Configuration configuration) throws UnknownHostException {
        DtlsConnectorConfig.Builder configBuilder = new DtlsConnectorConfig.Builder(configuration);
        configBuilder.setAddress(getInetSocketAddress());
        SslCredentials sslCredentials = this.coapDtlsCredentialsConfig.getCredentials();
        SslContextUtil.Credentials serverCredentials =
                new SslContextUtil.Credentials(sslCredentials.getPrivateKey(), null, sslCredentials.getCertificateChain());
        configBuilder.set(DTLS_CLIENT_AUTHENTICATION_MODE, WANTED);
        configBuilder.set(DTLS_RETRANSMISSION_TIMEOUT, dtlsRetransmissionTimeout, MILLISECONDS);
        configBuilder.set(DTLS_ROLE, SERVER_ONLY);
        if (cIdLength != null) {
            configBuilder.set(DTLS_CONNECTION_ID_LENGTH, cIdLength);
            if (cIdLength > 4) {
                configBuilder.set(DTLS_CONNECTION_ID_NODE_ID, 0);
            } else {
                configBuilder.set(DTLS_CONNECTION_ID_NODE_ID, null);
            }
        }
        if (maxTransmissionUnit > 0) {
            configBuilder.set(DTLS_MAX_TRANSMISSION_UNIT, maxTransmissionUnit);
        }
        if (maxFragmentLength > 0) {
            Length length = fromLength(maxFragmentLength);
            if (length != null) {
                configBuilder.set(DTLS_MAX_FRAGMENT_LENGTH, length);
            }
        }
        configBuilder.setAdvancedCertificateVerifier(
                new TbCoapDtlsCertificateVerifier(
                        transportService,
                        serviceInfoProvider,
                        dtlsSessionInactivityTimeout,
                        dtlsSessionReportTimeout,
                        skipValidityCheckForClientCert
                )
        );
        configBuilder.setCertificateIdentityProvider(new SingleCertificateProvider(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(),
                Collections.singletonList(CertificateType.X_509)));
        return configBuilder.build();
    }

    private InetSocketAddress getInetSocketAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(host);
        return new InetSocketAddress(addr, port);
    }


    private static Length fromLength(int length) {
        for (Length l : Length.values()) {
            if (l.length() == length) {
                return l;
            }
        }
        return null;
    }
}

