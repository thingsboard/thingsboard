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
