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
package org.thingsboard.rule.engine.aws.sqs;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.aws.sqs.TbSqsNodeConfiguration.QueueType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class TbSqsNodeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("764de824-929f-4114-95ea-0ea0401ffa3d"));
    private final ListeningExecutor executor = new TestDbCallbackExecutor();

    private final String messageId = "msgId-1d186a16-80c7-44b3-a245-a1fc835f20c7";
    private final String requestId = "reqId-bef0799b-dde9-4aa0-855b-86bbafaeaf31";

    private TbSqsNode node;
    private TbSqsNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private AmazonSQS sqsClientMock;
    @Mock
    private SendMessageResult sendMessageResultMock;
    @Mock
    private ResponseMetadata responseMetadataMock;

    @BeforeEach
    void setUp() {
        node = new TbSqsNode();
        config = new TbSqsNodeConfiguration().defaultConfiguration();
        ReflectionTestUtils.setField(node, "sqsClient", sqsClientMock);
        ReflectionTestUtils.setField(node, "config", config);
    }

    @Test
    void verifyDefaultConfig() {
        assertThat(config.getQueueType()).isEqualTo(QueueType.STANDARD);
        assertThat(config.getQueueUrlPattern()).isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789012/my-queue-name");
        assertThat(config.getDelaySeconds()).isEqualTo(0);
        assertThat(config.getMessageAttributes()).isEqualTo(Collections.emptyMap());
        assertThat(config.getAccessKeyId()).isNull();
        assertThat(config.getSecretAccessKey()).isNull();
        assertThat(config.getRegion()).isEqualTo("us-east-1");
    }

    @ParameterizedTest
    @MethodSource
    void givenQueueUrlPatternsAndQueueTypeIsFifo_whenOnMsg_thenVerifyRequest(String queueUrl, TbMsgMetaData metaData, String data) {
        config.setQueueType(QueueType.FIFO);
        config.setQueueUrlPattern(queueUrl);

        mockSendingMsgRequest();

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(data)
                .build();
        node.onMsg(ctxMock, msg);

        SendMessageRequest sendMsgRequest = new SendMessageRequest()
                .withQueueUrl(TbNodeUtils.processPattern(queueUrl, msg))
                .withMessageBody(data)
                .withMessageDeduplicationId(msg.getId().toString())
                .withMessageGroupId(DEVICE_ID.toString());
        then(sqsClientMock).should().sendMessage(sendMsgRequest);
    }

    private static Stream<Arguments> givenQueueUrlPatternsAndQueueTypeIsFifo_whenOnMsg_thenVerifyRequest() {
        return Stream.of(
                Arguments.of(
                        "https://sqs.us-east-1.amazonaws.com/123456789012/new-queue-name",
                        TbMsgMetaData.EMPTY,
                        TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of(
                        "https://sqs.us-east-1.amazonaws.com/123456789012/$[msgQueueName]",
                        TbMsgMetaData.EMPTY,
                        "{\"msgQueueName\":\"msg-queue-name\"}"),
                Arguments.of(
                        "https://sqs.us-east-1.amazonaws.com/123456789012/${mdQueueName}",
                        new TbMsgMetaData(Map.of("mdQueueName", "md-queue-name")),
                        TbMsg.EMPTY_JSON_OBJECT)
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenMsgAttributesPatternsAndQueueTypeIsStandard_whenOnMsg_thenVerifyRequest(TbMsgMetaData metaData, String data,
                                                                                      Map<String, String> attributes) {
        config.setMessageAttributes(attributes);

        mockSendingMsgRequest();

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(data)
                .build();
        node.onMsg(ctxMock, msg);

        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        this.config.getMessageAttributes().forEach((k, v) -> {
            String name = TbNodeUtils.processPattern(k, msg);
            String val = TbNodeUtils.processPattern(v, msg);
            messageAttributes.put(name, new MessageAttributeValue().withDataType("String").withStringValue(val));
        });
        SendMessageRequest sendMsgRequest = new SendMessageRequest()
                .withQueueUrl(config.getQueueUrlPattern())
                .withMessageBody(data)
                .withMessageAttributes(messageAttributes)
                .withDelaySeconds(config.getDelaySeconds());
        then(sqsClientMock).should().sendMessage(sendMsgRequest);
    }

    private static Stream<Arguments> givenMsgAttributesPatternsAndQueueTypeIsStandard_whenOnMsg_thenVerifyRequest() {
        return Stream.of(
                Arguments.of(TbMsgMetaData.EMPTY,
                        TbMsg.EMPTY_JSON_OBJECT,
                        Map.of("attributeName", "attributeValue")),
                Arguments.of(TbMsgMetaData.EMPTY,
                        "{\"msgAttrNamePattern\":\"msgAttrName\",\"msgAttrValuePattern\":\"msgAttrValue\"}",
                        Map.of("$[msgAttrNamePattern]", "$[msgAttrValuePattern]")),
                Arguments.of(new TbMsgMetaData(Map.of("mdAttrNamePattern", "mdAttrName", "mdAttrValuePattern", "mdAttrValue")),
                        TbMsg.EMPTY_JSON_OBJECT,
                        Map.of("${mdAttrNamePattern}", "${mdAttrValuePattern}"))
        );
    }

    @Test
    void givenForceAckIsTrueAndMsgResultContainsBodyAndAttributesAndNumber_whenOnMsg_thenEnqueueForTellNext() {
        ReflectionTestUtils.setField(node, "forceAck", true);
        String messageBodyMd5 = "msgBodyMd5-55fb8ba2-2b71-4673-a82a-969756764761";
        String messageAttributesMd5 = "msgAttrMd5-e3ba3eef-52ae-436a-bec1-0c2c2252d1f1";
        String sequenceNumber = "seqNum-bb5ddce0-cf4e-4295-b015-524bdb6a332f";

        mockSendingMsgRequest();
        given(sendMessageResultMock.getMD5OfMessageBody()).willReturn(messageBodyMd5);
        given(sendMessageResultMock.getMD5OfMessageAttributes()).willReturn(messageAttributesMd5);
        given(sendMessageResultMock.getSequenceNumber()).willReturn(sequenceNumber);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        then(ctxMock).should().ack(msg);
        SendMessageRequest sendMsgRequest = new SendMessageRequest()
                .withQueueUrl(TbNodeUtils.processPattern(config.getQueueUrlPattern(), msg))
                .withMessageBody(msg.getData())
                .withDelaySeconds(config.getDelaySeconds());
        then(sqsClientMock).should().sendMessage(sendMsgRequest);
        ArgumentCaptor<TbMsg> actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().enqueueForTellNext(actualMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS));
        TbMsg actualMsg = actualMsgCaptor.getValue();
        assertThat(actualMsg)
                .usingRecursiveComparison()
                .ignoringFields("metaData", "ctx")
                .isEqualTo(msg);
        assertThat(actualMsg.getMetaData().getData())
                .hasFieldOrPropertyWithValue("messageId", messageId)
                .hasFieldOrPropertyWithValue("requestId", requestId)
                .hasFieldOrPropertyWithValue("messageBodyMd5", messageBodyMd5)
                .hasFieldOrPropertyWithValue("messageAttributesMd5", messageAttributesMd5)
                .hasFieldOrPropertyWithValue("sequenceNumber", sequenceNumber);
        verifyNoMoreInteractions(ctxMock, sqsClientMock, sendMessageResultMock, responseMetadataMock);
    }

    @Test
    void givenForceAckIsFalseAndErrorOccursDuringProcessingRequest_whenOnMsg_thenTellFailure() {
        ReflectionTestUtils.setField(node, "forceAck", false);
        ListeningExecutor listeningExecutor = mock(ListeningExecutor.class);
        given(ctxMock.getExternalCallExecutor()).willReturn(listeningExecutor);
        String errorMsg = "Something went wrong";

        ListenableFuture<TbMsg> failedFuture = Futures.immediateFailedFuture(new RuntimeException(errorMsg));
        given(listeningExecutor.executeAsync(any(Callable.class))).willReturn(failedFuture);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        then(ctxMock).should(never()).enqueueForTellNext(any(), any(String.class));
        ArgumentCaptor<TbMsg> actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(actualMsgCaptor.capture(), throwableCaptor.capture());
        TbMsg actualMsg = actualMsgCaptor.getValue();
        assertThat(actualMsg)
                .usingRecursiveComparison()
                .ignoringFields("metaData", "ctx")
                .isEqualTo(msg);
        assertThat(actualMsg.getMetaData().getData())
                .hasFieldOrPropertyWithValue("error", RuntimeException.class + ": " + errorMsg);
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class).hasMessage(errorMsg);
        verifyNoMoreInteractions(ctxMock, sqsClientMock);
    }

    @Test
    void givenSqsClientIsNotNull_whenDestroy_thenShutdown() {
        node.destroy();
        then(sqsClientMock).should().shutdown();
    }

    @Test
    void givenSqsClientIsNull_whenDestroy_thenVerifyNoInteractions() {
        ReflectionTestUtils.setField(node, "sqsClient", null);
        node.destroy();
        then(sqsClientMock).shouldHaveNoInteractions();
    }

    private void mockSendingMsgRequest() {
        given(ctxMock.getExternalCallExecutor()).willReturn(executor);
        given(sqsClientMock.sendMessage(any(SendMessageRequest.class))).willReturn(sendMessageResultMock);
        given(sendMessageResultMock.getMessageId()).willReturn(messageId);
        given(sendMessageResultMock.getSdkResponseMetadata()).willReturn(responseMetadataMock);
        given(responseMetadataMock.getRequestId()).willReturn(requestId);
    }

}
