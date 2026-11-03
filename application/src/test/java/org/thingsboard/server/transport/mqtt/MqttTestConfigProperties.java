// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;

import java.util.Set;

@Data
@Builder
public class MqttTestConfigProperties {

    String deviceName;
    String gatewayName;
    boolean isSparkplug;
    Set<String> sparkplugAttributesMetricNames;

    TransportPayloadType transportPayloadType;

    String telemetryTopicFilter;
    String attributesTopicFilter;

    String telemetryProtoSchema;
    String attributesProtoSchema;
    String rpcResponseProtoSchema;
    String rpcRequestProtoSchema;

    boolean enableCompatibilityWithJsonPayloadFormat;
    boolean useJsonPayloadFormatForDefaultDownlinkTopics;
    boolean sendAckOnValidationException;

    DeviceProfileProvisionType provisionType;
    String provisionKey;
    String provisionSecret;

}
