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
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbSqsNodeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("764de824-929f-4114-95ea-0ea0401ffa3d"));
    private final ListeningExecutor executor = new TestDbCallbackExecutor();

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
        assertThat(config.getQueueType()).isEqualTo(TbSqsNodeConfiguration.QueueType.STANDARD);
        assertThat(config.getQueueUrlPattern()).isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789012/my-queue-name");
        assertThat(config.getDelaySeconds()).isEqualTo(0);
        assertThat(config.getMessageAttributes()).isEqualTo(Collections.emptyMap());
        assertThat(config.getAccessKeyId()).isNull();
        assertThat(config.getSecretAccessKey()).isNull();
        assertThat(config.getRegion()).isEqualTo("us-east-1");
    }

    @ParameterizedTest
    @MethodSource
    void givenQueueUrlAndMsgAttributesPatternsAndQueueTypes_whenOnMsg_thenTellSuccess(String queueUrl,
                                                                                      TbMsgMetaData metaData,
                                                                                      String data,
                                                                                      Map<String, String> attributes,
                                                                                      TbSqsNodeConfiguration.QueueType queueType) {
        config.setAccessKeyId("accessKeyId");
        config.setSecretAccessKey("secretAccessKey");
        config.setQueueUrlPattern(queueUrl);
        config.setQueueType(queueType);
        config.setMessageAttributes(attributes);

        String messageId = "msgId-1d186a16-80c7-44b3-a245-a1fc835f20c7";
        String requestId = "reqId-bef0799b-dde9-4aa0-855b-86bbafaeaf31";
        String messageBodyMd5 = "msgBodyMd5-55fb8ba2-2b71-4673-a82a-969756764761";
        String messageAttributesMd5 = "msgAttrMd5-e3ba3eef-52ae-436a-bec1-0c2c2252d1f1";
        String sequenceNumber = "seqNum-bb5ddce0-cf4e-4295-b015-524bdb6a332f";

        when(ctxMock.getExternalCallExecutor()).thenReturn(executor);
        when(sqsClientMock.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResultMock);
        when(sendMessageResultMock.getMessageId()).thenReturn(messageId);
        when(sendMessageResultMock.getSdkResponseMetadata()).thenReturn(responseMetadataMock);
        when(responseMetadataMock.getRequestId()).thenReturn(requestId);
        when(sendMessageResultMock.getMD5OfMessageBody()).thenReturn(messageBodyMd5);
        when(sendMessageResultMock.getMD5OfMessageAttributes()).thenReturn(messageAttributesMd5);
        when(sendMessageResultMock.getSequenceNumber()).thenReturn(sequenceNumber);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, data);
        node.onMsg(ctxMock, msg);

        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        this.config.getMessageAttributes().forEach((k,v) -> {
            String name = TbNodeUtils.processPattern(k, msg);
            String val = TbNodeUtils.processPattern(v, msg);
            messageAttributes.put(name, new MessageAttributeValue().withDataType("String").withStringValue(val));
        });
        SendMessageRequest sendMsgRequest =  new SendMessageRequest()
                .withQueueUrl(TbNodeUtils.processPattern(queueUrl, msg))
                .withMessageBody(data)
                .withMessageAttributes(messageAttributes);
        if (queueType == TbSqsNodeConfiguration.QueueType.STANDARD) {
            sendMsgRequest.setDelaySeconds(0);
        } else {
            sendMsgRequest.withMessageDeduplicationId(msg.getId().toString());
            sendMsgRequest.withMessageGroupId(DEVICE_ID.toString());
        }
        verify(sqsClientMock).sendMessage(sendMsgRequest);
        ArgumentCaptor<TbMsg> msgArgumentCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock).tellSuccess(msgArgumentCaptor.capture());
        assertThat(msgArgumentCaptor.getValue().getMetaData().getData())
                .hasFieldOrPropertyWithValue("messageId", messageId)
                .hasFieldOrPropertyWithValue("requestId", requestId)
                .hasFieldOrPropertyWithValue("messageBodyMd5", messageBodyMd5)
                .hasFieldOrPropertyWithValue("messageAttributesMd5", messageAttributesMd5)
                .hasFieldOrPropertyWithValue("sequenceNumber", sequenceNumber);
        verifyNoMoreInteractions(ctxMock, sqsClientMock, sendMessageResultMock, responseMetadataMock);
    }

    private static Stream<Arguments> givenQueueUrlAndMsgAttributesPatternsAndQueueTypes_whenOnMsg_thenTellSuccess() {
        return Stream.of(
                Arguments.of(
                        "https://sqs.us-east-1.amazonaws.com/123456789012/new-queue-name",
                        TbMsgMetaData.EMPTY,
                        TbMsg.EMPTY_JSON_OBJECT,
                        Map.of("attributeName", "attributeValue"),
                        TbSqsNodeConfiguration.QueueType.STANDARD),
                Arguments.of(
                        "https://sqs.us-east-1.amazonaws.com/123456789012/$[msgQueueName]",
                        TbMsgMetaData.EMPTY,
                        "{\"msgQueueName\":\"msg-queue-name\",\"msgAttrNamePattern\":\"msgAttrName\",\"msgAttrValuePattern\":\"msgAttrValue\"}",
                        Map.of("$[msgAttrNamePattern]", "$[msgAttrValuePattern]"),
                        TbSqsNodeConfiguration.QueueType.FIFO),
                Arguments.of("https://sqs.us-east-1.amazonaws.com/123456789012/${mdQueueName}",
                        new TbMsgMetaData(Map.of("mdQueueName", "md-queue-name", "mdAttrNamePattern", "mdAttrName", "mdAttrValuePattern", "mdAttrValue")),
                        TbMsg.EMPTY_JSON_OBJECT,
                        Map.of("${mdAttrNamePattern}", "${mdAttrValuePattern}"),
                        TbSqsNodeConfiguration.QueueType.STANDARD)
        );
    }

    @Test
    void givenErrorOccursDuringProcessingRequest_whenOnMsg_thenTellFailure() throws TbNodeException, ExecutionException, InterruptedException {
        ListeningExecutor listeningExecutor = mock(ListeningExecutor.class);
        when(ctxMock.getExternalCallExecutor()).thenReturn(listeningExecutor);
        String errorMsg = "Something went wrong";

        ListenableFuture<TbMsg> failedFuture = Futures.immediateFailedFuture(new RuntimeException(errorMsg));
        when(listeningExecutor.executeAsync(any(Callable.class))).thenReturn(failedFuture);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<TbMsg> msgArgumentCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(msgArgumentCaptor.capture(), throwableCaptor.capture());
        assertThat(msgArgumentCaptor.getValue().getMetaData().getData())
                .hasFieldOrPropertyWithValue("error", RuntimeException.class + ": " + errorMsg);
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class).hasMessage(errorMsg);
        verifyNoMoreInteractions(ctxMock, sqsClientMock);
    }

}
