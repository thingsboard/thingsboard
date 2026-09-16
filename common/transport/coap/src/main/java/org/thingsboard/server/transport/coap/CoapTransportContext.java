// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.coapserver.TbCoapTransportComponent;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.adaptors.JsonCoapAdaptor;
import org.thingsboard.server.transport.coap.adaptors.ProtoCoapAdaptor;
import org.thingsboard.server.transport.coap.client.CoapClientContext;
import org.thingsboard.server.transport.coap.efento.adaptor.EfentoCoapAdaptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


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

}
