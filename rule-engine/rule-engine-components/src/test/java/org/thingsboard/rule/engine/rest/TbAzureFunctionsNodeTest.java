package org.thingsboard.rule.engine.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TbAzureFunctionsNodeTest {

    private TbAzureFunctionsNode node;
    private TbAzureFunctionsNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private TbHttpClient clientMock;

    @BeforeEach
    public void setUp() {
        node = new TbAzureFunctionsNode();
        config = new TbAzureFunctionsNodeConfiguration().defaultConfiguration();
        ReflectionTestUtils.setField(node, "httpClient", clientMock);
        ReflectionTestUtils.setField(node, "config", config);
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
    }

    @ParameterizedTest
    @ValueSource(strings = {"${msgKeyTemplate}", "msgKey", "10"})
    public void givenInputKeys_whenOnMsg_thenTellSuccess(String msgKey) throws TbNodeException, ExecutionException, InterruptedException {
        config.setInputKeys(Map.of("${funcKeyTemplate}", msgKey));

        String data = """
                {
                "msgKey": 10
                }
                """;
        Map<String, String> metadata = new HashMap<>();
        metadata.put("funcKeyTemplate", "funcKey");
        metadata.put("msgKeyTemplate", "msgKey");
        DeviceId deviceId = new DeviceId(UUID.fromString("b8956804-147b-4595-a50d-19fca6dd0c7b"));
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, new TbMsgMetaData(metadata), data);
        String processedData = "{\"funcKey\":\"10\"}";
        TbMsg transformedMsg = TbMsg.transformMsgData(msg, processedData);
        doAnswer(invocation -> {
            Consumer<TbMsg> consumer = invocation.getArgument(2);
            consumer.accept(transformedMsg);
            return null;
        }).when(clientMock).processMessage(any(), any(), any(), any());

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(clientMock).processMessage(eq(ctxMock), msgCaptor.capture(), any(), any());
        TbMsg msgCaptorValue = msgCaptor.getValue();
        assertThat(msgCaptorValue.getData()).isEqualTo(transformedMsg.getData());
        verify(ctxMock).tellSuccess(eq(transformedMsg));
    }

    @Test
    public void givenInvalidAccessToken_whenOnMsg_thenTellFailure() throws TbNodeException, ExecutionException, InterruptedException {
        DeviceId deviceId = new DeviceId(UUID.fromString("b8956804-147b-4595-a50d-19fca6dd0c7b"));
        Map<String, String> metadata = new HashMap<>();
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, new TbMsgMetaData(metadata), TbMsg.EMPTY_JSON_OBJECT);
        Exception thrownException = new Exception("Unauthorized");

        doAnswer(invocation -> {
            metadata.put("error", thrownException.getClass() + ": " + thrownException.getMessage());
            TbMsg transformedMsg = TbMsg.transformMsgMetadata(msg, new TbMsgMetaData(metadata));
            BiConsumer<TbMsg, Exception> consumer = invocation.getArgument(3);
            consumer.accept(transformedMsg, thrownException);
            return null;
        }).when(clientMock).processMessage(any(), any(), any(), any());

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctxMock).tellFailure(msgCaptor.capture(), exceptionCaptor.capture());
        TbMsg msgCaptorValue = msgCaptor.getValue();
        assertThat(msgCaptorValue.getMetaData().getData()).isEqualTo(metadata);
        Exception exceptionCaptorValue = exceptionCaptor.getValue();
        assertThat(exceptionCaptorValue.getMessage()).isEqualTo(thrownException.getMessage());
    }
}
