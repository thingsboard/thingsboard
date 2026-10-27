// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import lombok.Data;

@Data
public class TransportConfigurationContainer {

    private boolean jsonPayload;
    private Descriptors.Descriptor telemetryMsgDescriptor;
    private Descriptors.Descriptor attributesMsgDescriptor;
    private Descriptors.Descriptor rpcResponseMsgDescriptor;
    private DynamicMessage.Builder rpcRequestDynamicMessageBuilder;

    public TransportConfigurationContainer(boolean jsonPayload, Descriptors.Descriptor telemetryMsgDescriptor, Descriptors.Descriptor attributesMsgDescriptor, Descriptors.Descriptor rpcResponseMsgDescriptor, DynamicMessage.Builder rpcRequestDynamicMessageBuilder) {
        this.jsonPayload = jsonPayload;
        this.telemetryMsgDescriptor = telemetryMsgDescriptor;
        this.attributesMsgDescriptor = attributesMsgDescriptor;
        this.rpcResponseMsgDescriptor = rpcResponseMsgDescriptor;
        this.rpcRequestDynamicMessageBuilder = rpcRequestDynamicMessageBuilder;
    }

    public TransportConfigurationContainer(boolean jsonPayload) {
        this.jsonPayload = jsonPayload;
    }
}
