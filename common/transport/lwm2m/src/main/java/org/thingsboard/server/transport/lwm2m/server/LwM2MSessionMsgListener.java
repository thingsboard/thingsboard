/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportUpdateCredentialsProto;

import java.util.UUID;

@Slf4j
public class LwM2MSessionMsgListener implements SessionMsgListener {
    private LwM2MTransportService service;

    LwM2MSessionMsgListener(UUID sessionId, LwM2MTransportService service) {
        this.service = service;
    }

    @Override
    public void onGetAttributesResponse(GetAttributeResponseMsg getAttributesResponse) {
        log.info("6.1) onGetAttributesResponse listener");
    }

    @Override
    public void onAttributeUpdate(AttributeUpdateNotificationMsg attributeUpdateNotification) {
        log.info("6.2) onAttributeUpdate listener");
    }

    @Override
    public void onRemoteSessionCloseCommand(SessionCloseNotificationProto sessionCloseNotification) {
        log.info("6.3) onAttributeUpdate nRemoteSessionCloseCommand");
    }

    @Override
    public void onToTransportUpdateCredentials(ToTransportUpdateCredentialsProto updateCredentials) {
        this.service.onGetChangeCredentials(updateCredentials);
    }

    @Override
    public void onToDeviceRpcRequest(ToDeviceRpcRequestMsg toDeviceRequest) {
        log.info("6.5)  onToDeviceRpcRequest listener");
    }

    @Override
    public void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse) {
        log.info("6.6)  onToServerRpcResponse");
    }



}
