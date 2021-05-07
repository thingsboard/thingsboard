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
package org.thingsboard.server.coapserver;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@TbCoapServerComponent
public class DefaultCoapServerService implements CoapServerService {

    @Autowired
    private CoapServerContext coapServerContext;

    private CoapServer server;

    private TbCoapDtlsCertificateVerifier tbDtlsCertificateVerifier;

    private ScheduledExecutorService dtlsSessionsExecutor;

    @PostConstruct
    public void init() throws Exception {
        createCoapServer();
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
    public CoapServer getCoapServer() throws Exception {
        if (server != null) {
            return server;
        } else {
            return createCoapServer();
        }
    }

    @Override
    public ConcurrentMap<String, TbCoapDtlsSessionInfo> getDtlsSessionsMap() {
        return tbDtlsCertificateVerifier != null ? tbDtlsCertificateVerifier.getTbCoapDtlsSessionIdsMap() : null;
    }

    @Override
    public long getTimeout() {
        return coapServerContext.getTimeout();
    }

    private CoapServer createCoapServer() throws Exception {
        if (!isAnySecurityModeEnabled()) {
            throw new RuntimeException("Failed to initialize CoAP Server. Invalid CoAP security mode!");
        }
        server = new CoapServer();

        if (isNoSecureModeEnabled()) {
            TbCoapNoSecureSettings noSecureSettings = coapServerContext.getNoSecureSettings();
            CoapEndpoint noSecureCoapEndpoint = noSecureSettings.getNoSecureCoapEndpoint();
            server.addEndpoint(noSecureCoapEndpoint);
        }

        if (isDtlsModeEnabled()) {
            CoapEndpoint.Builder dtlsCoapEndpointBuilder = new CoapEndpoint.Builder();
            TbCoapDtlsSettings dtlsSettings = coapServerContext.getDtlsSettings();
            DtlsConnectorConfig dtlsConnectorConfig = dtlsSettings.dtlsConnectorConfig();
            DTLSConnector connector = new DTLSConnector(dtlsConnectorConfig);
            dtlsCoapEndpointBuilder.setConnector(connector);
            CoapEndpoint dtlsCoapEndpoint = dtlsCoapEndpointBuilder.build();
            server.addEndpoint(dtlsCoapEndpoint);
            if (dtlsConnectorConfig.isClientAuthenticationRequired()) {
                tbDtlsCertificateVerifier = (TbCoapDtlsCertificateVerifier) dtlsConnectorConfig.getAdvancedCertificateVerifier();
                dtlsSessionsExecutor = Executors.newSingleThreadScheduledExecutor();
                dtlsSessionsExecutor.scheduleAtFixedRate(this::evictTimeoutSessions, new Random().nextInt((int) getDtlsSessionReportTimeout()), getDtlsSessionReportTimeout(), TimeUnit.MILLISECONDS);
            }
        }
        Resource root = server.getRoot();
        TbCoapServerMessageDeliverer messageDeliverer = new TbCoapServerMessageDeliverer(root);
        server.setMessageDeliverer(messageDeliverer);

        server.start();
        return server;

    }

    private boolean isNoSecureModeEnabled() {
        return coapServerContext.getNoSecureSettings() != null;
    }

    private boolean isDtlsModeEnabled() {
        return coapServerContext.getDtlsSettings() != null;
    }

    private boolean isAnySecurityModeEnabled() {
        return isNoSecureModeEnabled() || isDtlsModeEnabled();
    }

    private void evictTimeoutSessions() {
        tbDtlsCertificateVerifier.evictTimeoutSessions();
    }

    private long getDtlsSessionReportTimeout() {
        return tbDtlsCertificateVerifier.getDtlsSessionReportTimeout();
    }

}
