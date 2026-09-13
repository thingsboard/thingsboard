// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.mqtt.azure;

import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbAzureIotHubNodeConfiguration extends TbMqttNodeConfiguration {

    @Override
    public TbAzureIotHubNodeConfiguration defaultConfiguration() {
        TbAzureIotHubNodeConfiguration configuration = new TbAzureIotHubNodeConfiguration();
        configuration.setTopicPattern("devices/<device_id>/messages/events/");
        configuration.setHost("<iot-hub-name>.azure-devices.net");
        configuration.setPort(8883);
        configuration.setConnectTimeoutSec(10);
        configuration.setCleanSession(true);
        configuration.setSsl(true);
        configuration.setProtocolVersion(MqttVersion.MQTT_3_1_1);
        configuration.setCredentials(new AzureIotHubSasCredentials());
        return configuration;
    }

}
