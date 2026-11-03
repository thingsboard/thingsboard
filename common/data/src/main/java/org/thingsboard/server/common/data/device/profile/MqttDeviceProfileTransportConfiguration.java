// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Objects;
import java.util.Set;

@Data
public class MqttDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    @NoXss
    private String deviceTelemetryTopic = MqttTopics.DEVICE_TELEMETRY_TOPIC;
    @NoXss
    private String deviceAttributesTopic = MqttTopics.DEVICE_ATTRIBUTES_TOPIC;
    @NoXss
    private String deviceAttributesSubscribeTopic = MqttTopics.DEVICE_ATTRIBUTES_TOPIC;

    private TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
    private boolean sparkplug;
    private Set<String> sparkplugAttributesMetricNames;
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
