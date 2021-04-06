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
package org.thingsboard.server.transport.coap.adaptors;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.adaptor.ProtoConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.CoapTransportResource;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class ProtoCoapAdaptor implements CoapTransportAdaptor {

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound, Descriptors.Descriptor telemetryMsgDescriptor) throws AdaptorException {
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(dynamicMsgToJson(inbound.getPayload(), telemetryMsgDescriptor)));
        } catch (Exception e) {
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound, Descriptors.Descriptor attributesMsgDescriptor) throws AdaptorException {
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(dynamicMsgToJson(inbound.getPayload(), attributesMsgDescriptor)));
        } catch (Exception e) {
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException {
        return CoapAdaptorUtils.toGetAttributeRequestMsg(inbound);
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound, Descriptors.Descriptor rpcResponseMsgDescriptor) throws AdaptorException {
        Optional<Integer> requestId = CoapTransportResource.getRequestId(inbound);
        if (requestId.isEmpty()) {
            throw new AdaptorException("Request id is missing!");
        } else {
            try {
                JsonElement response = new JsonParser().parse(dynamicMsgToJson(inbound.getPayload(), rpcResponseMsgDescriptor));
                return TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId.orElseThrow(() -> new AdaptorException("Request id is missing!")))
                        .setPayload(response.toString()).build();
            } catch (Exception e) {
                throw new AdaptorException(e);
            }
        }
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException {
        try {
            return ProtoConverter.convertToServerRpcRequest(inbound.getPayload(), 0);
        } catch (InvalidProtocolBufferException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(UUID sessionId, Request inbound, TransportProtos.SessionInfoProto sessionInfo) throws AdaptorException {
        DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        try {
            return ProtoConverter.convertToClaimDeviceProto(deviceId, inbound.getPayload());
        } catch (InvalidProtocolBufferException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(UUID sessionId, Request inbound) throws AdaptorException {
        try {
            return ProtoConverter.convertToProvisionRequestMsg(inbound.getPayload());
        } catch (InvalidProtocolBufferException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public Response convertToPublish(boolean isConfirmable, TransportProtos.AttributeUpdateNotificationMsg msg) throws AdaptorException {
        return getObserveNotification(isConfirmable, msg.toByteArray());
    }

    @Override
    public Response convertToPublish(boolean isConfirmable, TransportProtos.ToDeviceRpcRequestMsg msg) throws AdaptorException {
        return getObserveNotification(isConfirmable, msg.toByteArray());
    }

    @Override
    public Response convertToPublish(TransportProtos.ToServerRpcResponseMsg msg) throws AdaptorException {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.setPayload(msg.toByteArray());
        return response;
    }

    @Override
    public Response convertToPublish(TransportProtos.GetAttributeResponseMsg msg) throws AdaptorException {
        if (msg.getClientAttributeListCount() == 0 && msg.getSharedAttributeListCount() == 0) {
            return new Response(CoAP.ResponseCode.NOT_FOUND);
        } else {
            Response response = new Response(CoAP.ResponseCode.CONTENT);
            response.setPayload(msg.toByteArray());
            return response;
        }
    }

    private Response getObserveNotification(boolean confirmable, byte[] notification) {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.setPayload(notification);
        response.setConfirmable(confirmable);
        return response;
    }

    private String dynamicMsgToJson(byte[] bytes, Descriptors.Descriptor descriptor) throws InvalidProtocolBufferException {
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(descriptor, bytes);
        return JsonFormat.printer().includingDefaultValueFields().print(dynamicMessage);
    }

}
