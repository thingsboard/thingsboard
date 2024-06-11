/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.CertPemCredentials;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;

public class TbAzureIotHubNodeTest extends TbMqttNodeTest {

    private TbAzureIotHubNode azureIotHubNode;
    private TbAzureIotHubNodeConfiguration azureIotHubNodeConfig;

    @BeforeEach
    public void setUp() {
        super.setUp();
        azureIotHubNode = spy(new TbAzureIotHubNode());
        azureIotHubNodeConfig = new TbAzureIotHubNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(azureIotHubNodeConfig.getTopicPattern()).isEqualTo("devices/<device_id>/messages/events/");
        assertThat(azureIotHubNodeConfig.getHost()).isEqualTo("<iot-hub-name>.azure-devices.net");
        assertThat(azureIotHubNodeConfig.getPort()).isEqualTo(8883);
        assertThat(azureIotHubNodeConfig.getConnectTimeoutSec()).isEqualTo(10);
        assertThat(azureIotHubNodeConfig.isCleanSession()).isTrue();
        assertThat(azureIotHubNodeConfig.isSsl()).isTrue();
        assertThat(azureIotHubNodeConfig.getCredentials()).isInstanceOf(AzureIotHubSasCredentials.class);
    }

    @Test
    public void verifyPrepareMqttClientConfigMethodWithAzureIotHubSasCredentials() throws TbNodeException {
        AzureIotHubSasCredentials credentials = new AzureIotHubSasCredentials();
        credentials.setSasKey("testSasKey");
        credentials.setCaCert("test-ca-cert.pem");
        azureIotHubNodeConfig.setCredentials(credentials);
        TbNodeConfiguration configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(azureIotHubNodeConfig));
        mqttNodeConfig = TbNodeUtils.convert(configuration, TbMqttNodeConfiguration.class);
        ReflectionTestUtils.setField(azureIotHubNode, "mqttNodeConfiguration", mqttNodeConfig);
        MqttClientConfig mqttClientConfig = new MqttClientConfig();

        azureIotHubNode.prepareMqttClientConfig(mqttClientConfig);

        assertThat(mqttClientConfig)
                .hasFieldOrPropertyWithValue("protocolVersion", MqttVersion.MQTT_3_1_1)
                .hasFieldOrPropertyWithValue("username", AzureIotHubUtil.buildUsername(mqttNodeConfig.getHost(), mqttClientConfig.getClientId()))
                .hasFieldOrPropertyWithValue("password", AzureIotHubUtil.buildSasToken(mqttNodeConfig.getHost(), credentials.getSasKey()));
    }

    @Test
    public void givenPemCredentialsAndSuccessfulInitClient_whenInit_thenOk() throws Exception {
        CertPemCredentials credentials = new CertPemCredentials();
        credentials.setCaCert("test-ca-cert.pem");
        credentials.setPassword("test-password");
        azureIotHubNodeConfig.setCredentials(credentials);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(azureIotHubNodeConfig));

        willReturn(mqttClientMock).given(azureIotHubNode).initClient(any());

        azureIotHubNode.init(ctxMock, configuration);

        assertThat(azureIotHubNode.getMqttNodeConfiguration())
                .hasFieldOrPropertyWithValue("port", 8883)
                .hasFieldOrPropertyWithValue("cleanSession", true);
    }

    @Test
    public void givenAzureIotHubSasCredentialsAndFailedInitClient_whenInit_thenThrowsException() throws Exception {
        AzureIotHubSasCredentials credentials = new AzureIotHubSasCredentials();
        credentials.setSasKey("testSasKey");
        credentials.setCaCert("test-ca-cert.pem");
        azureIotHubNodeConfig.setCredentials(credentials);

        String errorMsg = "Failed to connect to MQTT broker!";
        willThrow(new RuntimeException(errorMsg)).given(azureIotHubNode).initClient(any());

        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(azureIotHubNodeConfig));
        assertThatThrownBy(() -> azureIotHubNode.init(ctxMock, configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage(RuntimeException.class.getName() + ": " + errorMsg);
    }
}
