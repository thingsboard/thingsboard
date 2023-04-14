package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CalculateDeltaNodeTest {

    private static final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final ListeningExecutor DB_EXECUTOR = new ListeningExecutor() {
        @Override
        public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
            try {
                return Futures.immediateFuture(task.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void execute(@NotNull Runnable command) {
            command.run();
        }
    };
    @Mock
    private TbContext ctxMock;
    @Mock
    private TimeseriesService timeseriesServiceMock;
    private CalculateDeltaNode node;
    private CalculateDeltaNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new CalculateDeltaNode();
        config = new CalculateDeltaNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        node.init(ctxMock, nodeConfiguration);
    }

    @Test
    public void givenDefaultConfig_whenDefaultConfiguration_thenVerify() {
        assertEquals(config.getInputValueKey(), "pulseCounter");
        assertEquals(config.getOutputValueKey(), "delta");
        assertTrue(config.isUseCache());
        assertFalse(config.isAddPeriodBetweenMsgs());
        assertEquals(config.getPeriodValueKey(), "periodInMs");
        assertTrue(config.isTellFailureIfDeltaIsNegative());
        assertEquals(config.getRound(), 0);
    }

    @Test
    public void givenInvalidMsgType_whenOnMsg_thenShouldTellNextOther() {
        // GIVEN
        var msgData = "{\"pulseCounter\": 42}";
        var msg = TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(ctxMock, times(1)).tellNext(eq(msg), eq("Other"));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellFailure(any(), any());
    }

    @Test
    public void givenInputKeyIsNotPresent_whenOnMsg_thenShouldTellNextOther() {
        // GIVEN
        var msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), "{}");

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(ctxMock, times(1)).tellNext(eq(msg), eq("Other"));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellFailure(any(), any());
    }

    @Test
    public void happyPathWithDoubleValueAndNoCache() throws TbNodeException {
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of("temperature")))
        )).thenReturn(Futures.immediateFuture(
                List.of(new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 40.0)))
        ));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":2}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void happyPathWithLongValueAndNoCache() throws TbNodeException {
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of("temperature")))
        )).thenReturn(Futures.immediateFuture(
                List.of(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("temperature", 40L)))
        ));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":2}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void happyPathWithStringValueAndNoCache() throws TbNodeException {
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of("temperature")))
        )).thenReturn(Futures.immediateFuture(
                List.of(new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry("temperature", "40.0")))
        ));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":2}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void happyPathWithDoubleValueAndCache() throws TbNodeException {
        // STAGE 1
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of("temperature")))
        )).thenReturn(Futures.immediateFuture(
                List.of(new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 40.0)))
        ));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":2}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());

        // STAGE 2
        // GIVEN
        reset(ctxMock);
        reset(timeseriesServiceMock);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(timeseriesServiceMock, never()).findLatest(any(), any(), anyList());
        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":0}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void happyPathWithDoubleHaveDigitsAfterPointAndNoCache() throws TbNodeException {
        // STAGE 1
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setRound(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of("temperature")))
        )).thenReturn(Futures.immediateFuture(
                List.of(new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 40.500)))
        ));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":1.5}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void failedPathWithStringValueAndNoCache() {
        // GIVEN
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of("pulseCounter")))
        )).thenReturn(Futures.immediateFuture(
                List.of(new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry("pulseCounter", "high")))
        ));

        var msgData = "{\"pulseCounter\":\"123\"}";
        var msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var actualExceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(ctxMock, times(1)).tellFailure(actualMsgCaptor.capture(), actualExceptionCaptor.capture());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());

        var expectedExceptionMsg = "Calculation failed. Unable to parse value [high] of telemetry [pulseCounter] to Double";
        var actualException = actualExceptionCaptor.getValue();

        assertEquals(msg, actualMsgCaptor.getValue());
        assertInstanceOf(IllegalArgumentException.class, actualException);
        assertEquals(expectedExceptionMsg, actualException.getMessage());
    }

    @Test
    public void failedPathWithBooleanValueAndNoCache() {
        // GIVEN
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of("pulseCounter")))
        )).thenReturn(Futures.immediateFuture(
                List.of(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry("pulseCounter", false)))
        ));

        var msgData = "{\"pulseCounter\":true}";
        var msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var actualExceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(ctxMock, times(1)).tellFailure(actualMsgCaptor.capture(), actualExceptionCaptor.capture());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());

        var expectedExceptionMsg = "Calculation failed. Boolean values are not supported!";
        var actualException = actualExceptionCaptor.getValue();

        assertEquals(msg, actualMsgCaptor.getValue());
        assertInstanceOf(IllegalArgumentException.class, actualException);
        assertEquals(expectedExceptionMsg, actualException.getMessage());
    }

    @Test
    public void failedPathWithJsonValueAndNoCache() {
        // GIVEN
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), argThat(new ListMatcher<>(List.of("pulseCounter")))
        )).thenReturn(Futures.immediateFuture(
                List.of(new BasicTsKvEntry(System.currentTimeMillis(), new JsonDataEntry("pulseCounter", "{\"isActive\":false}")))
        ));

        var msgData = "{\"pulseCounter\":{\"isActive\":true}}";
        var msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", DUMMY_DEVICE_ORIGINATOR, new TbMsgMetaData(), msgData);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var actualExceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(ctxMock, times(1)).tellFailure(actualMsgCaptor.capture(), actualExceptionCaptor.capture());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());

        var expectedExceptionMsg = "Calculation failed. JSON values are not supported!";
        var actualException = actualExceptionCaptor.getValue();

        assertEquals(msg, actualMsgCaptor.getValue());
        assertInstanceOf(IllegalArgumentException.class, actualException);
        assertEquals(expectedExceptionMsg, actualException.getMessage());
    }

    @RequiredArgsConstructor
    private static class ListMatcher<T> implements ArgumentMatcher<List<T>> {

        private final List<T> expectedList;

        @Override
        public boolean matches(List<T> actualList) {
            if (actualList == expectedList) {
                return true;
            }
            if (actualList.size() != expectedList.size()) {
                return false;
            }
            return actualList.containsAll(expectedList);
        }

    }

}
