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
package org.thingsboard.rule.engine.action;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class TbDeviceStateNodeTest {

    private static TenantId DUMMY_TENANT_ID;
    private static TbMsg DUMMY_MSG;
    private static DeviceId DUMMY_MSG_ORIGINATOR;
    @Mock
    private TbContext ctxMock;
    @Mock
    private TbClusterService tbClusterServiceMock;
    private TbDeviceStateNode node;
    private TbDeviceStateNodeConfiguration config;

    @BeforeAll
    public static void init() {
        DUMMY_TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

        var device = new Device();
        device.setTenantId(DUMMY_TENANT_ID);
        device.setId(new DeviceId(UUID.randomUUID()));
        device.setName("My humidity sensor");
        device.setType("Humidity sensor");
        device.setDeviceProfileId(new DeviceProfileId(UUID.randomUUID()));
        var metaData = new TbMsgMetaData();
        metaData.putValue("deviceName", device.getName());
        metaData.putValue("deviceType", device.getType());
        metaData.putValue("ts", String.valueOf(System.currentTimeMillis()));
        var data = JacksonUtil.newObjectNode();
        data.put("humidity", 58.3);
        DUMMY_MSG = TbMsg.newMsg(
                TbMsgType.POST_TELEMETRY_REQUEST, device.getId(), metaData, JacksonUtil.toString(data)
        );
        DUMMY_MSG_ORIGINATOR = device.getId();
    }

    @BeforeEach
    public void setUp() {
        node = new TbDeviceStateNode();
        config = new TbDeviceStateNodeConfiguration();
    }

    @Test
    public void givenDefaultConfiguration_whenInvoked_thenCorrectValuesAreSet() {
        // GIVEN-WHEN
        config = config.defaultConfiguration();

        // THEN
        assertThat(config.getEvent()).isEqualTo(TbMsgType.ACTIVITY_EVENT);
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
        var originator = new AssetId(UUID.randomUUID());
        var msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, originator, new TbMsgMetaData(), "{}");
        given(ctxMock.isLocalEntity(originator)).willReturn(true);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should(times(1)).isLocalEntity(originator);
        then(ctxMock).should(times(1)).tellSuccess(msg);
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenNonLocalOriginator_whenOnMsg_thenTellsSuccessAndNoActivityActionsTriggered() {
        // GIVEN
        given(ctxMock.isLocalEntity(DUMMY_MSG.getOriginator())).willReturn(false);

        // WHEN
        node.onMsg(ctxMock, DUMMY_MSG);

        // THEN
        then(ctxMock).should(times(1)).isLocalEntity(DUMMY_MSG_ORIGINATOR);
        then(ctxMock).should().getSelfId();
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenConnectEventInConfig_whenOnMsg_thenOnDeviceConnectCalledAndTellsSuccess() {
        // GIVEN
        config.setEvent(TbMsgType.CONNECT_EVENT);
        var nodeConfig = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        try {
            node.init(ctxMock, nodeConfig);
        } catch (TbNodeException e) {
            fail("Node failed to initialize!", e);
        }
        given(ctxMock.getTenantId()).willReturn(DUMMY_TENANT_ID);
        given(ctxMock.getClusterService()).willReturn(tbClusterServiceMock);
        given(ctxMock.isLocalEntity(DUMMY_MSG.getOriginator())).willReturn(true);

        // WHEN
        node.onMsg(ctxMock, DUMMY_MSG);

        // THEN
        var protoCaptor = ArgumentCaptor.forClass(TransportProtos.ToCoreMsg.class);
        then(tbClusterServiceMock).should(times(1))
                .pushMsgToCore(eq(DUMMY_TENANT_ID), eq(DUMMY_MSG_ORIGINATOR), protoCaptor.capture(), any());

        TransportProtos.DeviceConnectProto expectedDeviceConnectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(DUMMY_TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(DUMMY_TENANT_ID.getId().getLeastSignificantBits())
                .setDeviceIdMSB(DUMMY_MSG_ORIGINATOR.getId().getMostSignificantBits())
                .setDeviceIdLSB(DUMMY_MSG_ORIGINATOR.getId().getLeastSignificantBits())
                .build();
        TransportProtos.ToCoreMsg expectedToCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceConnectMsg(expectedDeviceConnectMsg)
                .build();
        assertThat(expectedToCoreMsg).isEqualTo(protoCaptor.getValue());

        then(ctxMock).should(times(1)).tellSuccess(DUMMY_MSG);
        then(tbClusterServiceMock).shouldHaveNoMoreInteractions();
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenActivityEventInConfig_whenOnMsg_thenOnDeviceActivityCalledWithCorrectTimeAndTellsSuccess()
            throws TbNodeException {
        // GIVEN
        config.setEvent(TbMsgType.ACTIVITY_EVENT);
        var nodeConfig = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfig);
        given(ctxMock.getTenantId()).willReturn(DUMMY_TENANT_ID);
        given(ctxMock.getClusterService()).willReturn(tbClusterServiceMock);
        given(ctxMock.isLocalEntity(DUMMY_MSG.getOriginator())).willReturn(true);


        // WHEN
        long timeBeforeCall = System.currentTimeMillis();
        node.onMsg(ctxMock, DUMMY_MSG);
        long timeAfterCall = System.currentTimeMillis();

        // THEN
        var protoCaptor = ArgumentCaptor.forClass(TransportProtos.ToCoreMsg.class);
        then(tbClusterServiceMock).should(times(1))
                .pushMsgToCore(eq(DUMMY_TENANT_ID), eq(DUMMY_MSG_ORIGINATOR), protoCaptor.capture(), any());

        TransportProtos.ToCoreMsg actualToCoreMsg = protoCaptor.getValue();
        long actualLastActivityTime = actualToCoreMsg.getDeviceActivityMsg().getLastActivityTime();

        assertThat(actualLastActivityTime).isGreaterThanOrEqualTo(timeBeforeCall);
        assertThat(actualLastActivityTime).isLessThanOrEqualTo(timeAfterCall);

        TransportProtos.DeviceActivityProto updatedActivityMsg = actualToCoreMsg.getDeviceActivityMsg().toBuilder()
                .setLastActivityTime(123L)
                .build();
        TransportProtos.ToCoreMsg updatedToCoreMsg = actualToCoreMsg.toBuilder()
                .setDeviceActivityMsg(updatedActivityMsg)
                .build();

        TransportProtos.DeviceActivityProto expectedDeviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(DUMMY_TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(DUMMY_TENANT_ID.getId().getLeastSignificantBits())
                .setDeviceIdMSB(DUMMY_MSG_ORIGINATOR.getId().getMostSignificantBits())
                .setDeviceIdLSB(DUMMY_MSG_ORIGINATOR.getId().getLeastSignificantBits())
                .setLastActivityTime(123L)
                .build();
        TransportProtos.ToCoreMsg expectedToCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceActivityMsg(expectedDeviceActivityMsg)
                .build();

        assertThat(updatedToCoreMsg).isEqualTo(expectedToCoreMsg);

        then(ctxMock).should(times(1)).tellSuccess(DUMMY_MSG);
        then(tbClusterServiceMock).shouldHaveNoMoreInteractions();
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenInactivityEventInConfig_whenOnMsg_thenOnDeviceInactivityCalledAndTellsSuccess()
            throws TbNodeException {
        // GIVEN
        config.setEvent(TbMsgType.INACTIVITY_EVENT);
        var nodeConfig = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfig);
        given(ctxMock.getTenantId()).willReturn(DUMMY_TENANT_ID);
        given(ctxMock.getClusterService()).willReturn(tbClusterServiceMock);
        given(ctxMock.isLocalEntity(DUMMY_MSG.getOriginator())).willReturn(true);

        // WHEN
        node.onMsg(ctxMock, DUMMY_MSG);

        // THEN
        var protoCaptor = ArgumentCaptor.forClass(TransportProtos.ToCoreMsg.class);
        then(tbClusterServiceMock).should(times(1))
                .pushMsgToCore(eq(DUMMY_TENANT_ID), eq(DUMMY_MSG_ORIGINATOR), protoCaptor.capture(), any());

        TransportProtos.DeviceInactivityProto expectedDeviceInactivityMsg =
                TransportProtos.DeviceInactivityProto.newBuilder()
                        .setTenantIdMSB(DUMMY_TENANT_ID.getId().getMostSignificantBits())
                        .setTenantIdLSB(DUMMY_TENANT_ID.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(DUMMY_MSG_ORIGINATOR.getId().getMostSignificantBits())
                        .setDeviceIdLSB(DUMMY_MSG_ORIGINATOR.getId().getLeastSignificantBits())
                        .build();
        TransportProtos.ToCoreMsg expectedToCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceInactivityMsg(expectedDeviceInactivityMsg)
                .build();
        assertThat(expectedToCoreMsg).isEqualTo(protoCaptor.getValue());

        then(ctxMock).should(times(1)).tellSuccess(DUMMY_MSG);
        then(tbClusterServiceMock).shouldHaveNoMoreInteractions();
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenDisconnectEventInConfig_whenOnMsg_thenOnDeviceDisconnectCalledAndTellsSuccess()
            throws TbNodeException {
        // GIVEN
        config.setEvent(TbMsgType.DISCONNECT_EVENT);
        var nodeConfig = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfig);
        given(ctxMock.getTenantId()).willReturn(DUMMY_TENANT_ID);
        given(ctxMock.getClusterService()).willReturn(tbClusterServiceMock);
        given(ctxMock.isLocalEntity(DUMMY_MSG.getOriginator())).willReturn(true);

        // WHEN
        node.onMsg(ctxMock, DUMMY_MSG);

        // THEN
        var protoCaptor = ArgumentCaptor.forClass(TransportProtos.ToCoreMsg.class);
        then(tbClusterServiceMock).should(times(1))
                .pushMsgToCore(eq(DUMMY_TENANT_ID), eq(DUMMY_MSG_ORIGINATOR), protoCaptor.capture(), any());

        TransportProtos.DeviceDisconnectProto expectedDeviceDisconnectMsg =
                TransportProtos.DeviceDisconnectProto.newBuilder()
                        .setTenantIdMSB(DUMMY_TENANT_ID.getId().getMostSignificantBits())
                        .setTenantIdLSB(DUMMY_TENANT_ID.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(DUMMY_MSG_ORIGINATOR.getId().getMostSignificantBits())
                        .setDeviceIdLSB(DUMMY_MSG_ORIGINATOR.getId().getLeastSignificantBits())
                        .build();
        TransportProtos.ToCoreMsg expectedToCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceDisconnectMsg(expectedDeviceDisconnectMsg)
                .build();
        assertThat(expectedToCoreMsg).isEqualTo(protoCaptor.getValue());

        then(ctxMock).should(times(1)).tellSuccess(DUMMY_MSG);
        then(tbClusterServiceMock).shouldHaveNoMoreInteractions();
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

}
