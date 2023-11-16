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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
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
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TbMsgAttributesNodeTest {

    private static final DeviceId ORIGINATOR_ID = new DeviceId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());

    final String updateAttributesOnlyOnValueChangeKey = "updateAttributesOnlyOnValueChange";

    @Test
    void testFilterChangedAttr_whenCurrentAttributesEmpty_thenReturnNewAttributes() {
        TbMsgAttributesNode node = spy(TbMsgAttributesNode.class);
        List<AttributeKvEntry> newAttributes = new ArrayList<>();

        List<AttributeKvEntry> filtered = node.filterChangedAttr(Collections.emptyList(), newAttributes);
        assertThat(filtered).isSameAs(newAttributes);
    }

    @Test
    void testFilterChangedAttr_whenCurrentAttributesContainsInAnyOrderNewAttributes_thenReturnEmptyList() {
        TbMsgAttributesNode node = spy(TbMsgAttributesNode.class);
        List<AttributeKvEntry> currentAttributes = List.of(
                new BaseAttributeKvEntry(1694000000L, new StringDataEntry("address", "Peremohy ave 1")),
                new BaseAttributeKvEntry(1694000000L, new BooleanDataEntry("valid", true)),
                new BaseAttributeKvEntry(1694000000L, new LongDataEntry("counter", 100L)),
                new BaseAttributeKvEntry(1694000000L, new DoubleDataEntry("temp", -18.35)),
                new BaseAttributeKvEntry(1694000000L, new JsonDataEntry("json", "{\"warning\":\"out of paper\"}"))
        );
        List<AttributeKvEntry> newAttributes = new ArrayList<>(currentAttributes);
        newAttributes.add(newAttributes.get(0));
        newAttributes.remove(0);
        assertThat(newAttributes).hasSize(currentAttributes.size());
        assertThat(currentAttributes).isNotEmpty();
        assertThat(newAttributes).containsExactlyInAnyOrderElementsOf(currentAttributes);

        List<AttributeKvEntry> filtered = node.filterChangedAttr(currentAttributes, newAttributes);
        assertThat(filtered).isEmpty(); //no changes
    }

    @Test
    void testFilterChangedAttr_whenCurrentAttributesContainsInAnyOrderNewAttributes_thenReturnExpectedList() {
        TbMsgAttributesNode node = spy(TbMsgAttributesNode.class);
        List<AttributeKvEntry> currentAttributes = List.of(
                new BaseAttributeKvEntry(1694000000L, new StringDataEntry("address", "Peremohy ave 1")),
                new BaseAttributeKvEntry(1694000000L, new BooleanDataEntry("valid", true)),
                new BaseAttributeKvEntry(1694000000L, new LongDataEntry("counter", 100L)),
                new BaseAttributeKvEntry(1694000000L, new DoubleDataEntry("temp", -18.35)),
                new BaseAttributeKvEntry(1694000000L, new JsonDataEntry("json", "{\"warning\":\"out of paper\"}"))
        );
        List<AttributeKvEntry> newAttributes = List.of(
                new BaseAttributeKvEntry(1694000999L, new JsonDataEntry("json", "{\"status\":\"OK\"}")), // value changed, reordered
                new BaseAttributeKvEntry(1694000999L, new StringDataEntry("valid", "true")), //type changed
                new BaseAttributeKvEntry(1694000999L, new LongDataEntry("counter", 101L)), //value changed
                new BaseAttributeKvEntry(1694000999L, new DoubleDataEntry("temp", -18.35)),
                new BaseAttributeKvEntry(1694000999L, new StringDataEntry("address", "Peremohy ave 1")) // reordered
        );
        List<AttributeKvEntry> expected = List.of(
                new BaseAttributeKvEntry(1694000999L, new StringDataEntry("valid", "true")),
                new BaseAttributeKvEntry(1694000999L, new LongDataEntry("counter", 101L)),
                new BaseAttributeKvEntry(1694000999L, new JsonDataEntry("json", "{\"status\":\"OK\"}"))
        );

        List<AttributeKvEntry> filtered = node.filterChangedAttr(currentAttributes, newAttributes);
        assertThat(filtered).containsExactlyInAnyOrderElementsOf(expected);
    }

    // Notify device backward-compatibility test arguments
    private static Stream<Arguments> provideNotifyDeviceMdValue() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of(true, true),
                Arguments.of(false, false)
        );
    }

    // Notify device backward-compatibility test
    @ParameterizedTest
    @MethodSource("provideNotifyDeviceMdValue")
    void testNotifyDeviceArgumentForSaveAndNotify(Boolean mdValue, boolean expectedArgumentValue) throws TbNodeException {
        var ctxMock = mock(TbContext.class);
        var telemetryServiceMock = mock(RuleEngineTelemetryService.class);
        TbMsgAttributesNode node = spy(TbMsgAttributesNode.class);
        ObjectNode defaultConfig = (ObjectNode) JacksonUtil.valueToTree(new TbMsgAttributesNodeConfiguration().defaultConfiguration());
        defaultConfig.put("notifyDevice", false);
        var tbNodeConfiguration = new TbNodeConfiguration(defaultConfig);

        assertThat(defaultConfig.has("notifyDevice")).as("pre condition has notifyDevice").isTrue();

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        willCallRealMethod().given(node).init(any(TbContext.class), any(TbNodeConfiguration.class));
        willCallRealMethod().given(node).saveAttr(any(), eq(ctxMock), any(TbMsg.class), anyString(), anyBoolean());

        node.init(ctxMock, tbNodeConfiguration);

        TbMsgMetaData md = new TbMsgMetaData();
        if (mdValue != null) {
            md.putValue(NOTIFY_DEVICE_METADATA_KEY, mdValue.toString());
        }
        // dummy list with one ts kv to pass the empty list check.
        var testTbMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, ORIGINATOR_ID, md, TbMsg.EMPTY_STRING);
        List<AttributeKvEntry> testAttrList = List.of(new BaseAttributeKvEntry(0L, new StringDataEntry("testKey", "testValue")));

        node.saveAttr(testAttrList, ctxMock, testTbMsg, DataConstants.SHARED_SCOPE, false);

        ArgumentCaptor<Boolean> notifyDeviceCaptor = ArgumentCaptor.forClass(Boolean.class);

        verify(telemetryServiceMock, times(1)).saveAndNotify(
                eq(TENANT_ID), eq(ORIGINATOR_ID), eq(DataConstants.SHARED_SCOPE),
                eq(testAttrList), notifyDeviceCaptor.capture(), any()
        );
        boolean notifyDevice = notifyDeviceCaptor.getValue();
        assertThat(notifyDevice).isEqualTo(expectedArgumentValue);
    }

    @Test
    void testUpgrade_fromVersion0() throws TbNodeException {

        TbMsgAttributesNode node = mock(TbMsgAttributesNode.class);
        willCallRealMethod().given(node).upgrade(anyInt(), any());

        ObjectNode jsonNode = (ObjectNode) JacksonUtil.valueToTree(new TbMsgAttributesNodeConfiguration().defaultConfiguration());
        jsonNode.remove(updateAttributesOnlyOnValueChangeKey);
        assertThat(jsonNode.has(updateAttributesOnlyOnValueChangeKey)).as("pre condition has no " + updateAttributesOnlyOnValueChangeKey).isFalse();

        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(0, jsonNode);

        ObjectNode resultNode = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradeResult.getFirst()).as("upgrade result has changes").isTrue();
        assertThat(resultNode.has(updateAttributesOnlyOnValueChangeKey)).as("upgrade result has key " + updateAttributesOnlyOnValueChangeKey).isTrue();
        assertThat(resultNode.get(updateAttributesOnlyOnValueChangeKey).asBoolean()).as("upgrade result value [false] for key " + updateAttributesOnlyOnValueChangeKey).isFalse();
    }

    @Test
    void testUpgrade_fromVersion0_alreadyHasupdateAttributesOnlyOnValueChange() throws TbNodeException {
        TbMsgAttributesNode node = mock(TbMsgAttributesNode.class);
        willCallRealMethod().given(node).upgrade(anyInt(), any());

        ObjectNode jsonNode = (ObjectNode) JacksonUtil.valueToTree(new TbMsgAttributesNodeConfiguration().defaultConfiguration());
        jsonNode.remove(updateAttributesOnlyOnValueChangeKey);
        jsonNode.put(updateAttributesOnlyOnValueChangeKey, true);
        assertThat(jsonNode.has(updateAttributesOnlyOnValueChangeKey)).as("pre condition has no " + updateAttributesOnlyOnValueChangeKey).isTrue();
        assertThat(jsonNode.get(updateAttributesOnlyOnValueChangeKey).asBoolean()).as("pre condition has [true] for key " + updateAttributesOnlyOnValueChangeKey).isTrue();

        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(0, jsonNode);

        ObjectNode resultNode = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradeResult.getFirst()).as("upgrade result has changes").isFalse();
        assertThat(resultNode.has(updateAttributesOnlyOnValueChangeKey)).as("upgrade result has key " + updateAttributesOnlyOnValueChangeKey).isTrue();
        assertThat(resultNode.get(updateAttributesOnlyOnValueChangeKey).asBoolean()).as("upgrade result value [true] for key " + updateAttributesOnlyOnValueChangeKey).isTrue();
    }

}
