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
package org.thingsboard.rule.engine.action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class TbDeviceStateNodeTest {

    @Mock
    private TbContext ctxMock;
    @Mock
    private TbClusterService tbClusterServiceMock;
    private TbDeviceStateNode node;
    private TbDeviceStateNodeConfiguration config;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());
    private static final long METADATA_TS = System.currentTimeMillis();
    private TbMsg msg;

    @BeforeEach
    public void setup() {
        var device = new Device();
        device.setTenantId(TENANT_ID);
        device.setId(DEVICE_ID);
        device.setName("My humidity sensor");
        device.setType("Humidity sensor");
        device.setDeviceProfileId(new DeviceProfileId(UUID.randomUUID()));
        var metaData = new TbMsgMetaData();
        metaData.putValue("deviceName", device.getName());
        metaData.putValue("deviceType", device.getType());
        metaData.putValue("ts", String.valueOf(METADATA_TS));
        var data = JacksonUtil.newObjectNode();
        data.put("humidity", 58.3);
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, device.getId(), metaData, JacksonUtil.toString(data));
    }

    @BeforeEach
    public void setUp() {
        node = new TbDeviceStateNode();
        config = new TbDeviceStateNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void givenDefaultConfiguration_whenInvoked_thenCorrectValuesAreSet() {
        assertThat(config.getEvent()).isEqualTo(TbMsgType.ACTIVITY_EVENT);
        assertThat(TbDeviceStateNode.SUPPORTED_EVENTS).isEqualTo(Set.of(
                TbMsgType.CONNECT_EVENT, TbMsgType.ACTIVITY_EVENT,
                TbMsgType.DISCONNECT_EVENT, TbMsgType.INACTIVITY_EVENT
        ));
    }

    @Test
    public void givenNullEventInConfig_whenInit_thenThrowsUnrecoverableTbNodeException() {
        // GIVEN
        config.setEvent(null);
        var nodeConfig = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN-THEN
        assertThatThrownBy(() -> node.init(ctxMock, nodeConfig))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Event cannot be null!")
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    @Test
    public void givenUnsupportedEventInConfig_whenInit_thenThrowsUnrecoverableTbNodeException() {
        // GIVEN
        var unsupportedEvent = TbMsgType.TO_SERVER_RPC_REQUEST;
        config.setEvent(unsupportedEvent);
        var nodeConfig = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN-THEN
        assertThatThrownBy(() -> node.init(ctxMock, nodeConfig))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Unsupported event: " + unsupportedEvent)
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    @Test
    public void givenNonDeviceOriginator_whenOnMsg_thenTellsSuccessAndNoActivityActionsTriggered() {
        // GIVEN
        var asset = new AssetId(UUID.randomUUID());
        var msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, asset, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().tellSuccess(msg);
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    @ParameterizedTest
    @MethodSource("provideSupportedEventsAndExpectedMessages")
    public void givenSupportedEvent_whenOnMsg_thenCorrectMsgIsSent(TbMsgType event, TransportProtos.ToCoreMsg expectedToCoreMsg) {
        // GIVEN
        config.setEvent(event);
        var nodeConfig = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        try {
            node.init(ctxMock, nodeConfig);
        } catch (TbNodeException e) {
            fail("Node failed to initialize!", e);
        }
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getClusterService()).willReturn(tbClusterServiceMock);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var protoCaptor = ArgumentCaptor.forClass(TransportProtos.ToCoreMsg.class);
        var callbackCaptor = ArgumentCaptor.forClass(TbQueueCallback.class);
        then(tbClusterServiceMock).should().pushMsgToCore(eq(TENANT_ID), eq(DEVICE_ID), protoCaptor.capture(), callbackCaptor.capture());

        TbQueueCallback actualCallback = callbackCaptor.getValue();

        actualCallback.onSuccess(null);
        then(ctxMock).should().tellSuccess(msg);

        var throwable = new Throwable();
        actualCallback.onFailure(throwable);
        then(ctxMock).should().tellFailure(msg, throwable);

        assertThat(expectedToCoreMsg).isEqualTo(protoCaptor.getValue());

        then(tbClusterServiceMock).shouldHaveNoMoreInteractions();
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    private static Stream<Arguments> provideSupportedEventsAndExpectedMessages() {
        return Stream.of(
                Arguments.of(TbMsgType.CONNECT_EVENT, TransportProtos.ToCoreMsg.newBuilder().setDeviceConnectMsg(
                        TransportProtos.DeviceConnectProto.newBuilder()
                                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                .setLastConnectTime(METADATA_TS)
                                .build()).build()),
                Arguments.of(TbMsgType.ACTIVITY_EVENT, TransportProtos.ToCoreMsg.newBuilder().setDeviceActivityMsg(
                        TransportProtos.DeviceActivityProto.newBuilder()
                                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                .setLastActivityTime(METADATA_TS)
                                .build()).build()),
                Arguments.of(TbMsgType.DISCONNECT_EVENT, TransportProtos.ToCoreMsg.newBuilder().setDeviceDisconnectMsg(
                        TransportProtos.DeviceDisconnectProto.newBuilder()
                                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                .setLastDisconnectTime(METADATA_TS)
                                .build()).build()),
                Arguments.of(TbMsgType.INACTIVITY_EVENT, TransportProtos.ToCoreMsg.newBuilder().setDeviceInactivityMsg(
                        TransportProtos.DeviceInactivityProto.newBuilder()
                                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                .setLastInactivityTime(METADATA_TS)
                                .build()).build())
        );
    }

}
