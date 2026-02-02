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
package org.thingsboard.server.transport.lwm2m.bootstrap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.server.californium.bootstrap.LwM2mBootstrapPskStore;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.CaliforniumBootstrapServerEndpointsProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coap.CoapBootstrapServerProtocolProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coaps.CoapsBootstrapServerProtocolProvider;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.queue.util.TbLwM2mBootstrapTransportComponent;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2mDefaultBootstrapSessionManager;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.TbLwM2MDtlsBootstrapCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MInMemoryBootstrapConfigStore;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CURVES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.thingsboard.server.transport.lwm2m.server.DefaultLwM2mTransportService.PSK_CIPHER_SUITES;
import static org.thingsboard.server.transport.lwm2m.server.DefaultLwM2mTransportService.RPK_OR_X509_CIPHER_SUITES;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MNetworkConfig.getCoapConfig;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.setDtlsConnectorConfigCidLength;

@Slf4j
@Component
@TbLwM2mBootstrapTransportComponent
@RequiredArgsConstructor
public class LwM2MTransportBootstrapService {

    private final LwM2MTransportServerConfig serverConfig;
    private final LwM2MTransportBootstrapConfig bootstrapConfig;
    private final LwM2MBootstrapSecurityStore lwM2MBootstrapSecurityStore;
    private final LwM2MInMemoryBootstrapConfigStore lwM2MInMemoryBootstrapConfigStore;
    private final TransportService transportService;
    private final TbLwM2MDtlsBootstrapCertificateVerifier certificateVerifier;
    private LeshanBootstrapServer server;

    @PostConstruct
    public void init() {
        log.info("Starting LwM2M transport bootstrap server...");
        this.server = getLhBootstrapServer();
        this.server.start();
        log.info("Started LwM2M transport bootstrap server.");
    }

    @PreDestroy
    public void shutdown() {
        try {
            log.info("Stopping LwM2M transport bootstrap server!");
            server.destroy();
            log.info("LwM2M transport bootstrap server stopped!");
        } catch (Exception e) {
            log.error("Failed to gracefully stop the LwM2M transport bootstrap server!", e);
        }
    }

    public LeshanBootstrapServer getLhBootstrapServer() {
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();

        // Create Californium Endpoints Provider:
        // ------------------
        // Create Server Endpoints Provider
        CaliforniumBootstrapServerEndpointsProvider.Builder endpointsBuilder = new CaliforniumBootstrapServerEndpointsProvider.Builder(
                // Add coap Protocol support
                new CoapBootstrapServerProtocolProvider(),

                // Add coaps/dtls protocol support
                new CoapsBootstrapServerProtocolProvider(c -> {
                    if (this.bootstrapConfig.getSslCredentials() != null) {
                        c.setAdvancedCertificateVerifier(certificateVerifier);
                        c.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, RPK_OR_X509_CIPHER_SUITES);
                    } else {
                        log.info("Unable to load X509 files for LWM2MServer");
                        LwM2mBootstrapPskStore lwM2mBsPskStore = new LwM2mBootstrapPskStore(lwM2MBootstrapSecurityStore);
                        c.setAdvancedPskStore(lwM2mBsPskStore);
                        c.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, PSK_CIPHER_SUITES);
                    }
                }));


        // Create Californium Configuration
        Configuration serverCoapConfig = endpointsBuilder.createDefaultConfiguration();
        getCoapConfig(serverCoapConfig, bootstrapConfig.getPort(), bootstrapConfig.getSecurePort(),serverConfig);
        serverCoapConfig.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        serverCoapConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, serverConfig.isRecommendedCiphers());
        serverCoapConfig.setTransient(DtlsConfig.DTLS_CONNECTION_ID_LENGTH);
        serverCoapConfig.set(DTLS_RECOMMENDED_CURVES_ONLY, serverConfig.isRecommendedSupportedGroups());
        serverCoapConfig.setTransient(DTLS_RETRANSMISSION_TIMEOUT);
        serverCoapConfig.set(DTLS_RETRANSMISSION_TIMEOUT, serverConfig.getDtlsRetransmissionTimeout(), MILLISECONDS);

        if (serverConfig.getDtlsCidLength() != null) {
            setDtlsConnectorConfigCidLength( serverCoapConfig, serverConfig.getDtlsCidLength());
        }

        /* Create DTLS Config */
        this.setServerWithCredentials(builder);

        // Set Californium Configuration
        endpointsBuilder.setConfiguration(serverCoapConfig);
        serverConfig.setCoapConfig(serverCoapConfig);


        // Create CoAP endpoint
        InetSocketAddress coapAddr = new InetSocketAddress(bootstrapConfig.getHost(), bootstrapConfig.getPort());
        endpointsBuilder.addEndpoint(coapAddr, Protocol.COAP);

        // Create CoAP over DTLS endpoint
        InetSocketAddress coapsAddr = new InetSocketAddress(bootstrapConfig.getSecureHost(), bootstrapConfig.getSecurePort());
        endpointsBuilder.addEndpoint(coapsAddr, Protocol.COAPS);

        /* Set securityStore with new ConfigStore */
        builder.setConfigStore(lwM2MInMemoryBootstrapConfigStore);

        /* SecurityStore */
        builder.setSecurityStore(lwM2MBootstrapSecurityStore);


        BootstrapSessionManager sessionManager = new LwM2mDefaultBootstrapSessionManager(lwM2MBootstrapSecurityStore, lwM2MInMemoryBootstrapConfigStore, transportService);
        builder.setSessionManager(sessionManager);

        /* Create BootstrapServer */
        builder.setEndpointsProviders(endpointsBuilder.build());
        return builder.build();
    }

    private void setServerWithCredentials(LeshanBootstrapServerBuilder builder) {
        if (this.bootstrapConfig.getSslCredentials() != null) {
            SslCredentials sslCredentials = this.bootstrapConfig.getSslCredentials();
            builder.setPublicKey(sslCredentials.getPublicKey());
            builder.setPrivateKey(sslCredentials.getPrivateKey());
            builder.setCertificateChain(sslCredentials.getCertificateChain());
        } else {
            /* by default trust all */
            builder.setTrustedCertificates(new X509Certificate[0]);
        }
    }
}
