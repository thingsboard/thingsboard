// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.coapserver.TbCoapTransportComponent;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.adaptors.JsonCoapAdaptor;
import org.thingsboard.server.transport.coap.adaptors.ProtoCoapAdaptor;
import org.thingsboard.server.transport.coap.client.CoapClientContext;
import org.thingsboard.server.transport.coap.efento.adaptor.EfentoCoapAdaptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by ashvayka on 18.10.18.
 */
@Slf4j
@TbCoapTransportComponent
@Component
@Getter
public class CoapTransportContext extends TransportContext {

    @Value("${transport.sessions.report_timeout}")
    private long sessionReportTimeout;

    @Getter
    @Value("${transport.coap.timeout}")
    private Long timeout;

    @Getter
    @Value("${transport.coap.piggyback_timeout}")
    private Long piggybackTimeout;

    @Getter
    @Value("${transport.coap.psm_activity_timer:10000}")
    private long psmActivityTimer;

    @Getter
    @Value("${transport.coap.paging_transmission_window:10000}")
    private long pagingTransmissionWindow;

    @Autowired
    private JsonCoapAdaptor jsonCoapAdaptor;

    @Autowired
    private ProtoCoapAdaptor protoCoapAdaptor;

    @Autowired
    private EfentoCoapAdaptor efentoCoapAdaptor;

    @Autowired
    private CoapClientContext clientContext;

    private final ConcurrentMap<Integer, TransportProtos.ToDeviceRpcRequestMsg> rpcAwaitingAck = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final AtomicInteger connectionsActiveCounterCOAP = new AtomicInteger();
    @Getter(AccessLevel.NONE)
    private final AtomicInteger connectionsActiveCounterCOAPS = new AtomicInteger();
    @Getter(AccessLevel.NONE)
    private DefaultCounter requestsCOAP;
    @Getter(AccessLevel.NONE)
    private DefaultCounter requestsCOAPS;

    @PostConstruct
    public void init() {
        super.init();
        transportService.createGaugeStats("connections_active", connectionsActiveCounterCOAP, "protocol", "COAP");
        transportService.createGaugeStats("connections_active", connectionsActiveCounterCOAPS, "protocol", "COAPS");
        requestsCOAP = transportService.createCounterStats("requests", "protocol", "COAP", "tls_termination", "none");
        requestsCOAPS = transportService.createCounterStats("requests", "protocol", "COAPS", "tls_termination", "transport");
    }

    public void onRequest(boolean isDtls) {
        if (isDtls) {
            requestsCOAPS.increment();
        } else {
            requestsCOAP.increment();
        }
    }

    /**
     * Active sessions are the clients that keep an observe (attributes or RPC) subscription.
     */
    public void updateActiveSessions(int coap, int coaps) {
        connectionsActiveCounterCOAP.set(coap);
        connectionsActiveCounterCOAPS.set(coaps);
    }

}
