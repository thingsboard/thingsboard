/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TbMsgDeviceStateNodeTest {

    TenantId tenantId;
    DeviceId deviceId;
    TbContext ctx;
    TbMsgDeviceStateNode node;
    TbMsgDeviceStateNodeConfiguration config;
    TbMsgCallback callback;
    RuleEngineDeviceStateService ruleEngineDeviceStateService;

    @BeforeEach
    void setUp() {
        tenantId = new TenantId(UUID.randomUUID());
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);

        //node
        config = new TbMsgDeviceStateNodeConfiguration().defaultConfiguration();
        node = spy(new TbMsgDeviceStateNode());

        //init mock
        ctx = mock(TbContext.class);
        ruleEngineDeviceStateService = mock(RuleEngineDeviceStateService.class);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getRuleEngineDeviceStateService()).thenReturn(ruleEngineDeviceStateService);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void checkConfigNode_whenInit_thenFail() {
        assertThatThrownBy(() -> node.init(ctx, getNodeConfiguration(DataConstants.ENTITY_CREATED))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkConfigNode_whenOriginatorNotDevice_thenFail() throws TbNodeException, ExecutionException, InterruptedException {
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        node.init(ctx, getNodeConfiguration(DataConstants.ACTIVITY_EVENT));

        assertThatThrownBy(() -> node.onMsg(ctx, getTbMsg(customerId, "{}"))).isInstanceOf(TbNodeException.class);
    }

    @Test
    void givenMsg_whenOnMsg_then_activityEvent() throws TbNodeException, ExecutionException, InterruptedException {
        verifyOnMs_Node(DataConstants.ACTIVITY_EVENT);
    }

    @Test
    void givenMsg_whenOnMsg_then_connectEvent() throws TbNodeException, ExecutionException, InterruptedException {
        verifyOnMs_Node(DataConstants.CONNECT_EVENT);
    }

    @Test
    void givenMsg_whenOnMsg_then_inactivityEvent() throws TbNodeException, ExecutionException, InterruptedException {
        verifyOnMs_Node(DataConstants.INACTIVITY_EVENT);
    }

    @Test
    void givenMsg_whenOnMsg_then_disconnectEvent() throws TbNodeException, ExecutionException, InterruptedException {
        verifyOnMs_Node(DataConstants.DISCONNECT_EVENT);
    }

    private void verifyOnMs_Node(String event) throws TbNodeException, ExecutionException, InterruptedException {
        node.init(ctx, getNodeConfiguration(event));
        node.onMsg(ctx, getTbMsg(deviceId, "{}"));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        if (event.equals(DataConstants.ACTIVITY_EVENT)) {
            verify(ruleEngineDeviceStateService, times(1)).onDeviceActivity(eq(tenantId), eq(deviceId), anyLong());
        } else if (event.equals(DataConstants.CONNECT_EVENT)) {
            verify(ruleEngineDeviceStateService, times(1)).onDeviceConnect(tenantId, deviceId);
        } else if (event.equals(DataConstants.INACTIVITY_EVENT)) {
            verify(ruleEngineDeviceStateService, times(1)).onDeviceInactivityTimeoutUpdate(tenantId, deviceId, 0);
        } else if (event.equals(DataConstants.DISCONNECT_EVENT)) {
            verify(ruleEngineDeviceStateService, times(1)).onDeviceDisconnect(tenantId, deviceId);
        }

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
    }

    private TbNodeConfiguration getNodeConfiguration(String event) {
        config.setEvent(event);
        return new TbNodeConfiguration(JacksonUtil.valueToTree(config));
    }

    private TbMsg getTbMsg(EntityId entityId, String data) {
        final Map<String, String> mdMap = Map.of(
                "TestKey_1", "Test",
                "country", "US",
                "voltageDataValue", "220",
                "city", "NY"
        );
        return TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", entityId, new TbMsgMetaData(mdMap), data, callback);
    }
}
