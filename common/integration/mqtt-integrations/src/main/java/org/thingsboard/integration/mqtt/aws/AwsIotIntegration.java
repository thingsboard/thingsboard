// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt.aws;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.mqtt.MqttClientConfiguration;
import org.thingsboard.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.integration.mqtt.credentials.CertPemClientCredentials;
import org.thingsboard.integration.mqtt.credentials.MqttClientCredentials;
import org.thingsboard.server.common.data.StringUtils;

@Slf4j
public class AwsIotIntegration extends BasicMqttIntegration {

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
    }

    @Override
    protected void setupConfiguration(MqttClientConfiguration mqttClientConfiguration) {
        mqttClientConfiguration.setPort(8883);
        mqttClientConfiguration.setCleanSession(true);
        MqttClientCredentials credentials = mqttClientConfiguration.getCredentials();
        if (!(credentials instanceof CertPemClientCredentials certPemClientCredentials)) {
            throw new RuntimeException("Can't setup AWS IoT integration without AWS IoT Certificates!");
        }
        if (StringUtils.isEmpty(certPemClientCredentials.getCaCert()) ||
                StringUtils.isEmpty(certPemClientCredentials.getCert()) ||
                StringUtils.isEmpty(certPemClientCredentials.getPrivateKey())) {
            throw new RuntimeException("Can't setup AWS IoT integration. Required AWS IoT Certificates or Private Key is missing!");
        }
    }

}
