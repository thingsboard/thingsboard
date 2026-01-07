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
package org.thingsboard.rule.engine.action;

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
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
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
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.msg.TbMsgType.ACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_DELETED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_UPDATED;
import static org.thingsboard.server.common.data.msg.TbMsgType.INACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;

@ExtendWith(MockitoExtension.class)
public class TbCopyAttributesToEntityViewNodeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("9fdb1f05-dc66-4960-9263-ae195f1b4533"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("1d453dc9-9333-476a-a51f-093cf2176e59"));
    private final EntityViewId ENTITY_VIEW_ID = new EntityViewId(UUID.fromString("65636806-453d-4bb4-b513-92b833970753"));

    private final AttributesEntityView CLIENT_ATTRIBUTES = new AttributesEntityView(List.of("clientAttribute1"), Collections.emptyList(), Collections.emptyList());
    private final AttributesEntityView SERVER_ATTRIBUTES = new AttributesEntityView(Collections.emptyList(), List.of("serverAttribute1"), Collections.emptyList());
    private final AttributesEntityView SHARED_ATTRIBUTES = new AttributesEntityView(Collections.emptyList(), Collections.emptyList(), List.of("sharedAttribute1"));

    private final TelemetryEntityView CLIENT_TELEMETRY_ENTITY_VIEW = new TelemetryEntityView(Collections.emptyList(), CLIENT_ATTRIBUTES);
    private final TelemetryEntityView SERVER_TELEMETRY_ENTITY_VIEW = new TelemetryEntityView(Collections.emptyList(), SERVER_ATTRIBUTES);
    private final TelemetryEntityView SHARED_TELEMETRY_ENTITY_VIEW = new TelemetryEntityView(Collections.emptyList(), SHARED_ATTRIBUTES);

    private final long ENTITY_VIEW_START_TS = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
    private final long ENTITY_VIEW_END_TS = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();

    private TbCopyAttributesToEntityViewNode node;

    @Mock
    private TbContext ctxMock;
    @Mock
    private EntityViewService entityViewServiceMock;
    @Mock
    private RuleEngineTelemetryService telemetryServiceMock;

    @BeforeEach
    void setUp() throws TbNodeException {
        node = new TbCopyAttributesToEntityViewNode();
        var config = new EmptyNodeConfiguration().defaultConfiguration();
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);
    }

    @Test
    public void givenExistingClientAttributes_whenOnMsg_thenCopyAttributesToView() {
        EntityView entityView = getEntityView(CLIENT_TELEMETRY_ENTITY_VIEW);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of(DataConstants.SCOPE, AttributeScope.SERVER_SCOPE.name())))
                .data("{\"clientAttribute1\": 100, \"clientAttribute2\": \"value2\"}")
                .build();

        mockEntityViewLookup(entityView);
        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        doAnswer(invocation -> {
            AttributesSaveRequest request = invocation.getArgument(0);
            request.getCallback().onSuccess(null);
            return null;
        }).when(telemetryServiceMock).saveAttributes(any(AttributesSaveRequest.class));
        TbMsg newMsg = TbMsg.newMsg(msg, msg.getQueueName(), msg.getRuleChainId(), msg.getRuleNodeId());
        // TODO: use newMsg() with any(TbMsgType.class), replace in other tests as well.
        doAnswer(invocation -> newMsg).when(ctxMock).newMsg(any(), any(String.class), any(), any(), any(), any());

        node.onMsg(ctxMock, msg);

        verify(entityViewServiceMock).findEntityViewsByTenantIdAndEntityIdAsync(eq(TENANT_ID), eq(DEVICE_ID));
        verify(telemetryServiceMock).saveAttributes(assertArg(request -> {
            assertThat(request.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(request.getEntityId()).isEqualTo(ENTITY_VIEW_ID);
            assertThat(request.getScope()).isEqualTo(AttributeScope.CLIENT_SCOPE);

            assertThat(request.getEntries().size()).isEqualTo(1);
            assertThat(request.getEntries().get(0).getKey()).isEqualTo("clientAttribute1");
            assertThat(request.getEntries().get(0).getValue()).isEqualTo(100L);
        }));
        verify(ctxMock).ack(eq(msg));
        verify(ctxMock).enqueueForTellNext(eq(newMsg), eq(TbNodeConnectionType.SUCCESS));
        verifyNoMoreInteractions(ctxMock, entityViewServiceMock, telemetryServiceMock);
    }

    @Test
    public void givenExistingServerAttributesAndMsgTypeAttributesDeleted_whenOnMsg_thenDeleteAttributesFromView() {
        EntityView entityView = getEntityView(SERVER_TELEMETRY_ENTITY_VIEW);

        TbMsg msg = TbMsg.newMsg()
                .type(ATTRIBUTES_DELETED)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of(DataConstants.SCOPE, AttributeScope.SERVER_SCOPE.name())))
                .data("{\"attributes\": [\"serverAttribute1\"]}")
                .build();

        mockEntityViewLookup(entityView);
        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        doAnswer(invocation -> {
            AttributesDeleteRequest request = invocation.getArgument(0);
            request.getCallback().onSuccess(null);
            return null;
        }).when(telemetryServiceMock).deleteAttributes(any());
        TbMsg newMsg = TbMsg.newMsg(msg, msg.getQueueName(), msg.getRuleChainId(), msg.getRuleNodeId());
        doAnswer(invocation -> newMsg).when(ctxMock).newMsg(any(), any(String.class), any(), any(), any(), any());

        node.onMsg(ctxMock, msg);

        verify(entityViewServiceMock).findEntityViewsByTenantIdAndEntityIdAsync(eq(TENANT_ID), eq(DEVICE_ID));
        verify(telemetryServiceMock).deleteAttributes(assertArg(request -> {
            assertThat(request.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(request.getEntityId()).isEqualTo(ENTITY_VIEW_ID);
            assertThat(request.getScope()).isEqualTo(AttributeScope.SERVER_SCOPE);
            assertThat(request.getKeys().size()).isEqualTo(1);
            assertThat(request.getKeys().get(0)).isEqualTo("serverAttribute1");
        }));
        verify(ctxMock).ack(eq(msg));
        verify(ctxMock).enqueueForTellNext(eq(newMsg), eq(TbNodeConnectionType.SUCCESS));
        verifyNoMoreInteractions(ctxMock, entityViewServiceMock, telemetryServiceMock);
    }

    @Test
    public void givenNonMatchedSharedAttributesAndMsgTypeIsAttributesDeleted_whenOnMsg_thenNoAttributesDeleteFromView() {
        EntityView entityView = getEntityView(SHARED_TELEMETRY_ENTITY_VIEW);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.ATTRIBUTES_DELETED)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of(DataConstants.SCOPE, AttributeScope.SHARED_SCOPE.name())))
                .data("{\"attributes\": [\"anotherAttribute\"]}")
                .build();

        mockEntityViewLookup(entityView);

        node.onMsg(ctxMock, msg);

        verify(entityViewServiceMock).findEntityViewsByTenantIdAndEntityIdAsync(eq(TENANT_ID), eq(DEVICE_ID));
        verify(ctxMock).ack(eq(msg));
        verifyNoMoreInteractions(ctxMock, entityViewServiceMock);
    }

    @Test
    public void givenNonMatchedAttributesAndMsgTypeIsPostAttributesRequest_whenOnMsg_thenCopyNoAttributesToView() {
        EntityView entityView = getEntityView(CLIENT_TELEMETRY_ENTITY_VIEW);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of(DataConstants.SCOPE, AttributeScope.SERVER_SCOPE.name())))
                .data("{\"clientAttribute2\": \"value2\"}")
                .build();

        mockEntityViewLookup(entityView);
        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        doAnswer(invocation -> {
            AttributesSaveRequest request = invocation.getArgument(0);
            request.getCallback().onSuccess(null);
            return null;
        }).when(telemetryServiceMock).saveAttributes(any(AttributesSaveRequest.class));
        TbMsg newMsg = TbMsg.newMsg(msg, msg.getQueueName(), msg.getRuleChainId(), msg.getRuleNodeId());
        doAnswer(invocation -> newMsg).when(ctxMock).newMsg(any(), any(String.class), any(), any(), any(), any());

        node.onMsg(ctxMock, msg);

        verify(entityViewServiceMock).findEntityViewsByTenantIdAndEntityIdAsync(eq(TENANT_ID), eq(DEVICE_ID));
        verify(telemetryServiceMock).saveAttributes(assertArg(request -> {
            assertThat(request.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(request.getEntityId()).isEqualTo(ENTITY_VIEW_ID);
            assertThat(request.getScope()).isEqualTo(AttributeScope.CLIENT_SCOPE);
            assertThat(request.getEntries().isEmpty()).isTrue();
        }));
        verify(ctxMock).ack(eq(msg));
        verify(ctxMock).enqueueForTellNext(eq(newMsg), eq(TbNodeConnectionType.SUCCESS));
        verifyNoMoreInteractions(ctxMock, entityViewServiceMock, telemetryServiceMock);
    }

    @Test
    public void givenAttributesValidityPeriodOutOfStartDateAndEndDate_whenOnMsg_thenDoNothing() {
        EntityView entityView = getEntityView(
                SERVER_TELEMETRY_ENTITY_VIEW,
                Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli(),
                Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()
        );
        mockEntityViewLookup(entityView);

        TbMsg msg = TbMsg.newMsg()
                .type(ATTRIBUTES_DELETED)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of(DataConstants.SCOPE, AttributeScope.SERVER_SCOPE.name())))
                .data("{\"attributes\": [\"serverAttribute1\"]}")
                .build();
        node.onMsg(ctxMock, msg);

        verify(entityViewServiceMock).findEntityViewsByTenantIdAndEntityIdAsync(eq(TENANT_ID), eq(DEVICE_ID));
        verify(ctxMock).ack(eq(msg));
        verifyNoMoreInteractions(ctxMock, entityViewServiceMock);
    }

    @ParameterizedTest
    @EnumSource(TbMsgType.class)
    public void givenMsgTypeAndEmptyMetadata_whenOnMsg_thenVerifyFailureMsg(TbMsgType msgType) {
        TbMsg msg = TbMsg.newMsg()
                .type(msgType)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());

        if (msg.isTypeOneOf(ATTRIBUTES_UPDATED, ATTRIBUTES_DELETED,
                ACTIVITY_EVENT, INACTIVITY_EVENT, POST_ATTRIBUTES_REQUEST)) {
            assertThat(throwableCaptor.getValue()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Message metadata is empty");
            return;
        }
        assertThat(throwableCaptor.getValue()).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported msg type [" + msgType + "]");

        verifyNoMoreInteractions(ctxMock);
    }

    private EntityView getEntityView(TelemetryEntityView attributesEntityView, long startTimeMs, long endTimeMs) {
        EntityView entityView = new EntityView(ENTITY_VIEW_ID);
        entityView.setStartTimeMs(startTimeMs);
        entityView.setEndTimeMs(endTimeMs);
        entityView.setKeys(attributesEntityView);
        return entityView;
    }

    private EntityView getEntityView(TelemetryEntityView attributesEntityView) {
        return getEntityView(attributesEntityView, ENTITY_VIEW_START_TS, ENTITY_VIEW_END_TS);
    }

    private void mockEntityViewLookup(EntityView entityView) {
        when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(entityViewServiceMock.findEntityViewsByTenantIdAndEntityIdAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(entityView)));
    }
}
