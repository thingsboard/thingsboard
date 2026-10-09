// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt.azure;

import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.mqtt.MqttClientConfiguration;
import org.thingsboard.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.integration.mqtt.credentials.CertPemClientCredentials;
import org.thingsboard.integration.mqtt.credentials.MqttClientCredentials;
import org.thingsboard.mqtt.MqttClientConfig;

import java.time.Clock;
import java.util.Optional;

@Slf4j
public class AzureIotHubIntegration extends BasicMqttIntegration {

    private final Clock clock = Clock.systemUTC();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
    }

    @Override
    protected void setupConfiguration(MqttClientConfiguration mqttClientConfiguration) {
        mqttClientConfiguration.setPort(8883);
        mqttClientConfiguration.setCleanSession(true);
        MqttClientCredentials credentials = mqttClientConfiguration.getCredentials();
        mqttClientConfiguration.setCredentials(new MqttClientCredentials() {
            @Override
            public Optional<SslContext> initSslContext() {
                if (credentials instanceof AzureIotHubSasCredentials sasCredentials) {
                    if (sasCredentials.getCaCert() == null || sasCredentials.getCaCert().isEmpty()) {
                        sasCredentials.setCaCert(AzureIotHubUtil.getDefaultCaCert());
                    }
                } else if (credentials instanceof CertPemClientCredentials pemCredentials) {
                    if (pemCredentials.getCaCert() == null || pemCredentials.getCaCert().isEmpty()) {
                        pemCredentials.setCaCert(AzureIotHubUtil.getDefaultCaCert());
                    }
                }
                return credentials.initSslContext();
            }

            @Override
            public void configure(MqttClientConfig config) {
                config.setUsername(AzureIotHubUtil.buildUsername(mqttClientConfiguration.getHost(), config.getClientId()));
                if (credentials instanceof AzureIotHubSasCredentials sasCredentials) {
                    config.setPassword(AzureIotHubUtil.buildSasToken(mqttClientConfiguration.getHost(), sasCredentials.getSasKey(), clock));
                }
            }
        });
    }

}
