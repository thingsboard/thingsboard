// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.telemetry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TbCalculatedFieldsNodeTest {

    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("7361ca62-7688-4d78-b374-bc3d77e12dba"));
    final DeviceId deviceId = new DeviceId(UUID.fromString("21c55f8d-0c5c-47b3-a344-9657e194b0f6"));

    @Spy
    TbCalculatedFieldsNode node;

    @Mock
    TbContext ctxMock;
    @Mock
    RuleEngineTelemetryService telemetryServiceMock;

    @BeforeEach
    void setUp() {
        lenient().when(ctxMock.getTenantId()).thenReturn(tenantId);
        lenient().when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        var config = new EmptyNodeConfiguration().defaultConfiguration();
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @ParameterizedTest
    @EnumSource(TbMsgType.class)
    void givenInvalidMsgType_whenOnMsg_thenTellFailure(TbMsgType msgType) {
        if (TbMsgType.POST_TELEMETRY_REQUEST == msgType || TbMsgType.POST_ATTRIBUTES_REQUEST == msgType) {
            return;
        }

        // GIVEN
        var msg = TbMsg.newMsg()
                .type(msgType)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<Throwable> actualError = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), actualError.capture());
        assertThat(actualError.getValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported msg type: " + msg.getType());
    }

    @Test
    void givenTelemetryMsg_whenOnMsg_thenPushToTelemetryService() {
        // GIVEN
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(telemetryServiceMock).should().saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(TimeseriesSaveRequest.Strategy.CF_ONLY)
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST_TELEMETRY_REQUEST", "POST_ATTRIBUTES_REQUEST"})
    void givenEmptyTelemetryOrAttributesMsg_whenOnMsg_thenTellSuccess(String msgType) {
        // GIVEN
        var msg = TbMsg.newMsg()
                .type(TbMsgType.valueOf(msgType))
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().tellSuccess(msg);
    }

    @Test
    void givenEmptyAttributeScope_whenOnMsg_thenTellFailure() {
        // GIVEN
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("active", true).toString())
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<Throwable> actualError = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), actualError.capture());
        assertThat(actualError.getValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Attribute scope is missing");
    }

    @Test
    void givenInvalidAttributeScope_whenOnMsg_thenTellFailure() {
        // GIVEN
        String invalidScope = "INVALID_SCOPE";
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("active", true).toString())
                .metaData(new TbMsgMetaData(Map.of("scope", invalidScope)))
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<Throwable> actualError = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), actualError.capture());
        assertThat(actualError.getValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid attribute scope: " + invalidScope);
    }

    @Test
    void givenAttributesMsg_whenOnMsg_thenPushToTelemetryService() {
        // GIVEN
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data(JacksonUtil.newObjectNode().put("active", true).toString())
                .metaData(new TbMsgMetaData(Map.of("scope", AttributeScope.SERVER_SCOPE.name())))
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(telemetryServiceMock).should().saveAttributes(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(AttributesSaveRequest.Strategy.CF_ONLY)
        ));
    }

    @Test
    void givenTelemetryMsgWithTypeCastDisabledKeys_whenOnMsg_thenListedKeysAreSavedAsStrings() {
        var metadata = new TbMsgMetaData();
        metadata.putTypeCastDisabledKeys(Set.of("devAddr"));
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .data("{\"devAddr\": \"01020304\", \"temperature\": \"22.5\"}")
                .metaData(metadata)
                .build();

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<TimeseriesSaveRequest> captor = ArgumentCaptor.forClass(TimeseriesSaveRequest.class);
        then(telemetryServiceMock).should().saveTimeseries(captor.capture());
        assertThat(captor.getValue().getEntries()).extracting(KvEntry::getKey, KvEntry::getDataType, KvEntry::getValue).containsExactlyInAnyOrder(
                tuple("devAddr", DataType.STRING, "01020304"),
                tuple("temperature", DataType.DOUBLE, 22.5));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenAttributesMsg_whenOnMsg_thenTypeCastDisabledKeysKeepStrings(boolean withTypeCastDisabledKeys) {
        var metadata = new TbMsgMetaData(Map.of("scope", AttributeScope.CLIENT_SCOPE.name()));
        if (withTypeCastDisabledKeys) {
            metadata.putTypeCastDisabledKeys(Set.of("joinEui"));
        }
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .data("{\"joinEui\": \"0000000000000000\", \"hardwareVersion\": \"1.0\"}")
                .metaData(metadata)
                .build();

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<AttributesSaveRequest> captor = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        then(telemetryServiceMock).should().saveAttributes(captor.capture());
        assertThat(captor.getValue().getEntries()).extracting(KvEntry::getKey, KvEntry::getDataType, KvEntry::getValue).containsExactlyInAnyOrder(
                withTypeCastDisabledKeys ? tuple("joinEui", DataType.STRING, "0000000000000000") : tuple("joinEui", DataType.LONG, 0L),
                tuple("hardwareVersion", DataType.DOUBLE, 1.0));
    }

}
