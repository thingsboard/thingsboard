/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class TbMsgDelayNodeTest {

    @Mock
    private TbContext ctxMock;

    private TbMsgDelayNode node;
    private TbMsgDelayNodeConfiguration config;

    private final CustomerId customerId = new CustomerId(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());
    private TbMsg msg;

    @BeforeEach
    public void setUp() {
        node = new TbMsgDelayNode();
        config = new TbMsgDelayNodeConfiguration();
        msg = newMsg();
    }

    @Test
    public void givenDefaultConfiguration_whenInit_thenStartsAndCorrectValuesAreSet() {
        // GIVEN-WHEN
        config = config.defaultConfiguration();

        // THEN
        assertThat(config.getPeriodValue()).isEqualTo(60);
        assertThat(config.getPeriodTimeUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(config.getMaxPendingMsgs()).isEqualTo(1000);
        assertThat(config.getPeriodValuePattern()).isNull();
        assertThat(config.isUsePeriodValuePattern()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 1000, 100000})
    public void givenValidMaxPendingMsgsLimits_whenInit_thenNodeStartsSuccessfully(int maxPendingMsgs) {
        // GIVEN
        config = config.defaultConfiguration();
        config.setMaxPendingMsgs(maxPendingMsgs);

        // WHEN-THEN
        try {
            node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        } catch (TbNodeException e) {
            fail("Node failed to start!", e);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, 0, 1000001, Integer.MAX_VALUE})
    public void givenInvalidMaxPendingMsgsLimits_whenInit_thenNodeFailsToStartWithCorrectException(int maxPendingMsgs) {
        // GIVEN
        config = config.defaultConfiguration();
        config.setMaxPendingMsgs(maxPendingMsgs);

        // WHEN-THEN
        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Invalid maximum pending messages limit [" + maxPendingMsgs + "], " +
                        "should be in range from 1 to 100000 (inclusive)!")
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    @ParameterizedTest
    @EnumSource(value = TimeUnit.class, names = {"MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"})
    public void givenValidPeriodTimeUnits_whenInit_thenNodeStartsSuccessfully(TimeUnit periodTimeUnit) {
        // GIVEN
        config = config.defaultConfiguration();
        config.setPeriodTimeUnit(periodTimeUnit);

        // WHEN-THEN
        try {
            node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        } catch (TbNodeException e) {
            fail("Node failed to start!", e);
        }
    }

    @ParameterizedTest
    @EnumSource(value = TimeUnit.class, mode = EnumSource.Mode.EXCLUDE, names = {"MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"})
    public void givenInvalidPeriodTimeUnits_whenInit_thenNodeFailsToStartWithCorrectException(TimeUnit periodTimeUnit) {
        // GIVEN
        config = config.defaultConfiguration();
        config.setPeriodTimeUnit(periodTimeUnit);

        // WHEN-THEN
        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Unsupported time unit: " + periodTimeUnit)
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    @Test
    public void givenTwoMsgsAndLimitOfOne_whenOnMsg_thenShouldDelayFirstAndFailSecond() {
        // GIVEN
        config = config.defaultConfiguration();
        config.setMaxPendingMsgs(1);

        var selfId = new RuleNodeId(UUID.randomUUID());

        try {
            node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        } catch (TbNodeException e) {
            fail("Node failed to start!", e);
        }

        given(ctxMock.getSelfId()).willReturn(selfId);

        var expectedTickMsg = TbMsg.newMsg(
                null, TbMsgType.DELAY_TIMEOUT_SELF_MSG, selfId,
                msg.getCustomerId(), TbMsgMetaData.EMPTY, msg.getId().toString()
        );
        given(ctxMock.newMsg(
                isNull(), eq(TbMsgType.DELAY_TIMEOUT_SELF_MSG), eq(selfId),
                eq(msg.getCustomerId()), eq(TbMsgMetaData.EMPTY), eq(msg.getId().toString())
        )).willReturn(expectedTickMsg);

        // WHEN 1: first message is received
        var firstMsg = msg;

        node.onMsg(ctxMock, firstMsg);

        // THEN 1: first message is correctly delayed
        ArgumentCaptor<TbMsg> tickMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should(times(1)).tellSelf(tickMsgCaptor.capture(), anyLong());
        assertThat(tickMsgCaptor.getValue()).isEqualTo(expectedTickMsg);
        then(ctxMock).should(times(1)).ack(firstMsg);

        // WHEN 2: second message is received
        var secondMsg = newMsg();

        node.onMsg(ctxMock, secondMsg);

        // THEN 2: seconds message fails because of the limit
        var failedMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        then(ctxMock).should(times(1)).tellFailure(failedMsgCaptor.capture(), exceptionCaptor.capture());
        assertThat(failedMsgCaptor.getValue()).isEqualTo(secondMsg);
        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
        assertThat(exceptionCaptor.getValue()).hasMessage("Max limit of pending messages reached!");

        // WHEN 3: tick message from delayed first message is received
        node.onMsg(ctxMock, expectedTickMsg);

        // THEN 3: correct outbound message is sent
        var outboundMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should(times(1)).enqueueForTellNext(
                outboundMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS)
        );
        var actualOutboundMsg = outboundMsgCaptor.getValue();
        assertThat(actualOutboundMsg.getType()).isEqualTo(firstMsg.getType());
        assertThat(actualOutboundMsg.getOriginator()).isEqualTo(firstMsg.getOriginator());
        assertThat(actualOutboundMsg.getCustomerId()).isEqualTo(firstMsg.getCustomerId());
        assertThat(actualOutboundMsg.getData()).isEqualTo(firstMsg.getData());
        assertThat(actualOutboundMsg.getMetaData()).isEqualTo(firstMsg.getMetaData());
        assertThat(actualOutboundMsg.getQueueName()).isEqualTo(firstMsg.getQueueName());
        assertThat(actualOutboundMsg.getTs()).isEqualTo(firstMsg.getTs());
        assertThat(actualOutboundMsg.getInternalType()).isEqualTo(firstMsg.getInternalType());
        assertThat(actualOutboundMsg.getDataType()).isEqualTo(firstMsg.getDataType());
        assertThat(actualOutboundMsg.getRuleChainId()).isEqualTo(firstMsg.getRuleChainId());
        assertThat(actualOutboundMsg.getRuleNodeId()).isEqualTo(firstMsg.getRuleNodeId());
        assertThat(actualOutboundMsg.isValid()).isEqualTo(firstMsg.isValid());
        assertThat(actualOutboundMsg.getMetaDataTs()).isEqualTo(firstMsg.getMetaDataTs());
        assertThat(actualOutboundMsg.getAndIncrementRuleNodeCounter()).isEqualTo(firstMsg.getAndIncrementRuleNodeCounter());
    }

    private TbMsg newMsg() {
        var data = JacksonUtil.newObjectNode();
        data.put("humidity", 58.3);
        return TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, customerId, new TbMsgMetaData(), JacksonUtil.toString(data));
    }

    @Test
    public void givenThirtySecondsDelayWithNoPattern_whenOnMsg_thenShouldUseCorrectDelay() {
        // GIVEN
        config = config.defaultConfiguration();
        config.setPeriodValue(30);

        try {
            node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        } catch (TbNodeException e) {
            fail("Node failed to start!", e);
        }

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should(times(1)).tellSelf(any(), eq(30000L));
    }

    @Test
    public void givenOneHourDelayWithPattern_whenOnMsg_thenShouldUseCorrectDelay() {
        // GIVEN
        config = config.defaultConfiguration();
        config.setPeriodTimeUnit(TimeUnit.HOURS);
        config.setUsePeriodValuePattern(true);
        config.setPeriodValuePattern("$[processingDelayInHours]");

        var data = JacksonUtil.newObjectNode();
        data.put("processingDelayInHours", 1);
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, customerId, new TbMsgMetaData(), JacksonUtil.toString(data));

        try {
            node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        } catch (TbNodeException e) {
            fail("Node failed to start!", e);
        }

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should(times(1)).tellSelf(any(), eq(Duration.ofHours(1).toMillis()));
    }

    @Test
    public void givenNotParsableDelayWithPattern_whenOnMsg_thenShouldThrowException() {
        // GIVEN
        config = config.defaultConfiguration();
        config.setPeriodTimeUnit(TimeUnit.HOURS);
        config.setUsePeriodValuePattern(true);
        config.setPeriodValuePattern("$[processingDelayInHours]");

        var data = JacksonUtil.newObjectNode();
        data.put("processingDelayInHours", "one hour");
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, customerId, new TbMsgMetaData(), JacksonUtil.toString(data));

        try {
            node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        } catch (TbNodeException e) {
            fail("Node failed to start!", e);
        }

        // WHEN-THEN
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Can't parse period value using pattern: $[processingDelayInHours]");
    }

    @ParameterizedTest
    @MethodSource("provideOldConfigurations")
    public void givenOldConfigurations_whenUpgrade_thenShouldCorrectlyUpgrade(
            String oldConfigurationStr, int fromVersion, String expectedUpgradedConfigurationStr
    ) {
        // GIVEN
        var oldConfigurationNode = JacksonUtil.toJsonNode(oldConfigurationStr);

        // WHEN
        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(fromVersion, oldConfigurationNode);

        // THEN
        boolean actualHasChanges = upgradeResult.getFirst();
        assertThat(actualHasChanges).isTrue();

        var actualUpgradedConfiguration = JacksonUtil.treeToValue(upgradeResult.getSecond(), TbMsgDelayNodeConfiguration.class);
        var expectedUpgradedConfiguration = JacksonUtil.fromString(expectedUpgradedConfigurationStr, TbMsgDelayNodeConfiguration.class);
        assertThat(actualUpgradedConfiguration).isEqualTo(expectedUpgradedConfiguration);
    }

    private static Stream<Arguments> provideOldConfigurations() {
        return Stream.of(
                Arguments.of("{\"periodInSeconds\":60,\"maxPendingMsgs\":1000,\"periodInSecondsPattern\":null,\"useMetadataPeriodInSecondsPatterns\":false}", 0, "{\"periodValue\":60,\"periodTimeUnit\":\"SECONDS\",\"maxPendingMsgs\":1000,\"periodValuePattern\":null,\"usePeriodValuePattern\":false}"),
                Arguments.of("{\"periodInSeconds\":60,\"maxPendingMsgs\":1000,\"periodInSecondsPattern\":\"${metadataKey}\",\"useMetadataPeriodInSecondsPatterns\":true}", 0, "{\"periodValue\":60,\"periodTimeUnit\":\"SECONDS\",\"maxPendingMsgs\":1000,\"periodValuePattern\":\"${metadataKey}\",\"usePeriodValuePattern\":true}")
        );
    }

}
