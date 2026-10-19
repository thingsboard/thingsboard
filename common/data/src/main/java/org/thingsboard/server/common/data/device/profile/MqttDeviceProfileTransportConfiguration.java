// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Objects;
import java.util.Set;

@Schema
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
