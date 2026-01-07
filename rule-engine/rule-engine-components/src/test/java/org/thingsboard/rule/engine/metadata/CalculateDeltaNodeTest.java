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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
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
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class CalculateDeltaNodeTest extends AbstractRuleNodeUpgradeTest {

    private final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.fromString("2ba3ded4-882b-40cf-999a-89da9ccd58f9"));
    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("3842e740-0d89-43a9-8d52-ae44023847ba"));
    private final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();

    private static final int RULE_DISPATCHER_POOL_SIZE = 2;
    private static final int DB_CALLBACK_POOL_SIZE = 3;

    @Mock
    private TbContext ctxMock;
    @Mock
    private TimeseriesService timeseriesServiceMock;
    @Spy
    private CalculateDeltaNode node;
    private CalculateDeltaNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;

    @BeforeEach
    public void setUp() throws TbNodeException {
        config = new CalculateDeltaNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
    }

    @Test
    public void givenDefaultConfig_whenDefaultConfiguration_thenVerify() {
        assertEquals(config.getInputValueKey(), "pulseCounter");
        assertEquals(config.getOutputValueKey(), "delta");
        assertTrue(config.isUseCache());
        assertFalse(config.isAddPeriodBetweenMsgs());
        assertEquals(config.getPeriodValueKey(), "periodInMs");
        assertTrue(config.isTellFailureIfDeltaIsNegative());
    }


    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "}) // blank value
    public void givenInvalidInputKey_whenInitThenThrowException(String key) {
        config.setInputValueKey(key);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));
        assertThat(exception).hasMessage("Input value key should be specified!");
        assertThat(exception.isUnrecoverable()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "}) // blank value
    public void givenInvalidOutputKey_whenInitThenThrowException(String key) {
        config.setOutputValueKey(key);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));
        assertThat(exception).hasMessage("Output value key should be specified!");
        assertThat(exception.isUnrecoverable()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "}) // blank value
    public void givenInvalidPeriodKey_whenInitThenThrowException(String key) {
        config.setPeriodValueKey(key);
        config.setAddPeriodBetweenMsgs(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));
        assertThat(exception).hasMessage("Period value key should be specified!");
        assertThat(exception.isUnrecoverable()).isTrue();
    }

    @Test
    public void givenInvalidPeriodKeyAndAddPeriodDisabled_whenInitThenNoExceptionThrown() {
        config.setPeriodValueKey(null);
        config.setAddPeriodBetweenMsgs(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertDoesNotThrow(() -> node.init(ctxMock, nodeConfiguration));
    }

    @Test
    public void givenInvalidMsgType_whenOnMsg_thenShouldTellNextOther() throws TbNodeException {
        // GIVEN
        node.init(ctxMock, nodeConfiguration);
        var msgData = "{\"pulseCounter\": 42}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(ctxMock).tellNext(eq(msg), eq(TbNodeConnectionType.OTHER));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellFailure(any(), any());
    }

    @Test
    public void givenInvalidMsgDataType_whenOnMsg_thenShouldTellNextOther() throws TbNodeException {
        // GIVEN
        node.init(ctxMock, nodeConfiguration);
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_ARRAY)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(ctxMock).tellNext(eq(msg), eq(TbNodeConnectionType.OTHER));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellFailure(any(), any());
    }


    @Test
    public void givenInputKeyIsNotPresent_whenOnMsg_thenShouldTellNextOther() throws TbNodeException {
        // GIVEN
        node.init(ctxMock, nodeConfiguration);
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(ctxMock).tellNext(eq(msg), eq(TbNodeConnectionType.OTHER));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellFailure(any(), any());
    }

    @Test
    public void givenDoubleValue_whenOnMsgAndCachingOff_thenShouldTellSuccess() throws TbNodeException {
        // GIVEN
        config.setRound(1);
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setUseCache(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 40.5)));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":1.5}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void givenLongStringValue_whenOnMsgAndCachingOff_thenShouldTellSuccess() throws TbNodeException {
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setUseCache(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("temperature", 40L)));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":2}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void givenValidStringValue_whenOnMsgAndCachingOff_thenShouldTellSuccess() throws TbNodeException {
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setUseCache(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry("temperature", "40.0")));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":2}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void givenTwoMessagesAndPeriodOnAndCachingOn_whenOnMsg_thenVerify() throws TbNodeException {
        // STAGE 1
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setPeriodValueKey("ts_delta");
        config.setAddPeriodBetweenMsgs(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(1L, new DoubleDataEntry("temperature", 40.0)));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var firstMsgMetaData = new TbMsgMetaData();
        firstMsgMetaData.putValue("ts", String.valueOf(3L));
        var firstMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(firstMsgMetaData)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, firstMsg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":2,\"ts_delta\":2}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());

        // STAGE 2
        // GIVEN
        reset(ctxMock);
        reset(timeseriesServiceMock);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        var secondMsgMetaData = new TbMsgMetaData();
        secondMsgMetaData.putValue("ts", String.valueOf(6L));
        var secondMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(secondMsgMetaData)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, secondMsg);

        // THEN
        actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(timeseriesServiceMock, never()).findLatest(any(), any(), anyList());
        verify(ctxMock).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":0,\"ts_delta\":3}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void givenLastValueIsNull_whenOnMsgAndCachingOff_thenDeltaShouldBeZero() throws TbNodeException {
        // GIVEN
        config.setInputValueKey("temperature");
        config.setOutputValueKey("temp_delta");
        config.setUseCache(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", null)));

        var msgData = "{\"temperature\": 42,\"airPressure\":123}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temperature\":42,\"airPressure\":123,\"temp_delta\":0}";

        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void givenNegativeDeltaAndTellFailureIfNegativeDeltaTrue_whenOnMsg_thenShouldTellFailure() throws TbNodeException {
        // GIVEN
        config.setTellFailureIfDeltaIsNegative(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("pulseCounter", 200L)));

        var msgData = "{\"pulseCounter\":\"123\"}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var actualExceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(ctxMock).tellFailure(actualMsgCaptor.capture(), actualExceptionCaptor.capture());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());

        var expectedExceptionMsg = "Delta value is negative!";
        var actualException = actualExceptionCaptor.getValue();

        assertEquals(msg, actualMsgCaptor.getValue());
        assertInstanceOf(IllegalArgumentException.class, actualException);
        assertEquals(expectedExceptionMsg, actualException.getMessage());
    }

    @Test
    public void givenNegativeDeltaAndTellFailureIfNegativeDeltaFalse_whenOnMsg_thenShouldTellSuccess() throws TbNodeException {
        // GIVEN
        config.setTellFailureIfDeltaIsNegative(false);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("pulseCounter", 200L)));

        var msgData = "{\"pulseCounter\":\"123\"}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(actualMsgCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());

        String expectedMsgData = "{\"pulseCounter\":\"123\",\"delta\":-77}";
        assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
    }

    @Test
    public void givenInvalidStringValue_whenOnMsg_thenException() throws TbNodeException {
        // GIVEN
        node.init(ctxMock, nodeConfiguration);
        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry("pulseCounter", "high")));

        var msgData = "{\"pulseCounter\":\"123\"}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());

        assertThat(throwableCaptor.getValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculation failed. Unable to parse value [high] of telemetry [pulseCounter] to Double");
    }

    @Test
    public void givenBooleanValue_whenOnMsg_thenException() throws TbNodeException {
        // GIVEN
        node.init(ctxMock, nodeConfiguration);
        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry("pulseCounter", false)));

        var msgData = "{\"pulseCounter\":true}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());

        assertThat(throwableCaptor.getValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculation failed. Boolean values are not supported!");
    }

    @Test
    public void givenJsonValue_whenOnMsg_thenException() throws TbNodeException {
        // GIVEN
        node.init(ctxMock, nodeConfiguration);
        mockFindLatestAsync(new BasicTsKvEntry(System.currentTimeMillis(), new JsonDataEntry("pulseCounter", "{\"isActive\":false}")));

        var msgData = "{\"pulseCounter\":{\"isActive\":true}}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());

        assertThat(throwableCaptor.getValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculation failed. JSON values are not supported!");
    }

    @Test
    public void givenConcurrentAccess_whenOnMsg_thenGetFromDBInvokedOnce() throws TbNodeException, InterruptedException {
        DBCallbackExecutor dbCallbackExecutor = new DBCallbackExecutor();
        dbCallbackExecutor.init();

        RuleDispatcherExecutor ruleEngineDispatcherExecutor = new RuleDispatcherExecutor();
        ruleEngineDispatcherExecutor.init();

        assertThat(RULE_DISPATCHER_POOL_SIZE).as("dispatcher pool size have to be > 1").isGreaterThan(1);

        final TbContext ctx = mock(TbContext.class);
        final TimeseriesService timeseriesService = mock(TimeseriesService.class);

        when(ctx.getTimeseriesService()).thenReturn(timeseriesService);
        when(ctx.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(timeseriesService.findLatest(any(), any(), anyString())).thenReturn(Futures.immediateFuture(Optional.empty()));

        final CalculateDeltaNodeConfiguration config = new CalculateDeltaNodeConfiguration().defaultConfiguration();
        final TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        final CalculateDeltaNode node = spy(CalculateDeltaNode.class);

        node.init(ctx, nodeConfiguration);

        List<TbMsg> tbMsgList = IntStream.range(0, RULE_DISPATCHER_POOL_SIZE * 2).mapToObj(x -> {
            var msgData = "{\"pulseCounter\":" + 2 + "}";
            return TbMsg.newMsg()
                    .type(TbMsgType.POST_TELEMETRY_REQUEST)
                    .originator(DUMMY_DEVICE_ORIGINATOR)
                    .copyMetaData(TbMsgMetaData.EMPTY)
                    .data(msgData)
                    .build();
        }).toList();

        CountDownLatch processingLatch = new CountDownLatch(tbMsgList.size());

        willAnswer(invocation -> {
            processingLatch.countDown();
            return invocation.callRealMethod();
        }).given(node).processMsgAsync(any(), any());

        tbMsgList.forEach(msg -> ruleEngineDispatcherExecutor.executeAsync(() -> node.onMsg(ctx, msg)));

        assertThat(processingLatch.await(5, TimeUnit.SECONDS)).as("await on processingLatch").isTrue();

        verify(timeseriesService).findLatest(any(), any(), anyString());
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> verify(ctx, times(tbMsgList.size())).tellSuccess(any()));
    }

    private static class RuleDispatcherExecutor extends AbstractListeningExecutor {
        @Override
        protected int getThreadPollSize() {
            return RULE_DISPATCHER_POOL_SIZE;
        }
    }

    private static class DBCallbackExecutor extends AbstractListeningExecutor {
        @Override
        protected int getThreadPollSize() {
            return DB_CALLBACK_POOL_SIZE;
        }
    }

    @ParameterizedTest
    @MethodSource("CalculateDeltaTestConfig")
    public void givenCalculateDeltaConfig_whenOnMsg_thenVerify(CalculateDeltaTestConfig testConfig) throws TbNodeException {
        // GIVEN
        config.setTellFailureIfDeltaIsNegative(testConfig.tellFailureIfDeltaIsNegative());
        config.setExcludeZeroDeltas(testConfig.excludeZeroDeltas());
        config.setInputValueKey("temperature");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        mockFindLatestAsync(new BasicTsKvEntry(1L, new DoubleDataEntry("temperature", testConfig.prevValue())));

        var msgData = "{\"temperature\":" + testConfig.currentValue() + ",\"airPressure\":123}";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(msgData)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        testConfig.verificationMethod().accept(ctxMock, msg);
    }

    private static Stream<CalculateDeltaTestConfig> CalculateDeltaTestConfig() {
        return Stream.of(
                // delta = 0, tell failure if delta is negative is set to true and exclude zero deltas is set to true so delta should filter out the message.
                new CalculateDeltaTestConfig(true, true, 40, 40, (ctx, msg) -> {
                    verify(ctx).tellSuccess(eq(msg));
                    verify(ctx).getDbCallbackExecutor();
                    verifyNoMoreInteractions(ctx);
                }),
                // delta < 0, tell failure if delta is negative is set to true so it should throw exception.
                new CalculateDeltaTestConfig(true, true, 41, 40, (ctx, msg) -> {
                    var errorCaptor = ArgumentCaptor.forClass(Throwable.class);
                    verify(ctx).tellFailure(eq(msg), errorCaptor.capture());
                    verify(ctx).getDbCallbackExecutor();
                    verifyNoMoreInteractions(ctx);
                    assertThat(errorCaptor.getValue()).isInstanceOf(IllegalArgumentException.class).hasMessage("Delta value is negative!");
                }),
                // delta < 0, exclude zero deltas is set to true so it should return message with delta if delta is negative is set to false.
                new CalculateDeltaTestConfig(false, true, 41, 40, (ctx, msg) -> {
                    var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
                    verify(ctx).tellSuccess(actualMsgCaptor.capture());
                    verify(ctx).getDbCallbackExecutor();
                    verifyNoMoreInteractions(ctx);
                    String expectedMsgData = "{\"temperature\":40.0,\"airPressure\":123,\"delta\":-1}";
                    assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
                }),
                // delta = 0, tell failure if delta is negative is set to false and exclude zero deltas is set to true so delta should filter out the message.
                new CalculateDeltaTestConfig(false, true, 40, 40, (ctx, msg) -> {
                    verify(ctx).tellSuccess(eq(msg));
                    verify(ctx).getDbCallbackExecutor();
                    verifyNoMoreInteractions(ctx);
                }),
                // delta > 0, exclude zero deltas is set to true so it should return message with delta.
                new CalculateDeltaTestConfig(false, true, 39, 40, (ctx, msg) -> {
                    var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
                    verify(ctx).tellSuccess(actualMsgCaptor.capture());
                    verify(ctx).getDbCallbackExecutor();
                    verifyNoMoreInteractions(ctx);
                    String expectedMsgData = "{\"temperature\":40.0,\"airPressure\":123,\"delta\":1}";
                    assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
                }),
                // delta > 0, exclude zero deltas is set to false so it should return message with delta.
                new CalculateDeltaTestConfig(false, false, 39, 40, (ctx, msg) -> {
                    var actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
                    verify(ctx).tellSuccess(actualMsgCaptor.capture());
                    verify(ctx).getDbCallbackExecutor();
                    verifyNoMoreInteractions(ctx);
                    String expectedMsgData = "{\"temperature\":40.0,\"airPressure\":123,\"delta\":1}";
                    assertEquals(expectedMsgData, actualMsgCaptor.getValue().getData());
                })
        );
    }

    private record CalculateDeltaTestConfig(boolean tellFailureIfDeltaIsNegative, boolean excludeZeroDeltas,
                                            double prevValue, double currentValue,
                                            BiConsumer<TbContext, TbMsg> verificationMethod) {
    }

    private void mockFindLatestAsync(TsKvEntry tsKvEntry) {
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        when(timeseriesServiceMock.findLatest(
                eq(TENANT_ID), eq(DUMMY_DEVICE_ORIGINATOR), eq(tsKvEntry.getKey())
        )).thenReturn(Futures.immediateFuture(Optional.of(tsKvEntry)));
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"inputValueKey\":\"pulseCounter\",\"outputValueKey\":\"delta\",\"useCache\":true,\"addPeriodBetweenMsgs\":false, \"periodValueKey\":\"periodInMs\", \"round\":null,\"tellFailureIfDeltaIsNegative\":true}",
                        true,
                        "{\"inputValueKey\":\"pulseCounter\",\"outputValueKey\":\"delta\",\"useCache\":true,\"addPeriodBetweenMsgs\":false, \"periodValueKey\":\"periodInMs\", \"round\":null,\"tellFailureIfDeltaIsNegative\":true, \"excludeZeroDeltas\":false}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\"inputValueKey\":\"pulseCounter\",\"outputValueKey\":\"delta\",\"useCache\":true,\"addPeriodBetweenMsgs\":false, \"periodValueKey\":\"periodInMs\", \"round\":null,\"tellFailureIfDeltaIsNegative\":true, \"excludeZeroDeltas\":false}",
                        false,
                        "{\"inputValueKey\":\"pulseCounter\",\"outputValueKey\":\"delta\",\"useCache\":true,\"addPeriodBetweenMsgs\":false, \"periodValueKey\":\"periodInMs\", \"round\":null,\"tellFailureIfDeltaIsNegative\":true, \"excludeZeroDeltas\":false}")
        );

    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}
