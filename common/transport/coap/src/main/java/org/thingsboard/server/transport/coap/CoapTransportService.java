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
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.transport.coap.efento.CoapEfentoTransportResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Service("CoapTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.coap.enabled}'=='true')")
@Slf4j
public class CoapTransportService {

    private static final String V1 = "v1";
    private static final String API = "api";
    private static final String EFENTO = "efento";
    private static final String MEASUREMENTS = "m";
    public static final String EFENTO_MEASUREMENTS = EFENTO + "/" + MEASUREMENTS;

    @Autowired
    private CoapTransportContext coapTransportContext;

    private CoapServer server;

    @PostConstruct
    public void init() throws UnknownHostException {
        log.info("Starting CoAP transport...");
        log.info("Starting CoAP transport server");

        CoapTransportRootResource rootResource = new CoapTransportRootResource(coapTransportContext, "");
        TbCoapServerMessageDeliverer messageDeliverer = new TbCoapServerMessageDeliverer(rootResource);
        this.server = new TbCoapServer(messageDeliverer);
        createResources();
        InetAddress addr = InetAddress.getByName(coapTransportContext.getHost());
        InetSocketAddress sockAddr = new InetSocketAddress(addr, coapTransportContext.getPort());
        Builder builder = new Builder();
        builder.setInetSocketAddress(sockAddr);
        CoapEndpoint coapEndpoint = builder.build();

        server.addEndpoint(coapEndpoint);
        server.start();
        log.info("CoAP transport started!");
    }

    private void createResources() {
        CoapResource api = new CoapResource(API);
        api.add(new CoapTransportResource(coapTransportContext, V1));

        CoapResource efento = new CoapResource(EFENTO);
        CoapEfentoTransportResource efentoMeasurementsTransportResource = new CoapEfentoTransportResource(coapTransportContext, MEASUREMENTS);
        efento.add(efentoMeasurementsTransportResource);

        CoapEfentoTransportResource efentoMeasurements = new CoapEfentoTransportResource(coapTransportContext, EFENTO_MEASUREMENTS);

        server.add(api);
        server.add(efento);
        server.add(efentoMeasurements);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping CoAP transport!");
        this.server.destroy();
        log.info("CoAP transport stopped!");
    }
}
