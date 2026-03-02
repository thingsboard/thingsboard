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
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

}
