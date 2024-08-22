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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;

@ExtendWith(MockitoExtension.class)
public class TbMsgDelayNodeTest extends AbstractRuleNodeUpgradeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("20107cf0-1c5e-4ac4-8131-7c466c955a7c"));
    private final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("1be24225-b669-4b26-ab7e-083aaa82d0a0"));

    private final Set<TimeUnit> supportedTimeUnits = EnumSet.of(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS);
    private final String supportedTimeUnitsStr = String.join(",", TimeUnit.SECONDS.name(), TimeUnit.MINUTES.name(), TimeUnit.HOURS.name());

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
        assertThat(config.getPeriod()).isEqualTo("60");
        assertThat(config.getMaxPendingMsgs()).isEqualTo(1000);
        assertThat(config.getTimeUnit()).isEqualTo(TimeUnit.SECONDS.name());
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 5000000})
    public void givenInvalidMaxPendingMsgsValue_whenInit_thenThrowsException(int maxPendingMsgs) {
        config.setMaxPendingMsgs(maxPendingMsgs);

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Maximum pending messages should be in a range from 1 to 100000.")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    @Test
    public void givenPeriodIsNull_whenInit_thenThrowsException() {
        config.setPeriod(null);

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Period should be specified.")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    @Test
    public void givenTimeUnitIsNull_whenInit_thenThrowsException() {
        config.setTimeUnit(null);

        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Time unit should be specified.")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    @ParameterizedTest
    @MethodSource
    public void givenPeriodValueAndPeriodTimeUnitPatterns_whenOnMsg_thenTellSelfTickMsgAndEnqueueForTellNext(
            String periodPattern, String timeUnitPattern, TbMsgMetaData metaData, String data, long expectedDelay) throws TbNodeException {
        config.setPeriod(periodPattern);
        config.setTimeUnit(timeUnitPattern);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, data);
        var tickMsg = TbMsg.newMsg(TbMsgType.DELAY_TIMEOUT_SELF_MSG, RULE_NODE_ID, TbMsgMetaData.EMPTY, msg.getId().toString());

        given(ctxMock.newMsg(any(), any(TbMsgType.class), any(), any(), any(), any())).willReturn(tickMsg);
        given(ctxMock.getSelfId()).willReturn(RULE_NODE_ID);
        willAnswer(invocation -> {
            node.onMsg(ctxMock, invocation.getArgument(0));
            return null;
        }).given(ctxMock).tellSelf(any(TbMsg.class), any(Long.class));

        node.onMsg(ctxMock, msg);

        then(ctxMock).should().tellSelf(tickMsg, expectedDelay);
        then(ctxMock).should().ack(msg);
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().enqueueForTellNext(actualMsg.capture(), eq(TbNodeConnectionType.SUCCESS));
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("id", "ts").isEqualTo(msg);
    }

    private static Stream<Arguments> givenPeriodValueAndPeriodTimeUnitPatterns_whenOnMsg_thenTellSelfTickMsgAndEnqueueForTellNext() {
        return Stream.of(
                Arguments.of("1", "HOURS", TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT, TimeUnit.HOURS.toMillis(1L)),
                Arguments.of("${md-period}", "${md-time-unit}",
                        new TbMsgMetaData(Map.of(
                                "md-period", "5",
                                "md-time-unit", "MINUTES"
                        )), TbMsg.EMPTY_JSON_OBJECT, TimeUnit.MINUTES.toMillis(5L)),
                Arguments.of("$[msg-period]", "$[msg-time-unit]", TbMsgMetaData.EMPTY,
                        "{\"msg-period\":10,\"msg-time-unit\":\"SECONDS\"}", TimeUnit.SECONDS.toMillis(10L))
        );
    }

    @ParameterizedTest
    @EnumSource(TimeUnit.class)
    public void givenTimeUnit_whenOnMsg_thenVerify(TimeUnit timeUnit) throws TbNodeException {
        config.setTimeUnit(timeUnit.name());
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        if (supportedTimeUnits.contains(timeUnit)) {
            assertThatNoException().isThrownBy(() -> node.onMsg(ctxMock, msg));
        } else {
            assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Time unit '" + timeUnit + "' is not supported! Only " + supportedTimeUnitsStr + " are supported.");
        }
    }

    @Test
    public void givenPeriodIsUnparsable_whenOnMsg_thenThrowsException() throws TbNodeException {
        config.setPeriod("five");
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(NumberFormatException.class)
                .hasMessage("Can't parse period value : five");
    }

    @Test
    public void givenInvalidTimeUnit_whenOnMsg_thenThrowsException() throws TbNodeException {
        config.setTimeUnit("sec");
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid value for period time unit : sec");
    }

    @Test
    public void givenMaxLimitOfPendingMsgsReached_whenOnMsg_thenTellFailure() throws TbNodeException {
        config.setMaxPendingMsgs(1);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        for (int i = 0; i < 2; i++) {
            node.onMsg(ctxMock, msg);
        }

        ArgumentCaptor<Throwable> throwable = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), throwable.capture());
        assertThat(throwable.getValue()).isInstanceOf(RuntimeException.class).hasMessage("Max limit of pending messages reached!");
    }

    @Test
    public void verifyDestroyMethod() {
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        var pendingMsgs = new ConcurrentHashMap<>();
        pendingMsgs.put(UUID.fromString("321f0301-9bed-4e7d-b92f-a978f53ec5d6"), msg);
        ReflectionTestUtils.setField(node, "pendingMsgs", pendingMsgs);
        var actualPendingMsgs = (Map<UUID, TbMsg>) ReflectionTestUtils.getField(node, "pendingMsgs");
        assertThat(actualPendingMsgs).isEqualTo(pendingMsgs);

        node.destroy();

        assertThat(actualPendingMsgs).isEmpty();
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // config for version 1 with upgrade from version 0 (useMetadataPeriodInSecondsPatterns does not exist)
                Arguments.of(0,
                        """
                                    {
                                        "periodInSeconds": 13,
                                        "maxPendingMsgs": 1000,
                                        "periodInSecondsPattern": "17"
                                    }
                                """,
                        true,
                        """
                                    {
                                        "period": "60",
                                        "timeUnit": "SECONDS",
                                        "maxPendingMsgs": 1000
                                    }
                                """
                ),
                // config for version 1 with upgrade from version 0 (useMetadataPeriodInSecondsPatterns is false)
                Arguments.of(0,
                        """
                                    {
                                        "periodInSeconds": 60,
                                        "maxPendingMsgs": 1000,
                                        "periodInSecondsPattern": null,
                                        "useMetadataPeriodInSecondsPatterns": false
                                    }
                                """,
                        true,
                        """
                                    {
                                        "period": "60",
                                        "timeUnit": "SECONDS",
                                        "maxPendingMsgs": 1000
                                    }
                                """
                ),
                // config for version 1 with upgrade from version 0 (useMetadataPeriodInSecondsPattern is true)
                Arguments.of(0,
                        """
                                    {
                                        "periodInSeconds": 60,
                                        "maxPendingMsgs": 1000,
                                        "periodInSecondsPattern": "${period-pattern}",
                                        "useMetadataPeriodInSecondsPatterns": true
                                    }
                                """,
                        true,
                        """
                                    {
                                        "period": "${period-pattern}",
                                        "timeUnit": "SECONDS",
                                        "maxPendingMsgs": 1000
                                    }
                                """
                ),
                // config for version 1 with upgrade from version 0 (hasChanges is false)
                Arguments.of(0,
                        """
                                    {
                                        "period": "${period-pattern}",
                                        "timeUnit": "SECONDS",
                                        "maxPendingMsgs": 1000
                                    }
                                """,
                        false,
                        """
                                    {
                                        "period": "${period-pattern}",
                                        "timeUnit": "SECONDS",
                                        "maxPendingMsgs": 1000
                                    }
                                """
                )
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}
