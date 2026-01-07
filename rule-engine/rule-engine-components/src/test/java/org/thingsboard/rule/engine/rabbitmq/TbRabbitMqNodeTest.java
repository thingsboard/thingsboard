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
package org.thingsboard.rule.engine.rabbitmq;


import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;

@ExtendWith(MockitoExtension.class)
public class TbRabbitMqNodeTest {

    private final String supportedPropertiesStr = String.join(", ",
            "BASIC", "TEXT_PLAIN", "MINIMAL_BASIC", "MINIMAL_PERSISTENT_BASIC", "PERSISTENT_BASIC", "PERSISTENT_TEXT_PLAIN"
    );

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("b3d6f9dd-15cc-4e61-acc0-13197a090406"));
    private final ListeningExecutor executor = new TestDbCallbackExecutor();

    private TbRabbitMqNode node;
    private TbRabbitMqNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private ConnectionFactory factoryMock;
    @Mock
    private Connection connectionMock;
    @Mock
    private Channel channelMock;

    @BeforeEach
    public void setUp() {
        node = spy(new TbRabbitMqNode());
        config = new TbRabbitMqNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getExchangeNamePattern()).isEqualTo("");
        assertThat(config.getRoutingKeyPattern()).isEqualTo("");
        assertThat(config.getMessageProperties()).isNull();
        assertThat(config.getHost()).isNull();
        assertThat(config.getPort()).isEqualTo(ConnectionFactory.DEFAULT_AMQP_PORT);
        assertThat(config.getVirtualHost()).isEqualTo(ConnectionFactory.DEFAULT_VHOST);
        assertThat(config.getUsername()).isEqualTo(ConnectionFactory.DEFAULT_USER);
        assertThat(config.getPassword()).isEqualTo(ConnectionFactory.DEFAULT_PASS);
        assertThat(config.isAutomaticRecoveryEnabled()).isFalse();
        assertThat(config.getConnectionTimeout()).isEqualTo(ConnectionFactory.DEFAULT_CONNECTION_TIMEOUT);
        assertThat(config.getHandshakeTimeout()).isEqualTo(ConnectionFactory.DEFAULT_HANDSHAKE_TIMEOUT);
        assertThat(config.getClientProperties()).isEqualTo(Collections.emptyMap());
    }

    @Test
    public void verifyGetConnectionFactoryMethod() {
        ReflectionTestUtils.setField(node, "config", config);

        ConnectionFactory connectionFactory = node.getConnectionFactory();
        assertThat(connectionFactory).isNotNull();
        assertThat(connectionFactory.getHost()).isEqualTo(config.getHost());
        assertThat(connectionFactory.getPort()).isEqualTo(config.getPort());
        assertThat(connectionFactory.getVirtualHost()).isEqualTo(config.getVirtualHost());
        assertThat(connectionFactory.getUsername()).isEqualTo(config.getUsername());
        assertThat(connectionFactory.getPassword()).isEqualTo(config.getPassword());
        assertThat(connectionFactory.isAutomaticRecoveryEnabled()).isEqualTo(config.isAutomaticRecoveryEnabled());
        assertThat(connectionFactory.getConnectionTimeout()).isEqualTo(config.getConnectionTimeout());
        assertThat(connectionFactory.getHandshakeTimeout()).isEqualTo(config.getHandshakeTimeout());
        Map<String, Object> expectedClientProperties = new ConnectionFactory().getClientProperties();
        expectedClientProperties.putAll(config.getClientProperties());
        assertThat(connectionFactory.getClientProperties()).isEqualTo(expectedClientProperties);
    }

    @ParameterizedTest
    @MethodSource
    public void givenForceAckIsTrueAndExchangeNameAndRoutingKeyPatternsAndBasicProperties_whenOnMsg_thenPublishMsgAndEnqueueForTellNext(
            String exchangeNamePattern, String routingKeyPattern, String basicProperties, TbMsgMetaData metaData, String data
    ) throws Exception {
        config.setExchangeNamePattern(exchangeNamePattern);
        config.setRoutingKeyPattern(routingKeyPattern);
        config.setMessageProperties(basicProperties);

        given(ctxMock.isExternalNodeForceAck()).willReturn(true);
        mockOnInit();
        given(ctxMock.getExternalCallExecutor()).willReturn(executor);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(data)
                .build();
        node.onMsg(ctxMock, msg);

        then(ctxMock).should().ack(msg);
        String exchangeName = TbNodeUtils.processPattern(exchangeNamePattern, msg);
        String routingKey = TbNodeUtils.processPattern(routingKeyPattern, msg);
        AMQP.BasicProperties properties = StringUtils.isNotEmpty(basicProperties) ? TbRabbitMqNode.convert(basicProperties) : null;
        then(channelMock).should().basicPublish(exchangeName, routingKey, properties, data.getBytes(StandardCharsets.UTF_8));
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().enqueueForTellNext(actualMsg.capture(), eq(TbNodeConnectionType.SUCCESS));
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(msg);
    }

    private static Stream<Arguments> givenForceAckIsTrueAndExchangeNameAndRoutingKeyPatternsAndBasicProperties_whenOnMsg_thenPublishMsgAndEnqueueForTellNext() {
        return Stream.of(
                Arguments.of("topic_logs", "kern.critical", "", TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("${mdExchangeName}", "${mdRoutingKey}", "BASIC",
                        new TbMsgMetaData(Map.of("mdExchangeName", "md_topic_logs", "mdRoutingKey", "md.kern.critical")),
                        TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("$[msgExchangeName]", "$[msgRoutingKey]", "MINIMAL_PERSISTENT_BASIC",
                        TbMsgMetaData.EMPTY, "{\"msgExchangeName\":\"msg_topic_logs\",\"msgRoutingKey\":\"msg.kern.critical\"}")
        );
    }

    @Test
    public void givenForceAckIsFalseAndExchangeNameAndRoutingKeyPatternsAndBasicProperties_whenOnMsg_thenPublishMsgAndTellSuccess() throws Exception {
        given(ctxMock.isExternalNodeForceAck()).willReturn(false);
        mockOnInit();
        given(ctxMock.getExternalCallExecutor()).willReturn(executor);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        then(ctxMock).should(never()).ack(any(TbMsg.class));
        then(channelMock).should().basicPublish("", "", null, msg.getData().getBytes(StandardCharsets.UTF_8));
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(msg);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void givenForceAckAndErrorOccursDuringPublishing_whenOnMsg_thenVerifyTellFailure(boolean forceAck) throws Exception {
        given(ctxMock.isExternalNodeForceAck()).willReturn(forceAck);
        mockOnInit();
        ListeningExecutor listeningExecutor = mock(ListeningExecutor.class);
        given(ctxMock.getExternalCallExecutor()).willReturn(listeningExecutor);
        String errorMsg = "Something went wrong";
        ListenableFuture<TbMsg> failedFuture = Futures.immediateFailedFuture(new RuntimeException(errorMsg));
        willReturn(failedFuture).given(listeningExecutor).executeAsync(any(Callable.class));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        TbMsgMetaData metaData = new TbMsgMetaData();
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        then(ctxMock).should(forceAck ? times(1) : never()).ack(any(TbMsg.class));
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> throwable = ArgumentCaptor.forClass(Throwable.class);
        Runnable verifyTellFailure = forceAck ?
                () -> then(ctxMock).should().enqueueForTellFailure(actualMsg.capture(), throwable.capture()) :
                () -> then(ctxMock).should().tellFailure(actualMsg.capture(), throwable.capture());
        verifyTellFailure.run();
        metaData.putValue("error", RuntimeException.class + ": " + errorMsg);
        TbMsg expectedMsg = msg.transform()
                .metaData(metaData)
                .build();
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
        assertThat(throwable.getValue()).isInstanceOf(RuntimeException.class).hasMessage(errorMsg);
    }

    @ParameterizedTest
    @MethodSource
    public void givenAMQPBasicPropertiesName_whenConvert_thenReturnAMQPBasicProperties(String name, AMQP.BasicProperties expectedBasicProperties) throws TbNodeException {
        AMQP.BasicProperties actualBasicProperties = TbRabbitMqNode.convert(name);
        assertThat(actualBasicProperties).isEqualTo(expectedBasicProperties);
    }

    private static Stream<Arguments> givenAMQPBasicPropertiesName_whenConvert_thenReturnAMQPBasicProperties() {
        return Stream.of(
                Arguments.of("BASIC", MessageProperties.BASIC),
                Arguments.of("TEXT_PLAIN", MessageProperties.TEXT_PLAIN),
                Arguments.of("MINIMAL_BASIC", MessageProperties.MINIMAL_BASIC),
                Arguments.of("MINIMAL_PERSISTENT_BASIC", MessageProperties.MINIMAL_PERSISTENT_BASIC),
                Arguments.of("PERSISTENT_BASIC", MessageProperties.PERSISTENT_BASIC),
                Arguments.of("PERSISTENT_TEXT_PLAIN", MessageProperties.PERSISTENT_TEXT_PLAIN)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic", "TEXT_plain", "minimal basic", "", "    "})
    public void givenUndefinedProperties_whenConvert_thenThrowsException(String name) {
        assertThatThrownBy(() -> TbRabbitMqNode.convert(name))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Undefined message properties type '" + name +
                        "'! Only " + supportedPropertiesStr + " message properties types are supported!");
    }

    @Test
    public void givenConnection_whenDestroy_thenShouldClose() throws IOException {
        ReflectionTestUtils.setField(node, "connection", connectionMock);
        node.destroy();
        then(connectionMock).should().close();
    }

    @Test
    public void givenConnectionIsNull_whenDestroy_thenVerifyNoInteractions() {
        node.destroy();
        then(connectionMock).shouldHaveNoInteractions();
    }

    private void mockOnInit() throws IOException, TimeoutException {
        willAnswer(invocation -> factoryMock).given(node).getConnectionFactory();
        given(factoryMock.newConnection()).willReturn(connectionMock);
        given(connectionMock.createChannel()).willReturn(channelMock);
    }

}
