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
package org.thingsboard.rule.engine.mqtt.azure;

import io.netty.handler.codec.mqtt.MqttVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.credentials.CertPemCredentials;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.willReturn;

@ExtendWith(MockitoExtension.class)
public class TbAzureIotHubNodeTest extends AbstractRuleNodeUpgradeTest {

    private TbAzureIotHubNode azureIotHubNode;
    private TbAzureIotHubNodeConfiguration azureIotHubNodeConfig;

    @Mock
    protected TbContext ctxMock;
    @Mock
    protected MqttClient mqttClientMock;

    @BeforeEach
    public void setUp() {
        azureIotHubNode = spy(new TbAzureIotHubNode());
        azureIotHubNodeConfig = new TbAzureIotHubNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(azureIotHubNodeConfig.getTopicPattern()).isEqualTo("devices/<device_id>/messages/events/");
        assertThat(azureIotHubNodeConfig.getHost()).isEqualTo("<iot-hub-name>.azure-devices.net");
        assertThat(azureIotHubNodeConfig.getPort()).isEqualTo(8883);
        assertThat(azureIotHubNodeConfig.getConnectTimeoutSec()).isEqualTo(10);
        assertThat(azureIotHubNodeConfig.getClientId()).isNull();
        assertThat(azureIotHubNodeConfig.isAppendClientIdSuffix()).isFalse();
        assertThat(azureIotHubNodeConfig.isRetainedMessage()).isFalse();
        assertThat(azureIotHubNodeConfig.isCleanSession()).isTrue();
        assertThat(azureIotHubNodeConfig.isSsl()).isTrue();
        assertThat(azureIotHubNodeConfig.isParseToPlainText()).isFalse();
        assertThat(azureIotHubNodeConfig.getProtocolVersion()).isEqualTo(MqttVersion.MQTT_3_1_1);
        assertThat(azureIotHubNodeConfig.getCredentials()).isInstanceOf(AzureIotHubSasCredentials.class);
    }

    @Test
    public void verifyPrepareMqttClientConfigMethodWithAzureIotHubSasCredentials() throws Exception {
        var fixedClock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        azureIotHubNode.setClock(fixedClock);

        var credentials = new AzureIotHubSasCredentials();
        credentials.setSasKey("testSasKey");
        credentials.setCaCert("test-ca-cert.pem");
        azureIotHubNodeConfig.setCredentials(credentials);

        willReturn(mqttClientMock).given(azureIotHubNode).initAzureClient(any());
        azureIotHubNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(azureIotHubNodeConfig)));

        MqttClientConfig mqttClientConfig = new MqttClientConfig();
        azureIotHubNode.prepareMqttClientConfig(mqttClientConfig);

        assertThat(mqttClientConfig.getUsername()).isEqualTo(AzureIotHubUtil.buildUsername(azureIotHubNodeConfig.getHost(), mqttClientConfig.getClientId()));
        assertThat(mqttClientConfig.getPassword()).isEqualTo(AzureIotHubUtil.buildSasToken(azureIotHubNodeConfig.getHost(), credentials.getSasKey(), fixedClock));
    }

    @Test
    public void givenPemCredentialsAndSuccessfulConnectResult_whenInit_thenOk() throws Exception {
        CertPemCredentials credentials = new CertPemCredentials();
        credentials.setCaCert("test-ca-cert.pem");
        credentials.setPassword("test-password");
        azureIotHubNodeConfig.setCredentials(credentials);

        willReturn(mqttClientMock).given(azureIotHubNode).initAzureClient(any());

        assertThatNoException().isThrownBy(
                () -> azureIotHubNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(azureIotHubNodeConfig))));

        var mqttNodeConfiguration = (TbMqttNodeConfiguration) ReflectionTestUtils.getField(azureIotHubNode, "mqttNodeConfiguration");
        assertThat(mqttNodeConfiguration).isNotNull();
        assertThat(mqttNodeConfiguration.getPort()).isEqualTo(8883);
        assertThat(mqttNodeConfiguration.isCleanSession()).isTrue();
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"topicPattern\":\"devices/<device_id>/messages/events/\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"sas\",\"sasKey\":\"sasKey\",\"caCert\":null,\"caCertFileName\":null}}}",
                        true,
                        "{\"topicPattern\":\"devices/<device_id>/messages/events/\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"sas\",\"sasKey\":\"sasKey\",\"caCert\":null,\"caCertFileName\":null}, \"protocolVersion\":\"MQTT_3_1_1\"}\"}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\"topicPattern\":\"devices/<device_id>/messages/events/\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"sas\",\"sasKey\":\"sasKey\",\"caCert\":null,\"caCertFileName\":null}, \"protocolVersion\":\"MQTT_3_1_1\"}\"}",
                        false,
                        "{\"topicPattern\":\"devices/<device_id>/messages/events/\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"sas\",\"sasKey\":\"sasKey\",\"caCert\":null,\"caCertFileName\":null}, \"protocolVersion\":\"MQTT_3_1_1\"}\"}")
        );
    }

    @Override
    protected TbNode getTestNode() {
        return azureIotHubNode;
    }

}
