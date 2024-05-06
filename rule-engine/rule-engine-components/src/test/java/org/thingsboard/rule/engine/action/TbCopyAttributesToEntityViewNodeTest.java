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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.objects.AttributesEntityView;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.entityview.EntityViewService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.msg.TbMsgType.ACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_DELETED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_UPDATED;
import static org.thingsboard.server.common.data.msg.TbMsgType.INACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;

@ExtendWith(MockitoExtension.class)
public class TbCopyAttributesToEntityViewNodeTest {

    private final TenantId TENANT_ID = new TenantId(UUID.fromString("9fdb1f05-dc66-4960-9263-ae195f1b4533"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("1d453dc9-9333-476a-a51f-093cf2176e59"));
    private final long FROM_DATE = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
    private final long TO_DATE = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();

    private TbCopyAttributesToEntityViewNode node;
    private EmptyNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private EntityViewService entityViewServiceMock;
    @Mock
    private RuleEngineTelemetryService telemetryServiceMock;

    @BeforeEach
    void setUp() throws TbNodeException {
        node = new TbCopyAttributesToEntityViewNode();
        config = new EmptyNodeConfiguration().defaultConfiguration();
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);
    }

    @Test
    public void givenExistingAttributes_whenOnMsg_thenCopyAttributesToView() {
        EntityView entityView = getEntityView();
        EntityViewId entityViewId = entityView.getId();

        TbMsg msg = TbMsg.newMsg(
                TbMsgType.POST_ATTRIBUTES_REQUEST, DEVICE_ID, new TbMsgMetaData(Map.of("scope", AttributeScope.CLIENT_SCOPE.name())),
                "{\"attribute1\": 100, \"attribute2\": \"value2\"}");

        when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(entityViewServiceMock.findEntityViewsByTenantIdAndEntityIdAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(entityView)));
        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        doAnswer(invocation -> {
            FutureCallback<Void> callback = invocation.getArgument(4);
            callback.onSuccess(null);
            return null;
        }).when(telemetryServiceMock).saveAndNotify(any(), any(), any(AttributeScope.class), anyList(), any(FutureCallback.class));
        TbMsg newMsg = TbMsg.newMsg(msg, msg.getQueueName(), msg.getRuleChainId(), msg.getRuleNodeId());
        doAnswer(invocation -> newMsg).when(ctxMock).newMsg(any(), any(String.class), any(), any(), any(), any());

        node.onMsg(ctxMock, msg);

