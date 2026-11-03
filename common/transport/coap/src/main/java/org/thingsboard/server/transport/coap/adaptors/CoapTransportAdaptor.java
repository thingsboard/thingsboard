// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.adaptors;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;

import java.util.UUID;

public interface CoapTransportAdaptor {

    TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound, Descriptors.Descriptor telemetryMsgDescriptor) throws AdaptorException;

    TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound, Descriptors.Descriptor attributesMsgDescriptor) throws AdaptorException;

    TransportProtos.GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound, Descriptors.Descriptor rpcResponseMsgDescriptor) throws AdaptorException;

    TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.ClaimDeviceMsg convertToClaimDevice(UUID sessionId, Request inbound, TransportProtos.SessionInfoProto sessionInfo) throws AdaptorException;

    Response convertToPublish(TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException;

    Response convertToPublish(TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException;

    Response convertToPublish(TransportProtos.ToDeviceRpcRequestMsg rpcRequest, DynamicMessage.Builder rpcRequestDynamicMessageBuilder) throws AdaptorException;

    Response convertToPublish(TransportProtos.ToServerRpcResponseMsg msg) throws AdaptorException;

    ProvisionDeviceRequestMsg convertToProvisionRequestMsg(UUID sessionId, Request inbound) throws AdaptorException;

    int getContentFormat();

}
