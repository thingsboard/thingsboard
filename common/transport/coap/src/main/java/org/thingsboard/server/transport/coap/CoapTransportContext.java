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
