/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.rule.engine.telemetry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.telemetry.strategy.ProcessingStrategy;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.ConstraintValidator;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.thingsboard.rule.engine.api.AttributesSaveRequest.Strategy;
import static org.thingsboard.rule.engine.api.AttributesSaveRequest.builder;
import static org.thingsboard.rule.engine.telemetry.settings.AttributesProcessingSettings.Advanced;
import static org.thingsboard.rule.engine.telemetry.settings.AttributesProcessingSettings.Deduplicate;
import static org.thingsboard.rule.engine.telemetry.settings.AttributesProcessingSettings.OnEveryMessage;
import static org.thingsboard.rule.engine.telemetry.settings.AttributesProcessingSettings.WebSocketsOnly;
import static org.thingsboard.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;

@ExtendWith(MockitoExtension.class)
class TbMsgAttributesNodeTest extends AbstractRuleNodeUpgradeTest {

    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("6c18691e-4470-4766-9739-aface71d761f"));
    final DeviceId deviceId = new DeviceId(UUID.fromString("b66159d7-c77e-45e8-bb41-a8f557f434c1"));

    @Spy
    TbMsgAttributesNode node;
    TbMsgAttributesNodeConfiguration config;

    @Mock
    TbContext ctxMock;
    @Mock
    AttributesService attributesServiceMock;
    @Mock
    RuleEngineTelemetryService telemetryServiceMock;

    @BeforeEach
    void setUp() {
        lenient().when(ctxMock.getTenantId()).thenReturn(tenantId);
        lenient().when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        lenient().when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);

        config = new TbMsgAttributesNodeConfiguration().defaultConfiguration();
    }

    @Test
    void verifyDefaultConfig() {
        assertThat(config.getProcessingSettings()).isInstanceOf(OnEveryMessage.class);
        assertThat(config.getScope()).isEqualTo("SERVER_SCOPE");
        assertThat(config.isNotifyDevice()).isFalse();
        assertThat(config.isSendAttributesUpdatedNotification()).isFalse();
        assertThat(config.isUpdateAttributesOnlyOnValueChange()).isTrue();
    }

    @Test
    void givenProcessingSettingsAreNull_whenValidatingConstraints_thenThrowsException() {
        // GIVEN
        config.setProcessingSettings(null);

        // WHEN-THEN
        assertThatThrownBy(() -> ConstraintValidator.validateFields(config))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Validation error: processingSettings must not be null");
    }

    @Test
    void givenOnEveryMessageProcessingSettingsAndSameMessageTwoTimes_whenOnMsg_thenPersistSameMessageTwoTimes() throws TbNodeException {
        // GIVEN
        config.setUpdateAttributesOnlyOnValueChange(false);
        config.setProcessingSettings(new OnEveryMessage());

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of(NOTIFY_DEVICE_METADATA_KEY, "false")))
                .build();

        // WHEN-THEN
        var expectedSaveRequest = builder()
                .tenantId(tenantId)
                .entityId(msg.getOriginator())
                .scope(AttributeScope.valueOf(config.getScope()))
                .entry(new DoubleDataEntry("temperature", 22.3))
                .notifyDevice(false)
                .strategy(Strategy.PROCESS_ALL)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .build();

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(1)).saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest)
                        .usingRecursiveComparison()
                        .ignoringFields("callback", "entries.lastUpdateTs")
                        .isEqualTo(expectedSaveRequest)
        ));

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(2)).saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest)
                        .usingRecursiveComparison()
                        .ignoringFields("callback", "entries.lastUpdateTs")
                        .isEqualTo(expectedSaveRequest)
        ));
    }

    @Test
    void givenDeduplicateProcessingSettingsAndSameMessageTwoTimes_whenOnMsg_thenPersistThisMessageOnlyFirstTime() throws TbNodeException {
        // GIVEN
        config.setUpdateAttributesOnlyOnValueChange(false);
        config.setProcessingSettings(new Deduplicate(10));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of(NOTIFY_DEVICE_METADATA_KEY, "false")))
                .build();

        // WHEN-THEN
        var expectedSaveRequest = builder()
                .tenantId(tenantId)
                .entityId(msg.getOriginator())
                .scope(AttributeScope.valueOf(config.getScope()))
                .entry(new DoubleDataEntry("temperature", 22.3))
                .notifyDevice(false)
                .strategy(Strategy.PROCESS_ALL)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .build();

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should().saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest)
                        .usingRecursiveComparison()
                        .ignoringFields("callback", "entries.lastUpdateTs")
                        .isEqualTo(expectedSaveRequest)
        ));

        clearInvocations(telemetryServiceMock, ctxMock);

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(never()).saveAttributes(any());
    }

    @Test
    void givenWebSocketsOnlyProcessingSettingsAndSameMessageTwoTimes_whenOnMsg_thenSendsOnlyWsUpdateTwoTimes() throws TbNodeException {
        // GIVEN
        config.setUpdateAttributesOnlyOnValueChange(false);
        config.setProcessingSettings(new WebSocketsOnly());

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of(NOTIFY_DEVICE_METADATA_KEY, "false")))
                .build();

        // WHEN-THEN
        var expectedSaveRequest = builder()
                .tenantId(tenantId)
                .entityId(msg.getOriginator())
                .scope(AttributeScope.valueOf(config.getScope()))
                .entry(new DoubleDataEntry("temperature", 22.3))
                .notifyDevice(false)
                .strategy(Strategy.WS_ONLY)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .build();

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(1)).saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest)
                        .usingRecursiveComparison()
                        .ignoringFields("callback", "entries.lastUpdateTs")
                        .isEqualTo(expectedSaveRequest)
        ));

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(2)).saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest)
                        .usingRecursiveComparison()
                        .ignoringFields("callback", "entries.lastUpdateTs")
                        .isEqualTo(expectedSaveRequest)
        ));
    }

    @Test
    void givenAdvancedProcessingSettingsWithOnEveryMessageStrategiesForAllActionsAndSameMessageTwoTimes_whenOnMsg_thenPersistSameMessageTwoTimes() throws TbNodeException {
        // GIVEN
        config.setUpdateAttributesOnlyOnValueChange(false);
        config.setProcessingSettings(new Advanced(
                ProcessingStrategy.onEveryMessage(),
                ProcessingStrategy.onEveryMessage(),
                ProcessingStrategy.onEveryMessage()
        ));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of(NOTIFY_DEVICE_METADATA_KEY, "false")))
                .build();

        // WHEN-THEN
        var expectedSaveRequest = builder()
                .tenantId(tenantId)
                .entityId(msg.getOriginator())
                .scope(AttributeScope.valueOf(config.getScope()))
                .entry(new DoubleDataEntry("temperature", 22.3))
                .notifyDevice(false)
                .strategy(Strategy.PROCESS_ALL)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .build();

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(1)).saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest)
                        .usingRecursiveComparison()
                        .ignoringFields("callback", "entries.lastUpdateTs")
                        .isEqualTo(expectedSaveRequest)
        ));

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(2)).saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest)
                        .usingRecursiveComparison()
                        .ignoringFields("callback", "entries.lastUpdateTs")
                        .isEqualTo(expectedSaveRequest)
        ));
    }

    @Test
    void givenAdvancedProcessingSettingsWithDifferentDeduplicateStrategyForEachAction_whenOnMsg_thenEvaluatesStrategiesForEachActionsIndependently() throws TbNodeException {
        // GIVEN
        config.setUpdateAttributesOnlyOnValueChange(false);
        config.setProcessingSettings(new Advanced(
                ProcessingStrategy.deduplicate(1),
                ProcessingStrategy.deduplicate(2),
                ProcessingStrategy.deduplicate(3)
        ));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        long ts1 = 500L;
        long ts2 = 1500L;
        long ts3 = 2500L;
        long ts4 = 3500L;

        // WHEN-THEN
        node.onMsg(ctxMock, TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", Long.toString(ts1))))
                .build());
        then(telemetryServiceMock).should().saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(Strategy.PROCESS_ALL)
        ));

        clearInvocations(telemetryServiceMock);

        node.onMsg(ctxMock, TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", Long.toString(ts2))))
                .build());
        then(telemetryServiceMock).should().saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(new Strategy(true, false, false))
        ));

        clearInvocations(telemetryServiceMock);

        node.onMsg(ctxMock, TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", Long.toString(ts3))))
                .build());
        then(telemetryServiceMock).should().saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(new Strategy(true, true, false))
        ));

        clearInvocations(telemetryServiceMock);

        node.onMsg(ctxMock, TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", Long.toString(ts4))))
                .build());
        then(telemetryServiceMock).should().saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(new Strategy(true, false, true))
        ));
    }

    @Test
    public void givenAdvancedProcessingSettingsWithSkipStrategiesForAllActionsAndSameMessageTwoTimes_whenOnMsg_thenSkipsSameMessageTwoTimes() throws TbNodeException {
        // GIVEN
        config.setProcessingSettings(new Advanced(
                ProcessingStrategy.skip(),
                ProcessingStrategy.skip(),
                ProcessingStrategy.skip()
        ));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of(NOTIFY_DEVICE_METADATA_KEY, "false")))
                .build();

        // WHEN-THEN
        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(never()).saveAttributes(any());
        then(ctxMock).should(times(1)).tellSuccess(msg);

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(never()).saveAttributes(any());
        then(ctxMock).should(times(2)).tellSuccess(msg);
    }

    @Test
    void givenVariousChangesToAttributes_whenUpdateOnlyOnValueChangeEnabled_thenShouldCorrectlyFilterChangedAttributes() throws TbNodeException {
        // GIVEN
        config.setUpdateAttributesOnlyOnValueChange(true);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        List<AttributeKvEntry> currentAttributes = List.of(
                new BaseAttributeKvEntry(123L, new StringDataEntry("address", "Prospect Beresteiskyi 1")),
                new BaseAttributeKvEntry(123L, new BooleanDataEntry("valid", true)),
                new BaseAttributeKvEntry(123L, new LongDataEntry("counter", 100L)),
                new BaseAttributeKvEntry(123L, new DoubleDataEntry("temp", -18.35)),
                new BaseAttributeKvEntry(123L, new JsonDataEntry("json", "{\"warning\":\"out of paper\"}"))
        );
        given(attributesServiceMock.find(eq(tenantId), eq(deviceId), eq(AttributeScope.valueOf(config.getScope())), anyList())).willReturn(immediateFuture(currentAttributes));

        var data = JacksonUtil.newObjectNode()
                .put("address", "Prospect Beresteiskyi 1") // no changes
                .put("valid", "false") // type and value changed
                .put("counter", 101L) // value changed
                .put("temp", -18.35) // no changes
                .put("json", "{\"warning\":\"out of paper\"}") // only type changed
                .put("newKey", "newValue"); // new attribute

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(data.toString())
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        List<AttributeKvEntry> expectedChangedAttributes = List.of(
                new BaseAttributeKvEntry(456L, new StringDataEntry("valid", "false")),
                new BaseAttributeKvEntry(456L, new LongDataEntry("counter", 101L)),
                new BaseAttributeKvEntry(456L, new StringDataEntry("json", "{\"warning\":\"out of paper\"}")),
                new BaseAttributeKvEntry(456L, new StringDataEntry("newKey", "newValue"))
        );

        then(telemetryServiceMock).should().saveAttributes(assertArg(request ->
                assertThat(request.getEntries())
                        .usingRecursiveComparison()
                        .ignoringCollectionOrder()
                        .ignoringFields("lastUpdateTs")
                        .isEqualTo(expectedChangedAttributes)
        ));
    }

    @Test
    void givenNoChangesToAttributes_whenUpdateOnlyOnValueChangeEnabled_thenShouldNotCallSaveAndJustTellSuccess() throws TbNodeException {
        // GIVEN
        config.setUpdateAttributesOnlyOnValueChange(true);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        List<AttributeKvEntry> currentAttributes = List.of(
                new BaseAttributeKvEntry(123L, new StringDataEntry("address", "Prospect Beresteiskyi 1")),
                new BaseAttributeKvEntry(123L, new BooleanDataEntry("valid", true)),
                new BaseAttributeKvEntry(123L, new LongDataEntry("counter", 100L))
        );
        given(attributesServiceMock.find(eq(tenantId), eq(deviceId), eq(AttributeScope.valueOf(config.getScope())), anyList())).willReturn(immediateFuture(currentAttributes));

        var data = JacksonUtil.newObjectNode()
                .put("address", "Prospect Beresteiskyi 1")
                .put("valid", true)
                .put("counter", 100L);

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(data.toString())
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(telemetryServiceMock).shouldHaveNoInteractions();
        then(ctxMock).should().tellSuccess(msg);
    }

    // Notify device backward-compatibility test
    @ParameterizedTest
    @MethodSource
    void givenVariousValuesForNotifyDeviceInMetadata_thenShouldCorrectlyParseValueFromMetadata(String mdValue, boolean expectedArgumentValue) throws TbNodeException {
        // GIVEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        given(attributesServiceMock.find(tenantId, deviceId, AttributeScope.valueOf(config.getScope()), List.of("mode"))).willReturn(
                immediateFuture(List.of(new BaseAttributeKvEntry(123L, new StringDataEntry("mode", "tilt"))))
        );

        var metadata = new TbMsgMetaData();
        metadata.putValue(NOTIFY_DEVICE_METADATA_KEY, mdValue);

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("mode", "vibration").toString())
                .metaData(metadata)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(telemetryServiceMock).should().saveAttributes(assertArg(request -> assertThat(request.isNotifyDevice()).isEqualTo(expectedArgumentValue)));
    }

    // Notify device backward-compatibility test arguments
    static Stream<Arguments> givenVariousValuesForNotifyDeviceInMetadata_thenShouldCorrectlyParseValueFromMetadata() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of("null", false),
                Arguments.of("true", true),
                Arguments.of("false", false)
        );
    }

    // Rule nodes upgrade
    static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        """
                                {
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": "false",
                                  "sendAttributesUpdatedNotification": "false"
                                }
                                """,
                        true,
                        """
                                {
                                  "processingSettings": {
                                    "type": "ON_EVERY_MESSAGE"
                                  },
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": false,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": false
                                }
                                """
                ),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        """
                                {
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": false,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """,
                        true,
                        """
                                {
                                  "processingSettings": {
                                    "type": "ON_EVERY_MESSAGE"
                                  },
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": false,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """
                ),
                // all flags are booleans
                Arguments.of(1,
                        """
                                {
                                  "scope": "SHARED_SCOPE",
                                  "notifyDevice": true,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """,
                        true,
                        """
                                {
                                  "processingSettings": {
                                    "type": "ON_EVERY_MESSAGE"
                                  },
                                  "scope": "SHARED_SCOPE",
                                  "notifyDevice": true,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """
                ),
                // no boolean flags set
                Arguments.of(1,
                        """
                                {
                                  "scope": "CLIENT_SCOPE"
                                }
                                """,
                        true,
                        """
                                {
                                  "processingSettings": {
                                    "type": "ON_EVERY_MESSAGE"
                                  },
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": true,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """
                ),
                // all flags are boolean strings
                Arguments.of(1,
                        """
                                {
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": "false",
                                  "sendAttributesUpdatedNotification": "false",
                                  "updateAttributesOnlyOnValueChange": "true"
                                }
                                """,
                        true,
                        """
                                {
                                  "processingSettings": {
                                    "type": "ON_EVERY_MESSAGE"
                                  },
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": false,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """
                ),
                // at least one flag is boolean string
                Arguments.of(1,
                        """
                                {
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": "false",
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """,
                        true,
                        """
                                {
                                  "processingSettings": {
                                    "type": "ON_EVERY_MESSAGE"
                                  },
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": false,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """
                ),
                // notify device flag is null
                Arguments.of(1,
                        """
                                {
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": "null",
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """,
                        true,
                        """
                                {
                                  "processingSettings": {
                                    "type": "ON_EVERY_MESSAGE"
                                  },
                                  "scope": "CLIENT_SCOPE",
                                  "notifyDevice": true,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """
                ),
                // default config for version 2
                Arguments.of(2,
                        """
                                {
                                  "scope": "SERVER_SCOPE",
                                  "notifyDevice": false,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
                                }
                                """,
                        true,
                        """
                                {
                                  "processingSettings": {
                                    "type": "ON_EVERY_MESSAGE"
                                  },
                                  "scope": "SERVER_SCOPE",
                                  "notifyDevice": false,
                                  "sendAttributesUpdatedNotification": false,
                                  "updateAttributesOnlyOnValueChange": true
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
