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
package org.thingsboard.rule.engine.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttConnectResult;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.MqttClientSettings;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.AnonymousCredentials;
import org.thingsboard.rule.engine.credentials.BasicCredentials;
import org.thingsboard.rule.engine.credentials.CertPemCredentials;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class TbMqttNodeTest extends AbstractRuleNodeUpgradeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("d0c5d2a8-3a6e-4c95-8caf-47fbdc8ef98f"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("09115d92-d333-432a-868c-ccd6e89c9287"));
    private final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("11699e8f-c3f0-4366-9334-cbf75798314b"));

    protected TbMqttNode mqttNode;
    protected TbMqttNodeConfiguration mqttNodeConfig;

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
    protected void setUp() {
        mqttNode = spy(new TbMqttNode());
        mqttNodeConfig = new TbMqttNodeConfiguration().defaultConfiguration();
        lenient().when(ctxMock.getMqttClientSettings()).thenReturn(new MqttClientSettings() {
            @Override
            public int getRetransmissionMaxAttempts() {
                return 3;
            }

            @Override
            public long getRetransmissionInitialDelayMillis() {
                return 5000L;
            }

            @Override
            public double getRetransmissionJitterFactor() {
                return 0.15;
            }
        });
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(mqttNodeConfig.getTopicPattern()).isEqualTo("my-topic");
        assertThat(mqttNodeConfig.getHost()).isNull();
        assertThat(mqttNodeConfig.getPort()).isEqualTo(1883);
        assertThat(mqttNodeConfig.getConnectTimeoutSec()).isEqualTo(10);
        assertThat(mqttNodeConfig.getClientId()).isNull();
        assertThat(mqttNodeConfig.isAppendClientIdSuffix()).isFalse();
        assertThat(mqttNodeConfig.isRetainedMessage()).isFalse();
        assertThat(mqttNodeConfig.isCleanSession()).isTrue();
        assertThat(mqttNodeConfig.isSsl()).isFalse();
        assertThat(mqttNodeConfig.isParseToPlainText()).isFalse();
        assertThat(mqttNodeConfig.getProtocolVersion()).isEqualTo(MqttVersion.MQTT_3_1_1);
        assertThat(mqttNodeConfig.getCredentials()).isInstanceOf(AnonymousCredentials.class);
    }

    @Test
    public void verifyGetOwnerIdMethod() {
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getSelf()).willReturn(new RuleNode(RULE_NODE_ID));

        String actualOwnerIdStr = mqttNode.getOwnerId(ctxMock);
        String expectedOwnerIdStr = "Tenant[" + TENANT_ID.getId() + "]RuleNode[" + RULE_NODE_ID.getId() + "]";
        assertThat(actualOwnerIdStr).isEqualTo(expectedOwnerIdStr);
    }

    @Test
    public void verifyPrepareMqttClientConfigMethodWithBasicCredentials() throws Exception {
        BasicCredentials credentials = new BasicCredentials();
        credentials.setUsername("test_username");
        credentials.setPassword("test_password");
        mqttNodeConfig.setCredentials(credentials);

        mockSuccessfulInit();
        mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig)));

        MqttClientConfig mqttClientConfig = new MqttClientConfig();
        mqttNode.prepareMqttClientConfig(mqttClientConfig);

        assertThat(mqttClientConfig.getUsername()).isEqualTo("test_username");
        assertThat(mqttClientConfig.getPassword()).isEqualTo("test_password");
    }

    @Test
    public void givenSslIsTrueAndCredentials_whenGetSslContext_thenVerifySslContext() throws Exception {
        mqttNodeConfig.setSsl(true);
        mqttNodeConfig.setCredentials(new BasicCredentials());

        mockSuccessfulInit();
        mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig)));

        ArgumentCaptor<MqttClientConfig> mqttClientConfig = ArgumentCaptor.forClass(MqttClientConfig.class);
        then(mqttNode).should().prepareMqttClientConfig(mqttClientConfig.capture());
        SslContext actualSslContext = mqttClientConfig.getValue().getSslContext();
        assertThat(actualSslContext)
                .usingRecursiveComparison()
                .ignoringFields("ctx", "ctxLock", "sessionContext.context.ctx", "sessionContext.context.ctxLock",
                        "sslContext")
                .isEqualTo(SslContextBuilder.forClient().build());
    }

    @Test
    public void givenSslIsFalse_whenGetSslContext_thenVerifySslContextIsNull() throws Exception {
        mqttNodeConfig.setSsl(false);

        mockSuccessfulInit();
        mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig)));

        ArgumentCaptor<MqttClientConfig> mqttClientConfig = ArgumentCaptor.forClass(MqttClientConfig.class);
        then(mqttNode).should().prepareMqttClientConfig(mqttClientConfig.capture());
        SslContext actualSslContext = mqttClientConfig.getValue().getSslContext();
        assertThat(actualSslContext).isNull();
    }

    @Test
    public void givenSuccessfulConnectResult_whenInit_thenOk() throws Exception {
        mqttNodeConfig.setClientId("bfrbTESTmfkr23");
        mqttNodeConfig.setAppendClientIdSuffix(true);
        mqttNodeConfig.setCredentials(new CertPemCredentials());

        mockSuccessfulInit();

        assertThatNoException().isThrownBy(() -> mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig))));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidClientIdScenarios")
    public void givenInvalidClientId_whenInit_thenThrowsException(MqttVersion version, int maxLength, int repeat, String serviceId, boolean appendSuffix) {
        String baseClientId = "x".repeat(repeat);
        mqttNodeConfig.setClientId(baseClientId);
        mqttNodeConfig.setAppendClientIdSuffix(appendSuffix);
        mqttNodeConfig.setProtocolVersion(version);

        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getSelf()).willReturn(new RuleNode(RULE_NODE_ID));

        String clientId = appendSuffix ? baseClientId + "_" + serviceId : baseClientId;
        if (appendSuffix) {
            given(ctxMock.getServiceId()).willReturn(serviceId);
        }

        String expectedMessage = "The length of Client ID cannot be longer than " + maxLength + ", but current length is " + clientId.length() + ".";

        assertThatThrownBy(() -> mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage(expectedMessage)
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    private static Stream<Arguments> provideInvalidClientIdScenarios() {
        return Stream.of(
                // MQTT_5, too long clientId
                Arguments.of(MqttVersion.MQTT_5, 256, 257, null, false),

                // MQTT_5, base + suffix exceeds
                Arguments.of(MqttVersion.MQTT_5, 256, 250, "test-service", true),

                // MQTT_3_1, too long clientId
                Arguments.of(MqttVersion.MQTT_3_1, 23, 24, null, false),

                // MQTT_3_1, base + suffix exceeds
                Arguments.of(MqttVersion.MQTT_3_1, 23, 5, "verylongservicename", true)
        );
    }

    @Test
    public void givenFailedByTimeoutConnectResult_whenInit_thenThrowsException() throws ExecutionException, InterruptedException, TimeoutException {
        mqttNodeConfig.setHost("localhost");
        mqttNodeConfig.setClientId("bfrbTESTmfkr23");
        mqttNodeConfig.setCredentials(new CertPemCredentials());

        mockConnectClient();
        given(promiseMock.get(anyLong(), any(TimeUnit.class))).willThrow(new TimeoutException("Failed to connect"));

        assertThatThrownBy(() -> mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("java.lang.RuntimeException: Failed to connect to MQTT broker at localhost:1883.")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(false);
    }

    @Test
    public void givenFailedConnectResult_whenInit_thenThrowsException() throws Exception {
        mqttNodeConfig.setHost("localhost");
        mqttNodeConfig.setClientId("bfrbTESTmfkr23");
        mqttNodeConfig.setAppendClientIdSuffix(true);
        mqttNodeConfig.setCredentials(new CertPemCredentials());

        mockConnectClient();
        given(promiseMock.get(anyLong(), any(TimeUnit.class))).willReturn(resultMock);
        given(resultMock.isSuccess()).willReturn(false);
        given(resultMock.getReturnCode()).willReturn(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED);

        assertThatThrownBy(() -> mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("java.lang.RuntimeException: Failed to connect to MQTT broker at localhost:1883. Result code is: CONNECTION_REFUSED_NOT_AUTHORIZED")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(false);
    }

    @ParameterizedTest
    @MethodSource
    public void givenForceAckIsTrueAndTopicPatternAndIsRetainedMsgIsTrue_whenOnMsg_thenTellSuccess(
            String topicPattern, TbMsgMetaData metaData, String data
    ) throws Exception {
        mqttNodeConfig.setRetainedMessage(true);
        mqttNodeConfig.setTopicPattern(topicPattern);

        given(ctxMock.isExternalNodeForceAck()).willReturn(true);
        mockSuccessfulInit();
        mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig)));

        Future<Void> future = mock(Future.class);
        given(future.isSuccess()).willReturn(true);
        given(mqttClientMock.publish(any(String.class), any(ByteBuf.class), any(MqttQoS.class), anyBoolean())).willReturn(future);
        willAnswer(invocation -> {
            GenericFutureListener<Future<Void>> listener = invocation.getArgument(0);
            listener.operationComplete(future);
            return null;
        }).given(future).addListener(any());

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(data)
                .build();
        mqttNode.onMsg(ctxMock, msg);

        then(ctxMock).should().ack(msg);
        String expectedTopic = TbNodeUtils.processPattern(mqttNodeConfig.getTopicPattern(), msg);
        then(mqttClientMock).should().publish(expectedTopic, Unpooled.wrappedBuffer(msg.getData().getBytes(StandardCharsets.UTF_8)), MqttQoS.AT_LEAST_ONCE, true);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().enqueueForTellNext(actualMsg.capture(), eq(TbNodeConnectionType.SUCCESS));
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(msg);
    }

    private static Stream<Arguments> givenForceAckIsTrueAndTopicPatternAndIsRetainedMsgIsTrue_whenOnMsg_thenTellSuccess() {
        return Stream.of(
                Arguments.of("new-topic", TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("${md-topic-name}", new TbMsgMetaData(Map.of("md-topic-name", "md-new-topic")), TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("$[msg-topic-name]", TbMsgMetaData.EMPTY, "{\"msg-topic-name\":\"msg-new-topic\"}")
        );
    }

    @Test
    public void givenForceAckIsFalseParseToPlainTextIsTrueAndMsgPublishingFailed_whenOnMsg_thenTellFailure() throws Exception {
        mqttNodeConfig.setParseToPlainText(true);

        given(ctxMock.isExternalNodeForceAck()).willReturn(false);
        mockSuccessfulInit();
        mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig)));

        Future<Void> future = mock(Future.class);
        given(mqttClientMock.publish(any(String.class), any(ByteBuf.class), any(MqttQoS.class), anyBoolean())).willReturn(future);
        given(future.isSuccess()).willReturn(false);
        String errorMsg = "Message publishing was failed!";
        Throwable exception = new RuntimeException(errorMsg);
        given(future.cause()).willReturn(exception);
        willAnswer(invocation -> {
            GenericFutureListener<Future<Void>> listener = invocation.getArgument(0);
            listener.operationComplete(future);
            return null;
        }).given(future).addListener(any());

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data("\"string\"")
                .build();
        mqttNode.onMsg(ctxMock, msg);

        then(ctxMock).should(never()).ack(msg);
        String expectedData = JacksonUtil.toPlainText(msg.getData());
        then(mqttClientMock).should().publish(mqttNodeConfig.getTopicPattern(), Unpooled.wrappedBuffer(expectedData.getBytes(StandardCharsets.UTF_8)), MqttQoS.AT_LEAST_ONCE, false);
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("error", RuntimeException.class + ": " + errorMsg);
        TbMsg expectedMsg = msg.transform()
                .metaData(metaData)
                .build();
        ArgumentCaptor<TbMsg> actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellFailure(actualMsgCaptor.capture(), eq(exception));
        TbMsg actualMsg = actualMsgCaptor.getValue();
        assertThat(actualMsg).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    @Test
    public void givenMqttClientIsNotNull_whenDestroy_thenDisconnect() {
        ReflectionTestUtils.setField(mqttNode, "mqttClient", mqttClientMock);
        mqttNode.destroy();
        then(mqttClientMock).should().disconnect();
    }

    @Test
    public void givenMqttClientIsNull_whenDestroy_thenShouldHaveNoInteractions() {
        ReflectionTestUtils.setField(mqttNode, "mqttClient", null);
        mqttNode.destroy();
        then(mqttClientMock).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @MethodSource
    public void verifyProtocolVersionMapping(MqttVersion expectedVersion) throws Exception {
        mqttNodeConfig.setProtocolVersion(expectedVersion);

        given(ctxMock.isExternalNodeForceAck()).willReturn(false);
        mockSuccessfulInit();
        mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig)));

        ArgumentCaptor<MqttClientConfig> configCaptor = ArgumentCaptor.forClass(MqttClientConfig.class);
        then(mqttNode).should().prepareMqttClientConfig(configCaptor.capture());
        assertThat(expectedVersion).isEqualTo(configCaptor.getValue().getProtocolVersion());
    }

    private static Stream<Arguments> verifyProtocolVersionMapping() {
        return Stream.of(MqttVersion.values()).map(Arguments::of);
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"}}",
                        true,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false, \"protocolVersion\":\"MQTT_3_1\"}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false}",
                        true,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false, \"protocolVersion\":\"MQTT_3_1\"}"),
                // default config for version 2 with upgrade from version 1
                Arguments.of(2,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false, \"protocolVersion\":\"MQTT_3_1\"}",
                        false,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false, \"protocolVersion\":\"MQTT_3_1\"}")
        );
    }

    @Override
    protected TbNode getTestNode() {
        return mqttNode;
    }

    private void mockConnectClient() {
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getSelf()).willReturn(new RuleNode(RULE_NODE_ID));
        given(ctxMock.getSharedEventLoop()).willReturn(eventLoopGroupMock);
        willReturn(mqttClientMock).given(mqttNode).getMqttClient(any(), any());
        given(mqttClientMock.connect(any(), anyInt())).willReturn(promiseMock);
    }

    private void mockSuccessfulInit() throws Exception {
        mockConnectClient();
        given(promiseMock.get(anyLong(), any(TimeUnit.class))).willReturn(resultMock);
        given(resultMock.isSuccess()).willReturn(true);
    }

}
