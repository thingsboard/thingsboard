// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.coapserver.TbCoapTransportComponent;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.coap.efento.CoapEfentoTransportResource;

import java.net.UnknownHostException;

@Service("CoapTransportService")
@TbCoapTransportComponent
@Slf4j
public class CoapTransportService implements TbTransportService {

    private static final String V1 = "v1";
    private static final String API = "api";
    private static final String EFENTO = "efento";
    public static final String MEASUREMENTS = "m";
    public static final String DEVICE_INFO = "i";
    public static final String CONFIGURATION = "c";
    public static final String CURRENT_TIMESTAMP = "t";

    @Autowired
    private CoapServerService coapServerService;

    @Autowired
    private CoapTransportContext coapTransportContext;

    private CoapServer coapServer;

    @PostConstruct
    public void init() throws UnknownHostException {
        log.info("Starting CoAP transport...");
        coapServer = coapServerService.getCoapServer();
        CoapResource api = new CoapResource(API);
        api.add(new CoapTransportResource(coapTransportContext, coapServerService, V1));

        CoapEfentoTransportResource efento = new CoapEfentoTransportResource(coapTransportContext, EFENTO);
        efento.add(new CoapResource(MEASUREMENTS));
        efento.add(new CoapResource(DEVICE_INFO));
        efento.add(new CoapResource(CONFIGURATION));
        efento.add(new CoapResource(CURRENT_TIMESTAMP));
        coapServer.add(api);
        coapServer.add(efento);
        coapServer.add(new OtaPackageTransportResource(coapTransportContext, OtaPackageType.FIRMWARE));
        coapServer.add(new OtaPackageTransportResource(coapTransportContext, OtaPackageType.SOFTWARE));
        log.info("CoAP transport started!");
    }

    @PreDestroy
    public void shutdown() {
        log.info("CoAP transport stopped!");
    }

    @Override
    public String getName() {
        return DataConstants.COAP_TRANSPORT_NAME;
    }
}
