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
package org.thingsboard.rule.engine.rpc;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.edge.EdgeEventService;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbSendRPCReplyNodeTest {

    private static final String DUMMY_SERVICE_ID = "testServiceId";
    private static final int DUMMY_REQUEST_ID = 0;
    private static final UUID DUMMY_SESSION_ID = UUID.randomUUID();
    private static final String DUMMY_DATA = "{\"key\":\"value\"}";

    TbSendRPCReplyNode node;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    @Mock
    private TbContext ctx;

    @Mock
    private RuleEngineRpcService rpcService;

    @Mock
    private EdgeEventService edgeEventService;

    @Mock
    private ListeningExecutor listeningExecutor;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new TbSendRPCReplyNode();
        TbSendRpcReplyNodeConfiguration config = new TbSendRpcReplyNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @Test
    public void sendReplyToTransport() {
        when(ctx.getRpcService()).thenReturn(rpcService);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, getDefaultMetadata(),
                TbMsgDataType.JSON, DUMMY_DATA, null, null);

        node.onMsg(ctx, msg);

        verify(rpcService).sendRpcReplyToDevice(DUMMY_SERVICE_ID, DUMMY_SESSION_ID, DUMMY_REQUEST_ID, DUMMY_DATA);
        verify(edgeEventService, never()).saveAsync(any());
    }

    @Test
    public void sendReplyToEdgeQueue() {
        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());
        when(ctx.getDbCallbackExecutor()).thenReturn(listeningExecutor);

        TbMsgMetaData defaultMetadata = getDefaultMetadata();
        defaultMetadata.putValue(DataConstants.EDGE_ID, UUID.randomUUID().toString());
        defaultMetadata.putValue(DataConstants.DEVICE_ID, UUID.randomUUID().toString());
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, defaultMetadata,
                TbMsgDataType.JSON, DUMMY_DATA, null, null);

        node.onMsg(ctx, msg);

        verify(edgeEventService).saveAsync(any());
        verify(rpcService, never()).sendRpcReplyToDevice(DUMMY_SERVICE_ID, DUMMY_SESSION_ID, DUMMY_REQUEST_ID, DUMMY_DATA);
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    public void testOriginatorEntityTypes(EntityType entityType) throws TbNodeException {
        if (entityType == EntityType.DEVICE) return;
        EntityId entityId = new EntityId() {
            @Override
            public UUID getId() {
                return UUID.randomUUID();
            }

            @Override
            public EntityType getEntityType() {
                return entityType;
            }
        };
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, entityId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        node.onMsg(ctx, msg);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(eq(msg), captor.capture());
        Throwable value = captor.getValue();
        assertThat(value.getClass()).isEqualTo(RuntimeException.class);
        assertThat(value.getMessage()).isEqualTo("Message originator is not a device entity!");
    }

    @Test
    public void testForAvailabilityOfMetadataAndDataValues2() {
        //without requestId
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        verifyFailure(msg, "Request id is not present in the metadata!");

        //without serviceId
        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue("requestId", Integer.toString(DUMMY_REQUEST_ID));
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, metadata, TbMsg.EMPTY_JSON_OBJECT);
        verifyFailure(msg, "Service id is not present in the metadata!");

        //without sessionId
        metadata.putValue("serviceId", DUMMY_SERVICE_ID);
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, metadata, TbMsg.EMPTY_JSON_OBJECT);
        verifyFailure(msg, "Session id is not present in the metadata!");

        //with empty data
        metadata.putValue("sessionId", DUMMY_SESSION_ID.toString());
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, metadata, TbMsg.EMPTY_STRING);
        verifyFailure(msg, "Request body is empty!");
    }

    private void verifyFailure(TbMsg msg, String expectedErrorMessage) {
        node.onMsg(ctx, msg);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(eq(msg), captor.capture());
        Throwable value = captor.getValue();
        assertThat(value.getClass()).isEqualTo(RuntimeException.class);
        assertThat(value.getMessage()).isEqualTo(expectedErrorMessage);
    }

    private TbMsgMetaData getDefaultMetadata() {
        TbSendRpcReplyNodeConfiguration config = new TbSendRpcReplyNodeConfiguration().defaultConfiguration();
        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(config.getServiceIdMetaDataAttribute(), DUMMY_SERVICE_ID);
        metadata.putValue(config.getSessionIdMetaDataAttribute(), DUMMY_SESSION_ID.toString());
        metadata.putValue(config.getRequestIdMetaDataAttribute(), Integer.toString(DUMMY_REQUEST_ID));
        return metadata;
    }
}
