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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "push to edge",
        configClazz = TbMsgPushToEdgeNodeConfiguration.class,
        nodeDescription = "Push messages from cloud to edge",
        nodeDetails = "Push messages from cloud to edge. " +
                "Message originator must be assigned to particular edge or message originator is <b>EDGE</b> entity itself. " +
                "This node used only on cloud instances to push messages from cloud to edge. " +
                "Once message arrived into this node it’s going to be converted into edge event and saved to the database. " +
                "Node doesn't push messages directly to edge, but stores event(s) in the edge queue. " +
                "<br>Supports next originator types:" +
                "<br><code>DEVICE</code>" +
                "<br><code>ASSET</code>" +
                "<br><code>ENTITY_VIEW</code>" +
                "<br><code>DASHBOARD</code>" +
                "<br><code>TENANT</code>" +
                "<br><code>CUSTOMER</code>" +
                "<br><code>EDGE</code><br><br>" +
                "As well node supports next message types:" +
                "<br><code>POST_TELEMETRY_REQUEST</code>" +
                "<br><code>POST_ATTRIBUTES_REQUEST</code>" +
                "<br><code>ATTRIBUTES_UPDATED</code>" +
                "<br><code>ATTRIBUTES_DELETED</code>" +
                "<br><code>ALARM</code><br><br>" +
                "Message will be routed via <b>Failure</b> route if node was not able to save edge event to database or unsupported originator type/message type arrived. " +
                "In case successful storage edge event to database message will be routed via <b>Success</b> route.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodePushToEdgeConfig",
        icon = "cloud_download",
        ruleChainTypes = RuleChainType.CORE
)
public class TbMsgPushToEdgeNode extends AbstractTbMsgPushNode<TbMsgPushToEdgeNodeConfiguration, EdgeEvent, EdgeEventType> {

    static final int DEFAULT_PAGE_SIZE = 100;

    @Override
    EdgeEvent buildEvent(TenantId tenantId, EdgeEventActionType eventAction, UUID entityId,
                         EdgeEventType eventType, JsonNode entityBody) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(eventAction);
        edgeEvent.setEntityId(entityId);
        edgeEvent.setType(eventType);
        edgeEvent.setBody(entityBody);
        return edgeEvent;
    }

    @Override
    EdgeEventType getEventTypeByEntityType(EntityType entityType) {
        return EdgeUtils.getEdgeEventTypeByEntityType(entityType);
    }

    @Override
    EdgeEventType getAlarmEventType() {
        return EdgeEventType.ALARM;
    }

    @Override
    String getIgnoredMessageSource() {
        return DataConstants.EDGE_MSG_SOURCE;
    }

    @Override
    protected Class<TbMsgPushToEdgeNodeConfiguration> getConfigClazz() {
        return TbMsgPushToEdgeNodeConfiguration.class;
    }

    @Override
    protected void processMsg(TbContext ctx, TbMsg msg) {
        try {
            if (EntityType.EDGE.equals(msg.getOriginator().getEntityType())) {
                EdgeEvent edgeEvent = buildEvent(msg, ctx);
                EdgeId edgeId = new EdgeId(msg.getOriginator().getId());
                ListenableFuture<Void> future = notifyEdge(ctx, edgeEvent, edgeId);
                FutureCallback<Void> futureCallback = new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Void result) {
                        ctx.tellSuccess(msg);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ctx.tellFailure(msg, t);
                    }
                };
                Futures.addCallback(future, futureCallback, ctx.getDbCallbackExecutor());
            } else {
                PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
                PageData<EdgeId> pageData;
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                do {
                    pageData = ctx.getEdgeService().findRelatedEdgeIdsByEntityId(ctx.getTenantId(), msg.getOriginator(), pageLink);
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        for (EdgeId edgeId : pageData.getData()) {
                            EdgeEvent edgeEvent = buildEvent(msg, ctx);
                            futures.add(notifyEdge(ctx, edgeEvent, edgeId));
                        }
                        if (pageData.hasNext()) {
                            pageLink = pageLink.nextPageLink();
                        }
                    }
                } while (pageData != null && pageData.hasNext());

                if (futures.isEmpty()) {
                    // ack in case no edges are related to provided entity
                    ctx.ack(msg);
                } else {
                    Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable List<Void> voids) {
                            ctx.tellSuccess(msg);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            ctx.tellFailure(msg, t);
                        }
                    }, ctx.getDbCallbackExecutor());
                }
            }
        } catch (Exception e) {
            log.error("Failed to build edge event", e);
            ctx.tellFailure(msg, e);
        }
    }

    private ListenableFuture<Void> notifyEdge(TbContext ctx, EdgeEvent edgeEvent, EdgeId edgeId) {
        edgeEvent.setEdgeId(edgeId);
        ListenableFuture<Void> future = ctx.getEdgeEventService().saveAsync(edgeEvent);
        return Futures.transform(future, result -> {
            ctx.onEdgeEventUpdate(ctx.getTenantId(), edgeId);
            return null;
        }, ctx.getDbCallbackExecutor());
    }

}
