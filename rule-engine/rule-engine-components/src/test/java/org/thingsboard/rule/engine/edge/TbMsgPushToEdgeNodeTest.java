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
package org.thingsboard.rule.engine.edge;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbMsgPushToEdgeNodeTest extends AbstractRuleNodeUpgradeTest {

    private static final List<TbMsgType> MISC_EVENTS = List.of(TbMsgType.CONNECT_EVENT, TbMsgType.DISCONNECT_EVENT,
            TbMsgType.ACTIVITY_EVENT, TbMsgType.INACTIVITY_EVENT);

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

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, TbMsgMetaData.EMPTY,
                TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, null, null);

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

        TbMsg msg = TbMsg.newMsg(TbMsgType.ATTRIBUTES_UPDATED, userId, TbMsgMetaData.EMPTY,
                TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, null, null);

        node.onMsg(ctx, msg);

        verify(edgeEventService).saveAsync(any());
    }

    @Test
    public void testMiscEventsProcessedAsAttributesUpdated() {
        for (var event : MISC_EVENTS) {
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue(DataConstants.SCOPE, DataConstants.SERVER_SCOPE);
            testEvent(event, metaData, EdgeEventActionType.ATTRIBUTES_UPDATED, "kv");
        }
    }

    @Test
    public void testMiscEventsProcessedAsTimeseriesUpdated() {
        for (var event : MISC_EVENTS) {
            testEvent(event, TbMsgMetaData.EMPTY, EdgeEventActionType.TIMESERIES_UPDATED, "data");
        }
    }

    @Test
    public void testAttributeUpdateMsgUseTemplateIsTrueValidScope() throws TbNodeException {
        node = new TbMsgPushToEdgeNode();
        TbMsgPushToEdgeNodeConfiguration config = new TbMsgPushToEdgeNodeConfiguration();
        config.setScope("${attributesScope}");
        config.setUseAttributesScopeTemplate(true);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        EdgeId edgeId = new EdgeId(UUID.randomUUID());
        PageData<EdgeId> edgePageData = new PageData<>(List.of(edgeId), 1, 1, false);

        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(edgeService.findRelatedEdgeIdsByEntityId(any(), any(), any())).thenReturn(edgePageData);
        Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        Mockito.when(edgeService.findRelatedEdgeIdsByEntityId(tenantId, deviceId, new PageLink(TbMsgPushToEdgeNode.DEFAULT_PAGE_SIZE))).thenReturn(edgePageData);
        Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());

        Map<String, String> metadata = Map.of(
                "attributesScope", "SERVER_SCOPE"
        );
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, new TbMsgMetaData(metadata), TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctx, msg);

        ArgumentMatcher<EdgeEvent> eventArgumentMatcher = edgeEvent ->
                edgeEvent.getBody().get("scope").equals(JacksonUtil.valueToTree(AttributeScope.SERVER_SCOPE));
        verify(edgeEventService).saveAsync(Mockito.argThat(eventArgumentMatcher));
    }

    @Test
    public void testAttributeUpdateMsgUseTemplateIsTrueInvalidScope() throws TbNodeException {
        node = new TbMsgPushToEdgeNode();
        TbMsgPushToEdgeNodeConfiguration config = new TbMsgPushToEdgeNodeConfiguration();
        config.setScope("${attributesScope}");
        config.setUseAttributesScopeTemplate(true);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        EdgeId edgeId = new EdgeId(UUID.randomUUID());
        PageData<EdgeId> edgePageData = new PageData<>(List.of(edgeId), 1, 1, false);

        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(edgeService.findRelatedEdgeIdsByEntityId(any(), any(), any())).thenReturn(edgePageData);

        Map<String, String> metadata = Map.of(
                "attributesScope", "ANOTHER_SCOPE"
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, new TbMsgMetaData(metadata), TbMsg.EMPTY_JSON_OBJECT);

        node.onMsg(ctx, msg);

        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(eq(msg), exceptionCaptor.capture());
        Throwable exception = exceptionCaptor.getValue();
        assertEquals(IllegalArgumentException.class, exception.getClass());
        assertEquals("Failed to parse scope! No enum constant for name: " + msg.getMetaData().getValue("attributesScope"), exception.getMessage());
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                //config for version 0
                Arguments.of(0,
                        "{\"scope\": \"SERVER_SCOPE\"}",
                        true,
                        "{\"scope\": \"SERVER_SCOPE\", \"useAttributesScopeTemplate\": false}"
                ),
                //config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\"scope\": \"SERVER_SCOPE\", \"useAttributesScopeTemplate\": false}",
                        false,
                        "{\"scope\": \"SERVER_SCOPE\", \"useAttributesScopeTemplate\": false}"
                )
        );
    }

    @Override
    protected TbNode getTestNode() {
        return spy(new TbMsgPushToEdgeNode());
    }

    private void testEvent(TbMsgType event, TbMsgMetaData metaData, EdgeEventActionType expectedType, String dataKey) {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());

        TbMsg msg = TbMsg.newMsg(event, new EdgeId(UUID.randomUUID()), metaData,
                TbMsgDataType.JSON, "{\"lastConnectTs\":1}", null, null);

        node.onMsg(ctx, msg);

        ArgumentMatcher<EdgeEvent> eventArgumentMatcher = edgeEvent ->
                edgeEvent.getAction().equals(expectedType)
                        && edgeEvent.getBody().get(dataKey).get("lastConnectTs").asInt() == 1;
        verify(edgeEventService).saveAsync(Mockito.argThat(eventArgumentMatcher));

        Mockito.reset(ctx, edgeEventService);
    }
}
