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
package org.thingsboard.rule.engine.edge;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbMsgPushToEdgeNodeTest {

    TbMsgPushToEdgeNode node;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    @Mock
    private TbContext ctx;

    @Mock
    private EdgeService edgeService;
    @Mock
    private EdgeEventService edgeEventService;
    @Mock
    private ListeningExecutor dbCallbackExecutor;

    @Before
    public void setUp() throws TbNodeException {
        node = new TbMsgPushToEdgeNode();
        TbMsgPushToEdgeNodeConfiguration config = new TbMsgPushToEdgeNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @Test
    public void ackMsgInCaseNoEdgeRelated() {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(edgeService.findRelatedEdgeIdsByEntityId(tenantId, deviceId, new PageLink(TbMsgPushToEdgeNode.DEFAULT_PAGE_SIZE))).thenReturn(new PageData<>());

        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, "{}", null, null);

        node.onMsg(ctx, msg);

        verify(ctx).ack(msg);
    }

    @Test
    public void testAttributeUpdateMsg_userEntity() {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());

        UserId userId = new UserId(UUID.randomUUID());
        EdgeId edgeId = new EdgeId(UUID.randomUUID());
        PageData<EdgeId> edgePageData = new PageData<>(List.of(edgeId), 1, 1, false);
        Mockito.when(edgeService.findRelatedEdgeIdsByEntityId(tenantId, userId, new PageLink(TbMsgPushToEdgeNode.DEFAULT_PAGE_SIZE))).thenReturn(edgePageData);

        TbMsg msg = TbMsg.newMsg(DataConstants.ATTRIBUTES_UPDATED, userId, new TbMsgMetaData(),
                TbMsgDataType.JSON, "{}", null, null);

        node.onMsg(ctx, msg);

        verify(edgeEventService).saveAsync(any());
    }

    @Test
    public void testMiscEventsProcessedAsAttributesUpdated() {
        List<String> miscEvents = List.of(DataConstants.CONNECT_EVENT, DataConstants.DISCONNECT_EVENT,
                DataConstants.ACTIVITY_EVENT, DataConstants.INACTIVITY_EVENT);
        for (String event : miscEvents) {
            Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
            Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
            Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
            Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
            Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());

            TbMsg msg = TbMsg.newMsg(event, new EdgeId(UUID.randomUUID()), new TbMsgMetaData(),
                    TbMsgDataType.JSON, "{\"lastConnectTs\":1}", null, null);

            node.onMsg(ctx, msg);

            ArgumentMatcher<EdgeEvent> eventArgumentMatcher = edgeEvent ->
                    edgeEvent.getAction().equals(EdgeEventActionType.ATTRIBUTES_UPDATED)
                            && edgeEvent.getBody().get("kv").get("lastConnectTs").asInt() == 1;
            verify(edgeEventService).saveAsync(Mockito.argThat(eventArgumentMatcher));

            Mockito.reset(ctx, edgeEventService);
        }
    }
}
