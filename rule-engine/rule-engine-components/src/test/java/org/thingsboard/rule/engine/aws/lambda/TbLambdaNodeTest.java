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
package org.thingsboard.rule.engine.aws.lambda;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
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
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbLambdaNodeTest {

    private static final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("ddb88645-7379-4a08-a51c-e84a0b4b3d88"));
    
    private TbLambdaNode node;
    private TbLambdaNodeConfiguration config;

    @Mock
    private TbContext ctx;
    @Mock
    private AWSLambdaAsync clientMock;

    @BeforeEach
    public void setUp() {
        node = new TbLambdaNode();
        config = new TbLambdaNodeConfiguration().defaultConfiguration();
    }

    @ParameterizedTest
    @MethodSource
    public void givenFunctionNameAndQualifierPatterns_whenOnMsg_thenTellSuccess(String functionName, String qualifier) throws TbNodeException, ExecutionException, InterruptedException {
        init();
        config.setFunctionName(functionName);
        config.setQualifier(qualifier);
        String data = """
                {
                "x": 10,
                "y": 20,
                "funcNameMsgPattern": "new-function",
                "qualifierMsgPattern": "$LATEST"
                }
                """;
        Map<String, String> metadata = new HashMap<>();
        metadata.put("funcNameMdPattern", "new-function");
        metadata.put("qualifierMdPattern", "$LATEST");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, new TbMsgMetaData(metadata), data);
        String requestIdStr = "a124af57-e7c3-4ebb-83bf-b09ff86eaa23";
        InvokeRequest request = createInvokeRequest(msg.getData(), "new-function", "$LATEST");
        InvokeResult result = new InvokeResult();
        result.setSdkResponseMetadata(new ResponseMetadata(Map.of("AWS_REQUEST_ID", requestIdStr)));
        String payload = "{\"statusCode\":200,\"body\":30}";
        result.setPayload(ByteBuffer.wrap(payload.getBytes()));
        metadata.put("requestId", requestIdStr);
        TbMsg resultedMsg = TbMsg.transformMsg(msg, new TbMsgMetaData(metadata), payload);

        when(clientMock.invokeAsync(any(), any())).then(invocation -> {
            AsyncHandler<InvokeRequest, InvokeResult> asyncHandler = invocation.getArgument(1);
            asyncHandler.onSuccess(request, result);
            return null;
        });

        node.onMsg(ctx, msg);

        verify(clientMock).invokeAsync(eq(request), any());
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(msgCaptor.capture());
        TbMsg msgCaptorValue = msgCaptor.getValue();
        assertThat(msgCaptorValue.getData()).isEqualTo(resultedMsg.getData());
        assertThat(msgCaptorValue.getMetaData()).isEqualTo(resultedMsg.getMetaData());
    }

    private static Stream<Arguments> givenFunctionNameAndQualifierPatterns_whenOnMsg_thenTellSuccess() {
        return Stream.of(
                Arguments.of("new-function", "$LATEST"),
                Arguments.of("${funcNameMdPattern}", "${qualifierMdPattern}"),
                Arguments.of("$[funcNameMsgPattern]", "$[qualifierMsgPattern]")
        );
    }

    @Test
    public void givenExceptionWasThrownInsideFunctionAndTellFailureIfFuncThrowsExcIsTrue_whenOnMsg_thenTellFailure() throws TbNodeException, ExecutionException, InterruptedException {
        init();
        config.setTellFailureIfFuncThrowsExc(true);
        String data = """
                {
                "x": 10
                }
                """;
        Map<String, String> metadata = new HashMap<>();
        metadata.put("data", "40");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, new TbMsgMetaData(metadata), data);
        InvokeRequest request = createInvokeRequest(msg.getData(), "new-function", "$LATEST");
        InvokeResult result = new InvokeResult();
        result.setFunctionError("Unhandled exception from function");
        metadata.put("error", RuntimeException.class + ": Unhandled exception from function");
        TbMsg resultedMsg = TbMsg.transformMsgMetadata(msg, new TbMsgMetaData(metadata));

        when(clientMock.invokeAsync(any(), any())).then(invocation -> {
            AsyncHandler<InvokeRequest, InvokeResult> asyncHandler = invocation.getArgument(1);
            asyncHandler.onSuccess(request, result);
            return null;
        });

        node.onMsg(ctx, msg);

        verify(clientMock).invokeAsync(eq(request), any());
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellFailure(msgCaptor.capture(), throwableCaptor.capture());
        TbMsg msgCaptorValue = msgCaptor.getValue();
        assertThat(msgCaptorValue.getData()).isEqualTo(resultedMsg.getData());
        assertThat(msgCaptorValue.getMetaData()).isEqualTo(resultedMsg.getMetaData());
        Throwable throwableCaptorValue = throwableCaptor.getValue();
        assertThat(throwableCaptorValue).isInstanceOf(RuntimeException.class);
        assertThat(throwableCaptorValue.getMessage()).isEqualTo("Unhandled exception from function");
    }

    @Test
    public void givenExceptionWasThrownInsideFunctionAndTellFailureIfFuncThrowsExcIsFalse_whenOnMsg_thenTellSuccess() throws TbNodeException, ExecutionException, InterruptedException {
        init();
        config.setTellFailureIfFuncThrowsExc(false);
        String data = """
                {
                "x": 10
                }
                """;
        Map<String, String> metadata = new HashMap<>();
        metadata.put("data", "40");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, new TbMsgMetaData(metadata), data);
        InvokeRequest request = createInvokeRequest(msg.getData(), "new-function", "$LATEST");
        String requestIdStr = "e83dfbc4-68d5-441c-8ee9-289959a30d3b";
        InvokeResult result = new InvokeResult();
        result.setSdkResponseMetadata(new ResponseMetadata(Map.of("AWS_REQUEST_ID", requestIdStr)));
        String payload = "{\"errorMessage\":\"Something went wrong\",\"errorType\":\"Exception\",\"requestId\":\"e83dfbc4-68d5-441c-8ee9-289959a30d3b\"}";
        result.setPayload(ByteBuffer.wrap(payload.getBytes()));
        metadata.put("requestId", requestIdStr);
        TbMsg resultedMsg = TbMsg.transformMsg(msg, new TbMsgMetaData(metadata), payload);

        when(clientMock.invokeAsync(any(), any())).then(invocation -> {
            AsyncHandler<InvokeRequest, InvokeResult> asyncHandler = invocation.getArgument(1);
            asyncHandler.onSuccess(request, result);
            return null;
        });

        node.onMsg(ctx, msg);

        verify(clientMock).invokeAsync(eq(request), any());
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(msgCaptor.capture());
        TbMsg msgCaptorValue = msgCaptor.getValue();
        assertThat(msgCaptorValue.getData()).isEqualTo(resultedMsg.getData());
        assertThat(msgCaptorValue.getMetaData()).isEqualTo(resultedMsg.getMetaData());
    }

    @Test
    public void givenExceptionWasThrownOnAWS_whenOnMsg_thenTellFailure() throws TbNodeException, ExecutionException, InterruptedException {
        init();
        String data = """
                {
                "x": 10
                }
                """;
        Map<String, String> metadata = new HashMap<>();
        metadata.put("data", "40");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, new TbMsgMetaData(metadata), data);
        InvokeRequest request = createInvokeRequest(msg.getData(), "new-function", "$LATEST");

        when(clientMock.invokeAsync(any(), any())).then(invocation -> {
            AsyncHandler<InvokeRequest, InvokeResult> asyncHandler = invocation.getArgument(1);
            asyncHandler.onError(new Exception("Simulated error"));
            return null;
        });

        node.onMsg(ctx, msg);

        verify(clientMock).invokeAsync(eq(request), any());
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(eq(msg), captor.capture());
    }

    private void init() {
        config.setAccessKey("accessKey");
        config.setSecretKey("secretKey");
        config.setFunctionName("new-function");
        config.setTellFailureIfFuncThrowsExc(false);
        ReflectionTestUtils.setField(node, "client", clientMock);
        ReflectionTestUtils.setField(node, "config", config);
    }

    private InvokeRequest createInvokeRequest(String requestBody, String functionName, String qualifier) {
        InvokeRequest request = new InvokeRequest()
                .withFunctionName(functionName)
                .withPayload(requestBody);
        request.setInvocationType(config.getInvocationType());
        request.withQualifier(qualifier);
        return request;
    }
}
