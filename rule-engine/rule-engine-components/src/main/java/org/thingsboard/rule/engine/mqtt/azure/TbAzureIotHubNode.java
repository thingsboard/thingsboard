/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.rule.engine.mqtt.azure;

import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.mqtt.TbMqttNode;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;
import org.thingsboard.rule.engine.mqtt.credentials.CertPemClientCredentials;
import org.thingsboard.rule.engine.mqtt.credentials.MqttClientCredentials;
import org.thingsboard.server.common.data.plugin.ComponentType;

import java.util.Optional;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "azure iot hub",
        configClazz = TbAzureIotHubNodeConfiguration.class,
        nodeDescription = "Publish messages to the Azure IoT Hub",
        nodeDetails = "Will publish message payload to the Azure IoT Hub with QoS <b>AT_LEAST_ONCE</b>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAzureIotHubConfig"
)
public class TbAzureIotHubNode extends TbMqttNode {
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        try {
            this.mqttNodeConfiguration = TbNodeUtils.convert(configuration, TbMqttNodeConfiguration.class);
            mqttNodeConfiguration.setPort(8883);
            mqttNodeConfiguration.setCleanSession(true);
            MqttClientCredentials credentials = mqttNodeConfiguration.getCredentials();
            mqttNodeConfiguration.setCredentials(new MqttClientCredentials() {
                @Override
                public Optional<SslContext> initSslContext() {
                    if (credentials instanceof AzureIotHubSasCredentials) {
                        AzureIotHubSasCredentials sasCredentials = (AzureIotHubSasCredentials) credentials;
                        if (sasCredentials.getCaCert() == null || sasCredentials.getCaCert().isEmpty()) {
                            sasCredentials.setCaCert(AzureIotHubUtil.getDefaultCaCert());
                        }
                    } else if (credentials instanceof CertPemClientCredentials) {
                        CertPemClientCredentials pemCredentials = (CertPemClientCredentials) credentials;
                        if (pemCredentials.getCaCert() == null || pemCredentials.getCaCert().isEmpty()) {
                            pemCredentials.setCaCert(AzureIotHubUtil.getDefaultCaCert());
                        }
                    }
                    return credentials.initSslContext();
                }

                @Override
                public void configure(MqttClientConfig config) {
                    config.setProtocolVersion(MqttVersion.MQTT_3_1_1);
                    config.setUsername(AzureIotHubUtil.buildUsername(mqttNodeConfiguration.getHost(), config.getClientId()));
                    if (credentials instanceof AzureIotHubSasCredentials) {
                        AzureIotHubSasCredentials sasCredentials = (AzureIotHubSasCredentials) credentials;
                        config.setPassword(AzureIotHubUtil.buildSasToken(mqttNodeConfiguration.getHost(), sasCredentials.getSasKey()));
                    }
                }
            });

            this.mqttClient = initClient(ctx);
        } catch (Exception e) {
            throw new TbNodeException(e);
        }    }
}
