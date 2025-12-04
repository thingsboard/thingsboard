/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.eclipse.californium.core.config.CoapConfig.DEFAULT_BLOCKWISE_STATUS_LIFETIME_IN_SECONDS;

@Slf4j
@Component
@TbCoapServerComponent
public class DefaultCoapServerService implements CoapServerService, SmartInitializingSingleton {

    @Autowired
    private CoapServerContext coapServerContext;

    private CoapServer server;

    private TbCoapDtlsCertificateVerifier tbDtlsCertificateVerifier;

    private ScheduledExecutorService dtlsSessionsExecutor;

    private DTLSConnector dtlsConnector;

    private CoapEndpoint dtlsCoapEndpoint;

    @PostConstruct
    public void init() throws UnknownHostException {
        createCoapServer();
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (isDtlsEnabled()) {
            coapServerContext.getDtlsSettings().registerReloadCallback(() -> {
                try {
                    log.info("CoAP DTLS certificates reloaded. Recreating DTLS endpoint...");
                    recreateDtlsEndpoint();
                    log.info("CoAP DTLS endpoint recreated successfully with new certificates.");
                } catch (Exception e) {
                    log.error("Failed to recreate CoAP DTLS endpoint after certificate reload", e);
                }
            });
        }
    }

    @PreDestroy
    public void shutdown() {
        if (dtlsSessionsExecutor != null) {
            dtlsSessionsExecutor.shutdownNow();
        }
        log.info("Stopping CoAP server!");
        server.destroy();
        log.info("CoAP server stopped!");
    }

    @Override
    public CoapServer getCoapServer() throws UnknownHostException {
        if (server != null) {
            return server;
        } else {
            return createCoapServer();
        }
    }

    @Override
    public ConcurrentMap<TbCoapDtlsSessionKey, TbCoapDtlsSessionInfo> getDtlsSessionsMap() {
        return tbDtlsCertificateVerifier != null ? tbDtlsCertificateVerifier.getTbCoapDtlsSessionsMap() : null;
    }

    private CoapServer createCoapServer() throws UnknownHostException {
        Configuration networkConfig = createNetworkConfiguration();
        server = new CoapServer(networkConfig);

        CoapEndpoint.Builder noSecCoapEndpointBuilder = new CoapEndpoint.Builder();
        InetAddress addr = InetAddress.getByName(coapServerContext.getHost());
        InetSocketAddress sockAddr = new InetSocketAddress(addr, coapServerContext.getPort());
        noSecCoapEndpointBuilder.setInetSocketAddress(sockAddr);

        noSecCoapEndpointBuilder.setConfiguration(networkConfig);
        CoapEndpoint noSecCoapEndpoint = noSecCoapEndpointBuilder.build();
        server.addEndpoint(noSecCoapEndpoint);
        if (isDtlsEnabled()) {
            createDtlsEndpoint(networkConfig);
            dtlsSessionsExecutor = ThingsBoardExecutors.newSingleThreadScheduledExecutor(getClass().getSimpleName());
            dtlsSessionsExecutor.scheduleAtFixedRate(this::evictTimeoutSessions, new Random().nextInt((int) getDtlsSessionReportTimeout()), getDtlsSessionReportTimeout(), TimeUnit.MILLISECONDS);
        }
        Resource root = server.getRoot();
        TbCoapServerMessageDeliverer messageDeliverer = new TbCoapServerMessageDeliverer(root);
        server.setMessageDeliverer(messageDeliverer);

        server.start();
        return server;
    }

    private boolean isDtlsEnabled() {
        return coapServerContext.getDtlsSettings() != null;
    }

    private void evictTimeoutSessions() {
        tbDtlsCertificateVerifier.evictTimeoutSessions();
    }

    private long getDtlsSessionReportTimeout() {
        return tbDtlsCertificateVerifier.getDtlsSessionReportTimeout();
    }

    private Configuration createNetworkConfiguration() {
        Configuration networkConfig = new Configuration();
        networkConfig.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, true);
        networkConfig.set(CoapConfig.BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER, true);
        networkConfig.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, DEFAULT_BLOCKWISE_STATUS_LIFETIME_IN_SECONDS, TimeUnit.SECONDS);
        networkConfig.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 256 * 1024 * 1024);
        networkConfig.set(CoapConfig.RESPONSE_MATCHING, CoapConfig.MatcherMode.RELAXED);
        networkConfig.set(CoapConfig.PREFERRED_BLOCK_SIZE, 1024);
        networkConfig.set(CoapConfig.MAX_MESSAGE_SIZE, 1024);
        networkConfig.set(CoapConfig.MAX_RETRANSMIT, 4);
        networkConfig.set(CoapConfig.COAP_PORT, coapServerContext.getPort());
        return networkConfig;
    }

    private void createDtlsEndpoint(Configuration networkConfig) throws UnknownHostException {
        CoapEndpoint.Builder dtlsCoapEndpointBuilder = new CoapEndpoint.Builder();
        TbCoapDtlsSettings dtlsSettings = coapServerContext.getDtlsSettings();
        DtlsConnectorConfig dtlsConnectorConfig = dtlsSettings.dtlsConnectorConfig(networkConfig);
        networkConfig.set(CoapConfig.COAP_SECURE_PORT, dtlsConnectorConfig.getAddress().getPort());
        dtlsCoapEndpointBuilder.setConfiguration(networkConfig);
        dtlsConnector = new DTLSConnector(dtlsConnectorConfig);
        dtlsCoapEndpointBuilder.setConnector(dtlsConnector);
        dtlsCoapEndpoint = dtlsCoapEndpointBuilder.build();
        server.addEndpoint(dtlsCoapEndpoint);
        tbDtlsCertificateVerifier = (TbCoapDtlsCertificateVerifier) dtlsConnectorConfig.getAdvancedCertificateVerifier();
    }

    private synchronized void recreateDtlsEndpoint() throws IOException {
        if (dtlsCoapEndpoint != null) {
            log.info("Stopping old DTLS endpoint...");
            dtlsCoapEndpoint.stop();
            server.getEndpoints().remove(dtlsCoapEndpoint);
            if (dtlsConnector != null) {
                dtlsConnector.destroy();
            }
            dtlsCoapEndpoint.destroy();
            log.info("Old DTLS endpoint stopped and removed.");
        }

        Configuration networkConfig = createNetworkConfiguration();

        log.info("Creating new DTLS endpoint with updated certificates...");
        createDtlsEndpoint(networkConfig);
        dtlsCoapEndpoint.start();
        log.info("New DTLS endpoint started successfully.");
    }

}
