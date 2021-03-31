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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.transport.coap.efento.CoapEfentoTransportResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service("CoapTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.coap.enabled}'=='true')")
@Slf4j
public class CoapTransportService {

    private static final String V1 = "v1";
    private static final String API = "api";
    private static final String EFENTO = "efento";
    private static final String MEASUREMENTS = "m";

    @Autowired
    private CoapTransportContext coapTransportContext;

    private TbCoapDtlsCertificateVerifier tbDtlsCertificateVerifier;

    private CoapServer server;

    private ScheduledExecutorService dtlsSessionsExecutor;

    @PostConstruct
    public void init() throws UnknownHostException {
        log.info("Starting CoAP transport...");
        log.info("Starting CoAP transport server");

        this.server = new CoapServer();

        CoapEndpoint.Builder capEndpointBuilder = new CoapEndpoint.Builder();

        if (isDtlsEnabled()) {
            TbCoapDtlsSettings dtlsSettings = coapTransportContext.getDtlsSettings();
            DtlsConnectorConfig dtlsConnectorConfig = dtlsSettings.dtlsConnectorConfig();
            DTLSConnector connector = new DTLSConnector(dtlsConnectorConfig);
            capEndpointBuilder.setConnector(connector);
            if (dtlsConnectorConfig.isClientAuthenticationRequired()) {
                tbDtlsCertificateVerifier = (TbCoapDtlsCertificateVerifier) dtlsConnectorConfig.getAdvancedCertificateVerifier();
                dtlsSessionsExecutor = Executors.newSingleThreadScheduledExecutor();
                dtlsSessionsExecutor.scheduleAtFixedRate(this::evictTimeoutSessions, new Random().nextInt((int) getDtlsSessionReportTimeout()), getDtlsSessionReportTimeout(), TimeUnit.MILLISECONDS);
            }
        } else {
            InetAddress addr = InetAddress.getByName(coapTransportContext.getHost());
            InetSocketAddress sockAddr = new InetSocketAddress(addr, coapTransportContext.getPort());
            capEndpointBuilder.setInetSocketAddress(sockAddr);
            capEndpointBuilder.setNetworkConfig(NetworkConfig.getStandard());
        }
        CoapEndpoint coapEndpoint = capEndpointBuilder.build();

        server.addEndpoint(coapEndpoint);

        createResources();
        Resource root = this.server.getRoot();
        TbCoapServerMessageDeliverer messageDeliverer = new TbCoapServerMessageDeliverer(root);
        this.server.setMessageDeliverer(messageDeliverer);

        server.start();
        log.info("CoAP transport started!");
    }

    private void createResources() {
        CoapResource api = new CoapResource(API);
        api.add(new CoapTransportResource(coapTransportContext, getDtlsSessionsMap(), V1));

        CoapResource efento = new CoapResource(EFENTO);
        CoapEfentoTransportResource efentoMeasurementsTransportResource = new CoapEfentoTransportResource(coapTransportContext, MEASUREMENTS);
        efento.add(efentoMeasurementsTransportResource);

        server.add(api);
        server.add(efento);
    }

    private boolean isDtlsEnabled() {
        return coapTransportContext.getDtlsSettings() != null;
    }

    private ConcurrentMap<String, TbCoapDtlsSessionInfo> getDtlsSessionsMap() {
        return tbDtlsCertificateVerifier != null ? tbDtlsCertificateVerifier.getTbCoapDtlsSessionIdsMap() : null;
    }

    private void evictTimeoutSessions() {
        tbDtlsCertificateVerifier.evictTimeoutSessions();
    }

    private long getDtlsSessionReportTimeout() {
        return tbDtlsCertificateVerifier.getDtlsSessionReportTimeout();
    }

    @PreDestroy
    public void shutdown() {
        if (dtlsSessionsExecutor != null) {
            dtlsSessionsExecutor.shutdownNow();
        }
        log.info("Stopping CoAP transport!");
        this.server.destroy();
        log.info("CoAP transport stopped!");
    }
}
