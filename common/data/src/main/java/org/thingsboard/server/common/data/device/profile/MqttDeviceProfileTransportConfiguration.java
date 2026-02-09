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
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Objects;
import java.util.Set;

@Data
public class MqttDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    @Schema
    @NoXss
    private String deviceTelemetryTopic = MqttTopics.DEVICE_TELEMETRY_TOPIC;
    @Schema
    @NoXss
    private String deviceAttributesTopic = MqttTopics.DEVICE_ATTRIBUTES_TOPIC;
    @Schema
    @NoXss
    private String deviceAttributesSubscribeTopic = MqttTopics.DEVICE_ATTRIBUTES_TOPIC;

    @Schema
    private TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
    @Schema
    private boolean sparkplug;
    @ArraySchema(schema = @Schema(implementation = String.class))
    private Set<String> sparkplugAttributesMetricNames;
    @Schema
    private boolean sendAckOnValidationException;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of the device transport")
    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.MQTT;
    }

    public TransportPayloadTypeConfiguration getTransportPayloadTypeConfiguration() {
        return Objects.requireNonNullElseGet(transportPayloadTypeConfiguration, JsonTransportPayloadConfiguration::new);
    }

    public String getDeviceTelemetryTopic() {
        return StringUtils.notBlankOrDefault(deviceTelemetryTopic, MqttTopics.DEVICE_TELEMETRY_TOPIC);
    }

    public String getDeviceAttributesTopic() {
        return StringUtils.notBlankOrDefault(deviceAttributesTopic, MqttTopics.DEVICE_ATTRIBUTES_TOPIC);
    }

    public String getDeviceAttributesSubscribeTopic() {
        return StringUtils.notBlankOrDefault(deviceAttributesSubscribeTopic, MqttTopics.DEVICE_ATTRIBUTES_TOPIC);
    }

}
