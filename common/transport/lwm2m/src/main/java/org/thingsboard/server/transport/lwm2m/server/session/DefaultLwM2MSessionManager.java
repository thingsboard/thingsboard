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
package org.thingsboard.server.transport.lwm2m.server.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.LwM2mSessionMsgListener;
import org.thingsboard.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.thingsboard.server.transport.lwm2m.server.rpc.LwM2MRpcRequestHandler;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_OPEN;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_RPC_ASYNC_MSG;

@Slf4j
@Service
@TbLwM2mTransportComponent
public class DefaultLwM2MSessionManager implements LwM2MSessionManager {

    private final TransportService transportService;
    private final LwM2MAttributesService attributesService;
    private final LwM2MRpcRequestHandler rpcHandler;
    private final LwM2mUplinkMsgHandler uplinkHandler;

    public DefaultLwM2MSessionManager(TransportService transportService,
                                      @Lazy LwM2MAttributesService attributesService,
                                      @Lazy LwM2MRpcRequestHandler rpcHandler,
                                      @Lazy LwM2mUplinkMsgHandler uplinkHandler) {
        this.transportService = transportService;
        this.attributesService = attributesService;
        this.rpcHandler = rpcHandler;
        this.uplinkHandler = uplinkHandler;
    }

    @Override
    public void register(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(uplinkHandler, attributesService, rpcHandler, sessionInfo, transportService));
        TransportProtos.TransportToDeviceActorMsg msg = TransportProtos.TransportToDeviceActorMsg.newBuilder()
                .setSessionInfo(sessionInfo)
                .setSessionEvent(SESSION_EVENT_MSG_OPEN)
                .setSubscribeToAttributes(SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG)
                .setSubscribeToRPC(SUBSCRIBE_TO_RPC_ASYNC_MSG)
                .build();
        transportService.process(msg, null);
    }

    @Override
    public void deregister(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
        transportService.deregisterSession(sessionInfo);
    }
}
