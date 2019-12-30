/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap.adaptors;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ClaimDeviceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.transport.coap.CoapTransportResource;

import java.util.UUID;

public interface CoapTransportAdaptor {

    PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound) throws AdaptorException;

    PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound) throws AdaptorException;

    ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException;

    ClaimDeviceMsg convertToClaimDevice(UUID sessionId, Request inbound, SessionInfoProto sessionInfo) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, GetAttributeResponseMsg responseMsg) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener coapSessionListener, ToServerRpcResponseMsg msg) throws AdaptorException;

    ProvisionDeviceRequestMsg convertToProvisionRequestMsg(UUID sessionId, Request inbound) throws AdaptorException;
}
