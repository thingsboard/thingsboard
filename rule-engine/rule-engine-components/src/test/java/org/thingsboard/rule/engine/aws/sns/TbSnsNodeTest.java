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
package org.thingsboard.rule.engine.aws.sns;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
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
class TbSnsNodeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("fccfdf2e-6a88-4a94-81dd-5cbb557019cf"));
    private final ListeningExecutor executor = new TestDbCallbackExecutor();

    private TbSnsNode node;
    private TbSnsNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private AmazonSNS snsClientMock;
    @Mock
    private PublishResult publishResultMock;
    @Mock
    private ResponseMetadata responseMetadataMock;

    @BeforeEach
    void setUp() {
        node = new TbSnsNode();
        config = new TbSnsNodeConfiguration().defaultConfiguration();
        ReflectionTestUtils.setField(node, "snsClient", snsClientMock);
        ReflectionTestUtils.setField(node, "config", config);
    }

    @Test
    void verifyDefaultConfig() {
        assertThat(config.getTopicArnPattern()).isEqualTo("arn:aws:sns:us-east-1:123456789012:MyNewTopic");
        assertThat(config.getAccessKeyId()).isNull();
        assertThat(config.getSecretAccessKey()).isNull();
        assertThat(config.getRegion()).isEqualTo("us-east-1");
    }

    @ParameterizedTest
    @MethodSource
    void givenTopicNamePattern_whenOnMsg_thenTellSuccess(String topicName, TbMsgMetaData metaData, String data)
            throws TbNodeException, ExecutionException, InterruptedException {
        config.setAccessKeyId("accessKeyId");
        config.setSecretAccessKey("secretAccessKey");
        config.setTopicArnPattern(topicName);
        String messageId = "msgId-1d186a16-80c7-44b3-a245-a1fc835f20c7";
        String requestId = "reqId-bef0799b-dde9-4aa0-855b-86bbafaeaf31";

        when(ctxMock.getExternalCallExecutor()).thenReturn(executor);
        when(snsClientMock.publish(any(PublishRequest.class))).thenReturn(publishResultMock);
        when(publishResultMock.getMessageId()).thenReturn(messageId);
        when(publishResultMock.getSdkResponseMetadata()).thenReturn(responseMetadataMock);
        when(responseMetadataMock.getRequestId()).thenReturn(requestId);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, data);
        node.onMsg(ctxMock, msg);

        PublishRequest publishRequest = new PublishRequest()
                .withTopicArn(TbNodeUtils.processPattern(topicName, msg))
                .withMessage(data);
        verify(snsClientMock).publish(publishRequest);
        ArgumentCaptor<TbMsg> msgArgumentCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock).tellSuccess(msgArgumentCaptor.capture());
        assertThat(msgArgumentCaptor.getValue().getMetaData().getData())
                .hasFieldOrPropertyWithValue("messageId", messageId)
                .hasFieldOrPropertyWithValue("requestId", requestId);
        verifyNoMoreInteractions(ctxMock, snsClientMock, publishResultMock, responseMetadataMock);
    }

    private static Stream<Arguments> givenTopicNamePattern_whenOnMsg_thenTellSuccess() {
        return Stream.of(
                Arguments.of("arn:aws:sns:us-east-1:123456789012:NewTopic", TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("arn:aws:sns:us-east-1:123456789012:$[msgTopicName]", TbMsgMetaData.EMPTY, "{\"msgTopicName\":\"msg-topic-name\"}"),
                Arguments.of("arn:aws:sns:us-east-1:123456789012:${mdTopicName}", new TbMsgMetaData(Map.of("mdTopicName", "md-topic-name")), TbMsg.EMPTY_JSON_OBJECT)
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
        verifyNoMoreInteractions(ctxMock, snsClientMock);
    }
}
