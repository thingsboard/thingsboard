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
package org.thingsboard.rule.engine.aws.lambda;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.rule.engine.aws.lambda.TbAwsLambdaNodeConfiguration.DEFAULT_QUALIFIER;

@ExtendWith(MockitoExtension.class)
public class TbAwsLambdaNodeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("ddb88645-7379-4a08-a51c-e84a0b4b3d88"));

    private TbAwsLambdaNode node;
    private TbAwsLambdaNodeConfiguration config;

    @Mock
    private TbContext ctx;
    @Mock
    private AWSLambdaAsync clientMock;

    @BeforeEach
    public void setUp() {
        node = new TbAwsLambdaNode();
        config = new TbAwsLambdaNodeConfiguration().defaultConfiguration();
        config.setAccessKey("accessKey");
        config.setSecretKey("secretKey");
        config.setFunctionName("new-function");
    }

    @Test
    public void verifyDefaultConfig() {
        config = new TbAwsLambdaNodeConfiguration().defaultConfiguration();
        assertThat(config.getAccessKey()).isNull();
        assertThat(config.getSecretKey()).isNull();
        assertThat(config.getRegion()).isEqualTo(("us-east-1"));
        assertThat(config.getFunctionName()).isNull();
        assertThat(config.getQualifier()).isEqualTo(DEFAULT_QUALIFIER);
        assertThat(config.getConnectionTimeout()).isEqualTo(10);
        assertThat(config.getRequestTimeout()).isEqualTo(5);
        assertThat(config.isTellFailureIfFuncThrowsExc()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    public void givenInvalidFunctionName_whenInit_thenThrowsException(String funcName) {
        config.setFunctionName(funcName);
        verifyValidationExceptionOnInit();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    public void givenInvalidAccessKey_whenInit_thenThrowsException(String accessKey) {
        config.setAccessKey(accessKey);
        verifyValidationExceptionOnInit();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    public void givenInvalidSecretAccessKey_whenInit_thenThrowsException(String secretAccessKey) {
        config.setSecretKey(secretAccessKey);
        verifyValidationExceptionOnInit();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    public void givenInvalidRegion_whenInit_thenThrowsException(String region) {
        config.setRegion(region);
        verifyValidationExceptionOnInit();
    }

    @Test
    public void givenInvalidConnectionTimeout_whenInit_thenThrowsException() {
        config.setConnectionTimeout(-100);
        verifyValidationExceptionOnInit();
    }

    @Test
    public void givenInvalidRequestTimeout_whenInit_thenThrowsException() {
        config.setRequestTimeout(-100);
        verifyValidationExceptionOnInit();
    }

    @ParameterizedTest
    @MethodSource
    public void givenRequest_whenOnMsg_thenTellSuccess(String data, TbMsgMetaData metadata, String functionName, String qualifier, String expectedQualifier) {
        init();
        config.setFunctionName(functionName);
        config.setQualifier(qualifier);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(data)
                .build();

        InvokeRequest request = createInvokeRequest(msg);
        String requestIdStr = "a124af57-e7c3-4ebb-83bf-b09ff86eaa23";
        String funcResponsePayload = "{\"statusCode\":200}";

        when(clientMock.invokeAsync(any(), any())).then(invocation -> {
            InvokeResult result = new InvokeResult();
            result.setSdkResponseMetadata(new ResponseMetadata(Map.of(ResponseMetadata.AWS_REQUEST_ID, requestIdStr)));
            result.setPayload(ByteBuffer.wrap(funcResponsePayload.getBytes()));
            AsyncHandler<InvokeRequest, InvokeResult> asyncHandler = invocation.getArgument(1);
            asyncHandler.onSuccess(request, result);
            return null;
        });

        node.onMsg(ctx, msg);

        ArgumentCaptor<InvokeRequest> invokeRequestCaptor = ArgumentCaptor.forClass(InvokeRequest.class);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(clientMock).invokeAsync(invokeRequestCaptor.capture(), any());
        verify(ctx).tellSuccess(msgCaptor.capture());

        assertThat(invokeRequestCaptor.getValue().getQualifier()).isEqualTo(expectedQualifier);
        TbMsgMetaData resultMsgMetadata = metadata.copy();
        resultMsgMetadata.putValue("requestId", requestIdStr);
        TbMsg resultedMsg = msg.transform()
                .metaData(resultMsgMetadata)
                .data(funcResponsePayload)
                .build();
        assertThat(msgCaptor.getValue()).usingRecursiveComparison()
                .ignoringFields("ctx")
                .isEqualTo(resultedMsg);
    }

    private static Stream<Arguments> givenRequest_whenOnMsg_thenTellSuccess() {
        return Stream.of(
                Arguments.of(TbMsg.EMPTY_JSON_OBJECT, TbMsgMetaData.EMPTY, "functionA", "qualifierA", "qualifierA"),
                Arguments.of(TbMsg.EMPTY_JSON_OBJECT, TbMsgMetaData.EMPTY, "functionA", null, DEFAULT_QUALIFIER),
                Arguments.of("{\"funcNameMsgPattern\": \"functionB\", \"qualifierMsgPattern\": \"qualifierB\"}",
                        TbMsgMetaData.EMPTY, "$[funcNameMsgPattern]", "$[qualifierMsgPattern]", "qualifierB"),
                Arguments.of(TbMsg.EMPTY_JSON_OBJECT,
                        new TbMsgMetaData(
                                Map.of(
                                        "funcNameMdPattern", "functionC",
                                        "qualifierMdPattern", "qualifierC")
                        ), "${funcNameMdPattern}", "${qualifierMdPattern}", "qualifierC")
        );
    }

    @Test
    public void givenExceptionWasThrownInsideFunctionAndTellFailureIfFuncThrowsExcIsTrue_whenOnMsg_thenTellFailure() {
        init();
        config.setTellFailureIfFuncThrowsExc(true);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_ARRAY)
                .build();
        InvokeRequest request = createInvokeRequest(msg);
        String requestIdStr = "a124af57-e7c3-4ebb-83bf-b09ff86eaa23";
        String errorMsg = "Unhandled exception from function";

        when(clientMock.invokeAsync(any(), any())).then(invocation -> {
            InvokeResult result = new InvokeResult();
            result.setPayload(ByteBuffer.wrap(errorMsg.getBytes(StandardCharsets.UTF_8)));
            result.setFunctionError(errorMsg);
            result.setSdkResponseMetadata(new ResponseMetadata(Map.of(ResponseMetadata.AWS_REQUEST_ID, requestIdStr)));
            AsyncHandler<InvokeRequest, InvokeResult> asyncHandler = invocation.getArgument(1);
            asyncHandler.onSuccess(request, result);
            return null;
        });

        node.onMsg(ctx, msg);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(clientMock).invokeAsync(eq(request), any());
        verify(ctx).tellFailure(msgCaptor.capture(), throwableCaptor.capture());

        var metadata = Map.of("error", RuntimeException.class + ": " + errorMsg, "requestId", requestIdStr);
        TbMsg resultedMsg = msg.transform()
                .metaData(new TbMsgMetaData(metadata))
                .build();

        assertThat(msgCaptor.getValue()).usingRecursiveComparison()
                .ignoringFields("ctx")
                .isEqualTo(resultedMsg);
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class).hasMessage(errorMsg);
    }

    @Test
    public void givenExceptionWasThrownInsideFunctionAndTellFailureIfFuncThrowsExcIsFalse_whenOnMsg_thenTellSuccess() {
        init();

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        InvokeRequest request = createInvokeRequest(msg);
        String requestIdStr = "e83dfbc4-68d5-441c-8ee9-289959a30d3b";
        String payload = "{\"errorMessage\":\"Something went wrong\",\"errorType\":\"Exception\",\"requestId\":\"" + requestIdStr + "\"}";

        when(clientMock.invokeAsync(any(), any())).then(invocation -> {
            InvokeResult result = new InvokeResult();
            result.setSdkResponseMetadata(new ResponseMetadata(Map.of("AWS_REQUEST_ID", requestIdStr)));
            result.setPayload(ByteBuffer.wrap(payload.getBytes()));
            AsyncHandler<InvokeRequest, InvokeResult> asyncHandler = invocation.getArgument(1);
            asyncHandler.onSuccess(request, result);
            return null;
        });

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(clientMock).invokeAsync(eq(request), any());
        verify(ctx).tellSuccess(msgCaptor.capture());

        Map<String, String> metadata = Map.of("requestId", requestIdStr);
        TbMsg resultedMsg = msg.transform()
                .metaData(new TbMsgMetaData(metadata))
                .data(payload)
                .build();

        assertThat(msgCaptor.getValue()).usingRecursiveComparison()
                .ignoringFields("ctx")
                .isEqualTo(resultedMsg);
    }

    @Test
    public void givenPayloadFromResultIsNull_whenOnMsg_thenTellFailure() {
        init();

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        InvokeRequest request = createInvokeRequest(msg);
        String requestIdStr = "12bbb074-e2fc-4381-8f28-d4bd235103d5";
        String errorMsg = "Payload from result of AWS Lambda function execution is null.";

        when(clientMock.invokeAsync(any(), any())).then(invocation -> {
            InvokeResult result = new InvokeResult();
            result.setSdkResponseMetadata(new ResponseMetadata(Map.of(ResponseMetadata.AWS_REQUEST_ID, requestIdStr)));
            AsyncHandler<InvokeRequest, InvokeResult> asyncHandler = invocation.getArgument(1);
            asyncHandler.onSuccess(request, result);
            return null;
        });

        node.onMsg(ctx, msg);

        verify(clientMock).invokeAsync(eq(request), any());

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctx).tellFailure(msgCaptor.capture(), throwableCaptor.capture());

        var metadata = Map.of("error", RuntimeException.class + ": " + errorMsg, "requestId", requestIdStr);
        TbMsg resultedMsg = msg.transform()
                .metaData(new TbMsgMetaData(metadata))
                .build();

        assertThat(msgCaptor.getValue()).usingRecursiveComparison()
                .ignoringFields("ctx")
                .isEqualTo(resultedMsg);
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class).hasMessage(errorMsg);
    }

    @Test
    public void givenExceptionWasThrownOnAWS_whenOnMsg_thenTellFailure() {
        init();
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        InvokeRequest request = createInvokeRequest(msg);

        String errorMsg = "Simulated error";
        when(clientMock.invokeAsync(any(), any())).then(invocation -> {
            AsyncHandler<InvokeRequest, InvokeResult> asyncHandler = invocation.getArgument(1);
            asyncHandler.onError(new AWSLambdaException(errorMsg));
            return null;
        });

        node.onMsg(ctx, msg);

        verify(clientMock).invokeAsync(eq(request), any());
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(eq(msg), throwableCaptor.capture());
        assertThat(throwableCaptor.getValue()).isInstanceOf(AWSLambdaException.class).hasMessageStartingWith(errorMsg);
    }

    private void verifyValidationExceptionOnInit() {
        RuleNode ruleNode = new RuleNode();
        ruleNode.setName("test");
        when(ctx.getSelf()).thenReturn(ruleNode);
        String errorPrefix = "'test' node configuration is invalid: ";
        assertThatThrownBy(() -> node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessageContaining(errorPrefix)
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    private void init() {
        ReflectionTestUtils.setField(node, "client", clientMock);
        ReflectionTestUtils.setField(node, "config", config);
    }

    private InvokeRequest createInvokeRequest(TbMsg msg) {
        return new InvokeRequest()
                .withFunctionName(getFunctionName(msg))
                .withPayload(msg.getData())
                .withQualifier(getQualifier(msg));
    }

    private String getQualifier(TbMsg msg) {
        return StringUtils.isBlank(config.getQualifier()) ?
                TbAwsLambdaNodeConfiguration.DEFAULT_QUALIFIER :
                TbNodeUtils.processPattern(config.getQualifier(), msg);
    }

    private String getFunctionName(TbMsg msg) {
        return TbNodeUtils.processPattern(config.getFunctionName(), msg);
    }

}