        verify(entityViewServiceMock).findEntityViewsByTenantIdAndEntityIdAsync(eq(TENANT_ID), eq(DEVICE_ID));
        verify(telemetryServiceMock).saveAndNotify(eq(TENANT_ID), eq(entityViewId), eq(AttributeScope.CLIENT_SCOPE), anyList(), any(FutureCallback.class));
        verify(ctxMock).ack(eq(msg));
        verify(ctxMock).enqueueForTellNext(eq(newMsg), eq(TbNodeConnectionType.SUCCESS));
    }

    @Test
    public void givenExistingAttributesAndMsgTypeAttributesDeleted_whenOnMsg_thenDeleteAttributesFromView() {
        EntityView entityView = getEntityView();
        EntityViewId entityViewId = entityView.getId();

        TbMsg msg = TbMsg.newMsg(
                ATTRIBUTES_DELETED, DEVICE_ID, new TbMsgMetaData(Map.of("scope", AttributeScope.CLIENT_SCOPE.name())),
                "{\"attributes\": [\"attribute1\"]}");

        when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(entityViewServiceMock.findEntityViewsByTenantIdAndEntityIdAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(entityView)));
        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        doAnswer(invocation -> {
            FutureCallback<Void> callback = invocation.getArgument(4);
            callback.onSuccess(null);
            return null;
        }).when(telemetryServiceMock).deleteAndNotify(any(), any(), any(AttributeScope.class), anyList(), any(FutureCallback.class));
        TbMsg newMsg = TbMsg.newMsg(msg, msg.getQueueName(), msg.getRuleChainId(), msg.getRuleNodeId());
        doAnswer(invocation -> newMsg).when(ctxMock).newMsg(any(), any(String.class), any(), any(), any(), any());

        node.onMsg(ctxMock, msg);

        verify(entityViewServiceMock).findEntityViewsByTenantIdAndEntityIdAsync(eq(TENANT_ID), eq(DEVICE_ID));
        verify(telemetryServiceMock).deleteAndNotify(eq(TENANT_ID), eq(entityViewId), eq(AttributeScope.CLIENT_SCOPE), anyList(), any(FutureCallback.class));
        verify(ctxMock).ack(eq(msg));
        verify(ctxMock).enqueueForTellNext(eq(newMsg), eq(TbNodeConnectionType.SUCCESS));
    }

    @Test
    public void givenNonMatchedAttributesAndMsgTypeIsAttributesDeleted_whenOnMsg_thenNoAttributesDeleteFromView() {
        EntityView entityView = getEntityView();

        TbMsg msg = TbMsg.newMsg(
                TbMsgType.ATTRIBUTES_DELETED, DEVICE_ID, new TbMsgMetaData(Map.of("scope", AttributeScope.CLIENT_SCOPE.name())),
                "{\"attributes\": [\"anotherAttribute\"]}");

        when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(entityViewServiceMock.findEntityViewsByTenantIdAndEntityIdAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(entityView)));

        node.onMsg(ctxMock, msg);

        verify(entityViewServiceMock).findEntityViewsByTenantIdAndEntityIdAsync(eq(TENANT_ID), eq(DEVICE_ID));
        verify(ctxMock).ack(eq(msg));
        verify(ctxMock, never()).getTelemetryService();
    }

    @Test
    public void givenNonMatchedAttributesAndMsgTypeIsPostAttributesRequest_whenOnMsg_thenCopyNoAttributesToView() {
        EntityView entityView = getEntityView();
        EntityViewId entityViewId = entityView.getId();

        TbMsg msg = TbMsg.newMsg(
                TbMsgType.POST_ATTRIBUTES_REQUEST, DEVICE_ID, new TbMsgMetaData(Map.of("scope", AttributeScope.CLIENT_SCOPE.name())),
                "{\"attribute2\": \"value2\"}");

        when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(entityViewServiceMock.findEntityViewsByTenantIdAndEntityIdAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(entityView)));
        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        doAnswer(invocation -> {
            FutureCallback<Void> callback = invocation.getArgument(4);
            callback.onSuccess(null);
            return null;
        }).when(telemetryServiceMock).saveAndNotify(any(), any(), any(AttributeScope.class), anyList(), any(FutureCallback.class));
        TbMsg newMsg = TbMsg.newMsg(msg, msg.getQueueName(), msg.getRuleChainId(), msg.getRuleNodeId());
        doAnswer(invocation -> newMsg).when(ctxMock).newMsg(any(), any(String.class), any(), any(), any(), any());

        node.onMsg(ctxMock, msg);

        verify(entityViewServiceMock).findEntityViewsByTenantIdAndEntityIdAsync(eq(TENANT_ID), eq(DEVICE_ID));
        verify(telemetryServiceMock).saveAndNotify(eq(TENANT_ID), eq(entityViewId), eq(AttributeScope.CLIENT_SCOPE), eq(Collections.emptyList()), any(FutureCallback.class));
        verify(ctxMock).ack(eq(msg));
        verify(ctxMock).enqueueForTellNext(eq(newMsg), eq(TbNodeConnectionType.SUCCESS));
    }

    @Test
    public void givenAttributesValidityPeriodOutOfStartDateAndEndDate_whenOnMsg_thenDoNothing() {
        EntityViewId entityViewId = EntityViewId.fromString("d117f1a4-24ea-4fdd-b94e-5a472e99d925");
        EntityView entityView = new EntityView(entityViewId);
        entityView.setStartTimeMs(Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli());
        entityView.setEndTimeMs(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli());

        when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(entityViewServiceMock.findEntityViewsByTenantIdAndEntityIdAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(entityView)));

        TbMsg msg = TbMsg.newMsg(
                ATTRIBUTES_DELETED, DEVICE_ID, new TbMsgMetaData(Map.of("scope", AttributeScope.CLIENT_SCOPE.name())),
                "{\"attributes\": [\"attribute1\"]}");

        node.onMsg(ctxMock, msg);

        verify(entityViewServiceMock).findEntityViewsByTenantIdAndEntityIdAsync(eq(TENANT_ID), eq(DEVICE_ID));
        verify(ctxMock).ack(eq(msg));
    }

    @Test
    public void givenEmptyMetadata_whenOnMsg_thenThrowsException() {
        TbMsg msg = TbMsg.newMsg(
                ATTRIBUTES_UPDATED, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), captor.capture());
        Throwable throwable = captor.getValue();
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class).hasMessage("Message metadata is empty");
    }

    @ParameterizedTest
    @EnumSource(TbMsgType.class)
    public void givenUnsupportedMsgType_whenOnMsg_thenTellFailure(TbMsgType msgType) {
        TbMsg msg = TbMsg.newMsg(
                msgType, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        if (msg.isTypeOneOf(ATTRIBUTES_UPDATED, ATTRIBUTES_DELETED,
                ACTIVITY_EVENT, INACTIVITY_EVENT, POST_ATTRIBUTES_REQUEST)) {
            return;
        }

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), captor.capture());
        Throwable throwable = captor.getValue();
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class).hasMessage("Unsupported msg type [" + msgType + "]");
    }

    private EntityView getEntityView() {
        EntityViewId entityViewId = EntityViewId.fromString("a2109747-d1f4-475a-baaa-55f5d4897ad8");
        EntityView entityView = new EntityView(entityViewId);
        entityView.setStartTimeMs(FROM_DATE);
        entityView.setEndTimeMs(TO_DATE);
        AttributesEntityView attributes = new AttributesEntityView(List.of("attribute1"), Collections.emptyList(), Collections.emptyList());
        entityView.setKeys(new TelemetryEntityView(Collections.emptyList(), attributes));
        return entityView;
    }
}
