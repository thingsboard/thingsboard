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
package org.thingsboard.rule.engine.delay;

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
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willAnswer;

@ExtendWith(MockitoExtension.class)
public class TbMsgDelayNodeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("20107cf0-1c5e-4ac4-8131-7c466c955a7c"));

    private TbMsgDelayNode node;
    private TbMsgDelayNodeConfiguration config;

    @Mock
    private TbContext ctxMock;

    @BeforeEach
    public void setUp() {
        node = spy(new TbMsgDelayNode());
        config = new TbMsgDelayNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getPeriodInSeconds()).isEqualTo(60);
        assertThat(config.getMaxPendingMsgs()).isEqualTo(1000);
        assertThat(config.isUseMetadataPeriodInSecondsPatterns()).isFalse();
        assertThat(config.getPeriodInSecondsPattern()).isNull();
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))));
    }

    @ParameterizedTest
    @MethodSource
    public void givenPeriodInSecondsPattern_whenOnMsg_thenTellSelfTickMsgAndEnqueueForTellNext(
            String periodInSecondsPattern, TbMsgMetaData metaData, String data, long expectedDelay) throws TbNodeException {
        config.setUseMetadataPeriodInSecondsPatterns(true);
        config.setPeriodInSecondsPattern(periodInSecondsPattern);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, data);
        RuleNodeId ruleNodeId = new RuleNodeId(UUID.fromString("5236e9b9-1e29-4b95-b219-7043ff8f0414"));
        TbMsg tickMsg = TbMsg.newMsg(TbMsgType.DELAY_TIMEOUT_SELF_MSG, ruleNodeId, TbMsgMetaData.EMPTY, msg.getId().toString());

        given(ctxMock.getSelfId()).willReturn(ruleNodeId);
        given(ctxMock.newMsg(any(), any(TbMsgType.class), any(), any(), any(), any())).willReturn(tickMsg);
        willAnswer(invocation -> {
            node.onMsg(ctxMock, invocation.getArgument(0));
            return null;
        }).given(ctxMock).tellSelf(tickMsg, expectedDelay);

        node.onMsg(ctxMock, msg);

        then(ctxMock).should().newMsg(null, TbMsgType.DELAY_TIMEOUT_SELF_MSG, ruleNodeId, null, TbMsgMetaData.EMPTY, msg.getId().toString());
        then(ctxMock).should().tellSelf(tickMsg, expectedDelay);
        then(ctxMock).should().ack(msg);
        then(node).should().onMsg(ctxMock, tickMsg);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().enqueueForTellNext(actualMsg.capture(), eq(TbNodeConnectionType.SUCCESS));
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("id", "ts").isEqualTo(msg);
    }

    private static Stream<Arguments> givenPeriodInSecondsPattern_whenOnMsg_thenTellSelfTickMsgAndEnqueueForTellNext() {
        return Stream.of(
                Arguments.of("${md-period-in-seconds}", new TbMsgMetaData(Map.of("md-period-in-seconds", "10")), TbMsg.EMPTY_JSON_OBJECT, 10000L),
                Arguments.of("$[msg-period-in-seconds]", TbMsgMetaData.EMPTY, "{\"msg-period-in-seconds\":5}", 5000L)
        );
    }

    @Test
    public void givenPeriodInSecondsPatternIsUnparsable_whenOnMsg_thenThrowsException() throws TbNodeException {
        config.setUseMetadataPeriodInSecondsPatterns(true);
        config.setPeriodInSecondsPattern("$[msg-period-in-seconds]");
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, "{\"msg-period-in-seconds\":\"five\"}");
        RuleNodeId ruleNodeId = new RuleNodeId(UUID.fromString("5236e9b9-1e29-4b95-b219-7043ff8f0414"));
        TbMsg tickMsg = TbMsg.newMsg(TbMsgType.DELAY_TIMEOUT_SELF_MSG, ruleNodeId, TbMsgMetaData.EMPTY, msg.getId().toString());

        given(ctxMock.getSelfId()).willReturn(ruleNodeId);
        given(ctxMock.newMsg(any(), any(TbMsgType.class), any(), any(), any(), any())).willReturn(tickMsg);

        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Can't parse period in seconds from metadata using pattern: $[msg-period-in-seconds]");
    }

    @Test
    public void givenMaxLimitOfPendingMsgsReached_whenOnMsg_thenTellFailure() throws TbNodeException {
        int maxPendingMsgs = 5;
        config.setMaxPendingMsgs(maxPendingMsgs);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        RuleNodeId ruleNodeId = new RuleNodeId(UUID.fromString("d1440f09-ca81-41f3-b67e-1495aee87dc6"));
        String msgId = "e38c87c5-8916-4bb0-b448-b8d08ad639df";
        TbMsg tickMsg = TbMsg.newMsg(TbMsgType.DELAY_TIMEOUT_SELF_MSG, ruleNodeId, TbMsgMetaData.EMPTY, msgId);

        given(ctxMock.getSelfId()).willReturn(ruleNodeId);
        given(ctxMock.newMsg(any(), any(TbMsgType.class), any(), any(), any(), any())).willReturn(tickMsg);

        for (int i = 0; i < 6; i++) {
            TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, "msg : " + (i + 1));
            node.onMsg(ctxMock, msg);
        }

        then(ctxMock).should(times(maxPendingMsgs)).newMsg(isNull(), eq(TbMsgType.DELAY_TIMEOUT_SELF_MSG), eq(ruleNodeId), isNull(), eq(TbMsgMetaData.EMPTY), any());
        then(ctxMock).should(times(maxPendingMsgs)).tellSelf(tickMsg, 60000L);
        then(ctxMock).should(times(maxPendingMsgs)).ack(any());
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> throwable = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(actualMsg.capture(), throwable.capture());
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, "msg : 6");
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("id", "ts").isEqualTo(msg);
        assertThat(throwable.getValue()).isInstanceOf(RuntimeException.class).hasMessage("Max limit of pending messages reached!");
    }

    @Test
    public void givenNumberOfMsgsMoreThenMaxPendingMsgs_whenOnMsg_thenTellSelfTickMsgAndEnqueueForTellNext() throws TbNodeException {
        config.setMaxPendingMsgs(3);
        config.setPeriodInSeconds(1);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        RuleNodeId ruleNodeId = new RuleNodeId(UUID.fromString("e8172ef8-bf91-4821-b9f5-ccd7b865e418"));
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        TbMsg tickMsg = TbMsg.newMsg(TbMsgType.DELAY_TIMEOUT_SELF_MSG, ruleNodeId, TbMsgMetaData.EMPTY, msg.getId().toString());

        given(ctxMock.getSelfId()).willReturn(ruleNodeId);
        given(ctxMock.newMsg(any(), any(TbMsgType.class), any(), any(), any(), any())).willReturn(tickMsg);
        willAnswer(invocation -> {
            node.onMsg(ctxMock, invocation.getArgument(0));
            return null;
        }).given(ctxMock).tellSelf(tickMsg, 1000L);

        for (int i = 0; i < 9; i++) {
            node.onMsg(ctxMock, msg);
        }

        then(ctxMock).should(times(9)).newMsg(null, TbMsgType.DELAY_TIMEOUT_SELF_MSG, ruleNodeId, null, TbMsgMetaData.EMPTY, msg.getId().toString());
        then(ctxMock).should(times(9)).tellSelf(tickMsg, 1000L);
        then(ctxMock).should(times(9)).ack(msg);
        then(node).should(times(9)).onMsg(ctxMock, tickMsg);
        then(ctxMock).should(times(9)).enqueueForTellNext(any(TbMsg.class), eq(TbNodeConnectionType.SUCCESS));
    }

    @Test
    public void verifyDestroyMethod() {
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        var pendingMsgs = new HashMap<UUID, TbMsg>();
        pendingMsgs.put(UUID.fromString("321f0301-9bed-4e7d-b92f-a978f53ec5d6"), msg);
        ReflectionTestUtils.setField(node, "pendingMsgs", pendingMsgs);
        var actualPendingMsgs = (Map<UUID, TbMsg>) ReflectionTestUtils.getField(node, "pendingMsgs");
        assertThat(actualPendingMsgs).isEqualTo(pendingMsgs);

        node.destroy();

        assertThat(actualPendingMsgs).isEmpty();
    }
}
