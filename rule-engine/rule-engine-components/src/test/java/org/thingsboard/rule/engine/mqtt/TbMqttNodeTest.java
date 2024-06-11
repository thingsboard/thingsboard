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
package org.thingsboard.rule.engine.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
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
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.AnonymousCredentials;
import org.thingsboard.rule.engine.credentials.BasicCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.net.ssl.SSLException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.amazonaws.util.StringUtils.UTF8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
public class TbMqttNodeTest extends AbstractRuleNodeUpgradeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("09115d92-d333-432a-868c-ccd6e89c9287"));

    protected TbMqttNode mqttNode;
    protected TbMqttNodeConfiguration mqttNodeConfig;

    @Mock
    protected TbContext ctxMock;
    @Mock
    protected MqttClient mqttClientMock;

    @BeforeEach
    protected void setUp() {
        mqttNode = spy(new TbMqttNode());
        mqttNodeConfig = new TbMqttNodeConfiguration().defaultConfiguration();
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
        assertThat(mqttNodeConfig.getCredentials()).isInstanceOf(AnonymousCredentials.class);
    }

    @Test
    public void verifyGetOwnerIdMethod() {
        String tenantIdStr = "6f67b6cc-21dd-46c5-809c-402b738a3f8b";
        String ruleNodeIdStr = "80a90b53-6888-4344-bf46-01ce8e96eee7";
        RuleNode ruleNode = new RuleNode(new RuleNodeId(UUID.fromString(ruleNodeIdStr)));
        given(ctxMock.getTenantId()).willReturn(TenantId.fromUUID(UUID.fromString(tenantIdStr)));
        given(ctxMock.getSelf()).willReturn(ruleNode);

        String actualOwnerIdStr = mqttNode.getOwnerId(ctxMock);
        String expectedOwnerIdStr = "Tenant[" + tenantIdStr + "]RuleNode[" + ruleNodeIdStr + "]";
        assertThat(actualOwnerIdStr).isEqualTo(expectedOwnerIdStr);
    }

    @Test
    public void verifyPrepareMqttClientConfigMethodWithBasicCredentials() throws SSLException {
        BasicCredentials credentials = new BasicCredentials();
        credentials.setUsername("test_username");
        credentials.setPassword("test_password");
        mqttNodeConfig.setCredentials(credentials);
        ReflectionTestUtils.setField(mqttNode, "mqttNodeConfiguration", mqttNodeConfig);
        MqttClientConfig mqttClientConfig = new MqttClientConfig(mqttNode.getSslContext());

        mqttNode.prepareMqttClientConfig(mqttClientConfig);

        assertThat(mqttClientConfig)
                .hasFieldOrPropertyWithValue("username", "test_username")
                .hasFieldOrPropertyWithValue("password", "test_password");
    }

    @ParameterizedTest
    @MethodSource
    public void verifyGetSslContextMethod(boolean ssl, ClientCredentials credentials, SslContext expectedSslContext) throws SSLException {
        mqttNodeConfig.setSsl(ssl);
        mqttNodeConfig.setCredentials(credentials);
        ReflectionTestUtils.setField(mqttNode, "mqttNodeConfiguration", mqttNodeConfig);

        SslContext actualSslContext = mqttNode.getSslContext();
        assertThat(actualSslContext)
                .usingRecursiveComparison()
                .ignoringFields("ctx", "ctxLock", "sessionContext.context.ctx", "sessionContext.context.ctxLock")
                .isEqualTo(expectedSslContext);
    }

    private static Stream<Arguments> verifyGetSslContextMethod() throws SSLException {
        return Stream.of(
                Arguments.of(true, new BasicCredentials(), SslContextBuilder.forClient().build()),
                Arguments.of(false, new AnonymousCredentials(), null)
        );
    }

    @Test
    public void givenFailedToInitializeMqttClient_whenInit_thenThrowsException() throws Exception {
        String errorMsg = "Failed to connect to MQTT broker!";
        willThrow(new RuntimeException(errorMsg)).given(mqttNode).initClient(any());

        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig));
        assertThatThrownBy(() -> mqttNode.init(ctxMock, configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage(RuntimeException.class.getName() + ": " + errorMsg);
    }

    @ParameterizedTest
    @MethodSource
    public void givenTopicPatternAndIsRetainedMsgIsTrue_whenOnMsg_thenTellSuccess(String topicPattern, TbMsgMetaData metaData, String data) throws Exception {
        mqttNodeConfig.setRetainedMessage(true);
        mqttNodeConfig.setTopicPattern(topicPattern);

        willReturn(mqttClientMock).given(mqttNode).initClient(any());
        Future<Void> future = mock(Future.class);
        given(future.isSuccess()).willReturn(true);
        given(mqttClientMock.publish(any(String.class), any(ByteBuf.class), any(MqttQoS.class), anyBoolean())).willReturn(future);
        willAnswer(invocation-> {
            GenericFutureListener<Future<Void>> listener = invocation.getArgument(0);
            listener.operationComplete(future);
            return null;
        }).given(future).addListener(any());

        mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig)));
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, data);
        mqttNode.onMsg(ctxMock, msg);

        String expectedTopic = TbNodeUtils.processPattern(mqttNodeConfig.getTopicPattern(), msg);
        then(mqttClientMock).should().publish(expectedTopic, Unpooled.wrappedBuffer(msg.getData().getBytes(UTF8)), MqttQoS.AT_LEAST_ONCE, true);
        then(ctxMock).should().tellSuccess(msg);
    }

    private static Stream<Arguments> givenTopicPatternAndIsRetainedMsgIsTrue_whenOnMsg_thenTellSuccess() {
        return Stream.of(
                Arguments.of("new-topic", TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("${md-topic-name}", new TbMsgMetaData(Map.of("md-topic-name", "md-new-topic")), TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("$[msg-topic-name]", TbMsgMetaData.EMPTY, "{\"msg-topic-name\":\"msg-new-topic\"}")
        );
    }

    @Test
    public void givenParseToPlainTextIsTrueAndMsgPublishingFailed_whenOnMsg_thenTellFailure() throws Exception {
        mqttNodeConfig.setParseToPlainText(true);

        willReturn(mqttClientMock).given(mqttNode).initClient(any());
        Future<Void> future = mock(Future.class);
        given(mqttClientMock.publish(any(String.class), any(ByteBuf.class), any(MqttQoS.class), anyBoolean())).willReturn(future);
        given(future.isSuccess()).willReturn(false);
        String errorMsg = "Message publishing was failed!";
        Throwable exception = new RuntimeException(errorMsg);
        given(future.cause()).willReturn(exception);
        willAnswer(invocation-> {
            GenericFutureListener<Future<Void>> listener = invocation.getArgument(0);
            listener.operationComplete(future);
            return null;
        }).given(future).addListener(any());

        mqttNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(mqttNodeConfig)));
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, "\"string\"");
        mqttNode.onMsg(ctxMock, msg);

        String expectedData = JacksonUtil.toPlainText(msg.getData());
        then(mqttClientMock).should().publish(mqttNodeConfig.getTopicPattern(), Unpooled.wrappedBuffer(expectedData.getBytes(UTF8)), MqttQoS.AT_LEAST_ONCE, false);
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("error", RuntimeException.class + ": " + errorMsg);
        TbMsg expectedMsg = TbMsg.transformMsgMetadata(msg, metaData);
        ArgumentCaptor<TbMsg> actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellFailure(actualMsgCaptor.capture(), eq(exception));
        TbMsg actualMsg = actualMsgCaptor.getValue();
        assertThat(actualMsg).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"}}",
                        true,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false}",
                        false,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false}")
        );

    }

    @Override
    protected TbNode getTestNode() {
        return mqttNode;
    }
}
