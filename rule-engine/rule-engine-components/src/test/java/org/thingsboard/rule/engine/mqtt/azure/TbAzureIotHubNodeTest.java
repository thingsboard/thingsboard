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

import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttConnectResult;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.credentials.CertPemCredentials;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.willReturn;

@ExtendWith(MockitoExtension.class)
public class TbAzureIotHubNodeTest {

    private TbAzureIotHubNode azureIotHubNode;
    private TbAzureIotHubNodeConfiguration azureIotHubNodeConfig;

    @Mock
    protected TbContext ctxMock;
    @Mock
    protected MqttClient mqttClientMock;
    @Mock
    protected EventLoopGroup eventLoopGroupMock;
    @Mock
    protected Promise<MqttConnectResult> promiseMock;
    @Mock
    protected MqttConnectResult resultMock;

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
        assertThat(azureIotHubNodeConfig.getCredentials()).isInstanceOf(AzureIotHubSasCredentials.class);
    }

    @Test
    public void verifyPrepareMqttClientConfigMethodWithAzureIotHubSasCredentials() throws Exception {
        AzureIotHubSasCredentials credentials = new AzureIotHubSasCredentials();
        credentials.setSasKey("testSasKey");
        credentials.setCaCert("test-ca-cert.pem");
        azureIotHubNodeConfig.setCredentials(credentials);

        mockSuccessfulInit();
        azureIotHubNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(azureIotHubNodeConfig)));

        MqttClientConfig mqttClientConfig = new MqttClientConfig();
        azureIotHubNode.prepareMqttClientConfig(mqttClientConfig);

        assertThat(mqttClientConfig.getProtocolVersion()).isEqualTo(MqttVersion.MQTT_3_1_1);
        assertThat(mqttClientConfig.getUsername()).isEqualTo(AzureIotHubUtil.buildUsername(azureIotHubNodeConfig.getHost(), mqttClientConfig.getClientId()));
        assertThat(mqttClientConfig.getPassword()).isEqualTo(AzureIotHubUtil.buildSasToken(azureIotHubNodeConfig.getHost(), credentials.getSasKey()));
    }

    @Test
    public void givenPemCredentialsAndSuccessfulConnectResult_whenInit_thenOk() throws Exception {
        CertPemCredentials credentials = new CertPemCredentials();
        credentials.setCaCert("test-ca-cert.pem");
        credentials.setPassword("test-password");
        azureIotHubNodeConfig.setCredentials(credentials);

        mockSuccessfulInit();

        assertThatNoException().isThrownBy(
                () -> azureIotHubNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(azureIotHubNodeConfig))));

        TbMqttNodeConfiguration mqttNodeConfiguration = azureIotHubNode.getMqttNodeConfiguration();
        assertThat(mqttNodeConfiguration.getPort()).isEqualTo(8883);
        assertThat(mqttNodeConfiguration.isCleanSession()).isTrue();
    }

    private void mockSuccessfulInit() throws Exception {
        given(ctxMock.getTenantId()).willReturn(TenantId.fromUUID(UUID.fromString("74aad2a5-3c07-43fb-9c9b-07fafb4e86ce")));
        given(ctxMock.getSelf()).willReturn(new RuleNode(new RuleNodeId(UUID.fromString("da5eb2ef-4ea7-4ac9-9359-0e727a0c30ce"))));
        given(ctxMock.getSharedEventLoop()).willReturn(eventLoopGroupMock);
        willReturn(mqttClientMock).given(azureIotHubNode).getMqttClient(any(), any());
        given(mqttClientMock.connect(any(), anyInt())).willReturn(promiseMock);
        given(promiseMock.get(anyLong(), any(TimeUnit.class))).willReturn(resultMock);
        given(resultMock.isSuccess()).willReturn(true);
    }
}
