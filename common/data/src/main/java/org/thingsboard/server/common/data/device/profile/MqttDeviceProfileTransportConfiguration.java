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
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.DeviceTransportType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "transportPayloadType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MqttJsonDeviceProfileTransportConfiguration.class, name = "JSON"),
        @JsonSubTypes.Type(value = MqttProtoDeviceProfileTransportConfiguration.class, name = "PROTOBUF")})
@JsonDeserialize(using = MqttTransportConfigurationDeserializer.class)
@Data
public abstract class MqttDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    public abstract TransportPayloadType getTransportPayloadType();

    protected String deviceTelemetryTopic = MqttTopics.DEVICE_TELEMETRY_TOPIC;
    protected String deviceAttributesTopic = MqttTopics.DEVICE_ATTRIBUTES_TOPIC;

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.MQTT;
    }

}
