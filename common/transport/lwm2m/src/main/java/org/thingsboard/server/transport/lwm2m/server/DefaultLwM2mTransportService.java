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
package org.thingsboard.server.transport.lwm2m.server;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.LwM2mPskStore;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.server.californium.endpoint.coaps.CoapsServerProtocolProvider;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MAuthorizer;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CURVES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole.SERVER_ONLY;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CCM_8;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MNetworkConfig.getCoapConfig;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_UPDATE_COAP_RESOURCE;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_UPDATE_COAP_RESOURCE;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.setDtlsConnectorConfigCidLength;

@Slf4j
@Component
@DependsOn({"lwM2mDownlinkMsgHandler", "lwM2mUplinkMsgHandler"})
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mTransportService implements LwM2MTransportService, SmartInitializingSingleton {

    public static final CipherSuite[] RPK_OR_X509_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256};
    public static final CipherSuite[] PSK_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256};

    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    private final OtaPackageDataCache otaPackageDataCache;
    private final LwM2mUplinkMsgHandler handler;
    private final RegistrationStore registrationStore;
    private final TbSecurityStore securityStore;
    private final TbLwM2MDtlsCertificateVerifier certificateVerifier;
    private final TbLwM2MAuthorizer authorizer;
    private final LwM2mVersionedModelProvider modelProvider;

    private volatile LeshanServer server;
    private volatile LwM2mServerListener serverListener;

    @Override
    public void afterSingletonsInstantiated() {
        config.registerServerReloadCallback(() -> {
            try {
                log.info("LwM2M certificates reloaded. Recreating LwM2M server...");
                recreateLwM2mServer();
                log.info("LwM2M server recreated successfully with new certificates.");
            } catch (Exception e) {
                log.error("Failed to recreate LwM2M server after certificate reload", e);
            }
        });
    }

    @AfterStartUp(order = AfterStartUp.AFTER_TRANSPORT_SERVICE)
    public void init() {
        this.server = getLhServer();
        this.context.setServer(server);
        this.startLhServer();
    }

    private void startLhServer() {
        log.info("Starting LwM2M transport server...");
        this.server.start();
        serverListener = new LwM2mServerListener(handler);
        this.server.getRegistrationService().addListener(serverListener.registrationListener);
        this.server.getPresenceService().addListener(serverListener.presenceListener);
        this.server.getObservationService().addListener(serverListener.observationListener);
        this.server.getSendService().addListener(serverListener.sendListener);
        log.info("Started LwM2M transport server.");
    }

    @PreDestroy
    public void shutdown() {
        try {
            log.info("Stopping LwM2M transport server!");
            server.destroy();
            log.info("LwM2M transport server stopped!");
        } catch (Exception e) {
            log.error("Failed to gracefully stop the LwM2M transport server!", e);
        }
    }

    private LeshanServer getLhServer() {
        LeshanServerBuilder builder = new LeshanServerBuilder();

        /* Define model provider (Create Models )*/
        builder.setObjectModelProvider(modelProvider);


        /* Set securityStore with new registrationStore */
        builder.setSecurityStore(securityStore);
        builder.setRegistrationStore(registrationStore);
        builder.setAuthorizer(authorizer);


        // Create Californium Endpoints Provider:
        // ------------------
        // Create Server Endpoints Provider
        CaliforniumServerEndpointsProvider.Builder endpointsBuilder = new CaliforniumServerEndpointsProvider.Builder(
                // Add coap Protocol support
                new CoapServerProtocolProvider(),

                // Add coaps/dtls protocol support
                new CoapsServerProtocolProvider(c -> {
                    if (this.config.getSslCredentials() != null) {
                        c.setAdvancedCertificateVerifier(certificateVerifier);
                        c.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, RPK_OR_X509_CIPHER_SUITES);
                    } else {
                        log.info("Unable to load X509 files for LWM2MServer");
                        LwM2mPskStore lwM2mPskStore = new LwM2mPskStore(securityStore, registrationStore);
                        c.setAdvancedPskStore(lwM2mPskStore);
                        c.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, PSK_CIPHER_SUITES);
                    }
                }));

        // Create Californium Configuration
        Configuration serverCoapConfig = endpointsBuilder.createDefaultConfiguration();
        getCoapConfig(serverCoapConfig, config.getPort(), config.getSecurePort(), config);

        // Set some DTLS stuff

        serverCoapConfig.setTransient(DTLS_RECOMMENDED_CURVES_ONLY);
        serverCoapConfig.set(DTLS_RECOMMENDED_CURVES_ONLY, config.isRecommendedSupportedGroups());

        serverCoapConfig.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        serverCoapConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, config.isRecommendedCiphers());

        serverCoapConfig.set(DTLS_RETRANSMISSION_TIMEOUT, config.getDtlsRetransmissionTimeout(), MILLISECONDS);
        serverCoapConfig.set(DTLS_ROLE, SERVER_ONLY);
        serverCoapConfig.setTransient(DtlsConfig.DTLS_CONNECTION_ID_LENGTH);

        if (config.getDtlsCidLength() != null) {
            setDtlsConnectorConfigCidLength(serverCoapConfig, config.getDtlsCidLength());
        }

        /* Create DTLS Config */
        this.setServerWithCredentials(builder);
        // Set Californium Configuration
        endpointsBuilder.setConfiguration(serverCoapConfig);


        // Create CoAP endpoint
        InetSocketAddress coapAddr = new InetSocketAddress(config.getHost(), config.getPort());
        endpointsBuilder.addEndpoint(coapAddr, Protocol.COAP);

        // Create CoAP over DTLS endpoint
        InetSocketAddress coapsAddr = new InetSocketAddress(config.getSecureHost(), config.getSecurePort());
        endpointsBuilder.addEndpoint(coapsAddr, Protocol.COAPS);


        builder.setDecoder(new DefaultLwM2mDecoder(true));
        builder.setEncoder(new DefaultLwM2mEncoder(true));

        // Create LWM2M server
        builder.setEndpointsProviders(endpointsBuilder.build());
        LeshanServer leshanServer = builder.build();
        CoapServer coapServer = ((CaliforniumServerEndpointsProvider) (leshanServer.getEndpointsProvider()).toArray()[0]).getCoapServer();
        if (coapServer != null) {
            CoapResource root = (CoapResource) coapServer.getRoot();
            if (root == null) {
                root = new CoapResource("");
                coapServer.add(root);
            }
            root.add(new LwM2mTransportCoapResource(otaPackageDataCache, FIRMWARE_UPDATE_COAP_RESOURCE));
            root.add(new LwM2mTransportCoapResource(otaPackageDataCache, SOFTWARE_UPDATE_COAP_RESOURCE));
        }
        return leshanServer;
    }

    private void setServerWithCredentials(LeshanServerBuilder builder) {
        if (this.config.getSslCredentials() != null) {
            SslCredentials sslCredentials = this.config.getSslCredentials();
            builder.setPublicKey(sslCredentials.getPublicKey());
            builder.setPrivateKey(sslCredentials.getPrivateKey());
            builder.setCertificateChain(sslCredentials.getCertificateChain());
            builder.setAuthorizer(authorizer);
        } else {
            /* by default trust all */
            builder.setTrustedCertificates(new X509Certificate[0]);
        }
    }

    private synchronized void recreateLwM2mServer() {
        LeshanServer oldServer = this.server;
        LwM2mServerListener oldListener = this.serverListener;

        log.info("Creating new LwM2M server with updated certificates...");
        LeshanServer newServer = getLhServer();

        // Only cycle the endpoint providers (CoAP/DTLS). The RegistrationStore and SecurityStore are
        // Spring singletons shared with newServer — calling oldServer.stop()/destroy() would propagate
        // to them (LeshanServer.stop/destroy propagate to Stoppable/Destroyable stores), which would
        // shut down the shared schedulers (TbInMemoryRegistrationStore.destroy calls schedExecutor.shutdownNow),
        // killing newServer's cleaner tasks. Leaving the stores running preserves existing device
        // registrations across the swap — clients only need to re-establish DTLS on next uplink.
        if (oldServer != null) {
            log.info("Stopping old LwM2M endpoints to release ports...");
            if (oldListener != null) {
                oldServer.getRegistrationService().removeListener(oldListener.registrationListener);
                oldServer.getPresenceService().removeListener(oldListener.presenceListener);
                oldServer.getObservationService().removeListener(oldListener.observationListener);
                oldServer.getSendService().removeListener(oldListener.sendListener);
            }
            stopEndpoints(oldServer);
        }

        try {
            newServer.start();
        } catch (Exception e) {
            log.error("Failed to start new LwM2M server", e);
            destroyEndpoints(newServer);
            // Attempt to restart the old endpoints (shared stores are still running).
            if (oldServer != null) {
                try {
                    startEndpoints(oldServer);
                    if (oldListener != null) {
                        oldServer.getRegistrationService().addListener(oldListener.registrationListener);
                        oldServer.getPresenceService().addListener(oldListener.presenceListener);
                        oldServer.getObservationService().addListener(oldListener.observationListener);
                        oldServer.getSendService().addListener(oldListener.sendListener);
                    }
                    log.info("Restored old LwM2M endpoints successfully.");
                } catch (Exception restoreEx) {
                    log.error("Failed to restore old LwM2M endpoints", restoreEx);
                }
            }
            throw e;
        }

        LwM2mServerListener newListener = new LwM2mServerListener(handler);
        newServer.getRegistrationService().addListener(newListener.registrationListener);
        newServer.getPresenceService().addListener(newListener.presenceListener);
        newServer.getObservationService().addListener(newListener.observationListener);
        newServer.getSendService().addListener(newListener.sendListener);

        this.server = newServer;
        this.context.setServer(newServer);
        this.serverListener = newListener;
        log.info("New LwM2M server started with refreshed certificates. Existing device registrations preserved; clients will re-establish DTLS on next uplink.");

        // Destroy old endpoints only — leave the shared stores alone.
        if (oldServer != null) {
            destroyEndpoints(oldServer);
        }
    }

    private void stopEndpoints(LeshanServer server) {
        server.getEndpointsProvider().forEach(LwM2mServerEndpointsProvider::stop);
    }

    private void startEndpoints(LeshanServer server) {
        server.getEndpointsProvider().forEach(LwM2mServerEndpointsProvider::start);
    }

    private void destroyEndpoints(LeshanServer server) {
        server.getEndpointsProvider().forEach(LwM2mServerEndpointsProvider::destroy);
    }

    @Override
    public String getName() {
        return DataConstants.LWM2M_TRANSPORT_NAME;
    }

}
