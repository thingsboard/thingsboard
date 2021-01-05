/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "push to edge",
        configClazz = EmptyNodeConfiguration.class,
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
        configDirective = "tbNodeEmptyConfig",
        icon = "cloud_download",
        ruleChainTypes = RuleChainType.CORE
)
public class TbMsgPushToEdgeNode implements TbNode {

    private EmptyNodeConfiguration config;

    private static final ObjectMapper json = new ObjectMapper();

    private static final String SCOPE = "scope";

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (DataConstants.EDGE_MSG_SOURCE.equalsIgnoreCase(msg.getMetaData().getValue(DataConstants.MSG_SOURCE_KEY))) {
            log.debug("Ignoring msg from the cloud, msg [{}]", msg);
            ctx.ack(msg);
            return;
        }
        if (isSupportedOriginator(msg.getOriginator().getEntityType())) {
            if (isSupportedMsgType(msg.getType())) {
                processMsg(ctx, msg);
            } else {
                log.debug("Unsupported msg type {}", msg.getType());
                ctx.tellFailure(msg, new RuntimeException("Unsupported msg type '" + msg.getType() + "'"));
            }
        } else {
            log.debug("Unsupported originator type {}", msg.getOriginator().getEntityType());
            ctx.tellFailure(msg, new RuntimeException("Unsupported originator type '" + msg.getOriginator().getEntityType() + "'"));
        }
    }

    private void processMsg(TbContext ctx, TbMsg msg) {
        if (EntityType.EDGE.equals(msg.getOriginator().getEntityType())) {
            try {
                EdgeEvent edgeEvent = buildEdgeEvent(msg, ctx);
                if (edgeEvent != null) {
                    EdgeId edgeId = new EdgeId(msg.getOriginator().getId());
                    edgeEvent.setEdgeId(edgeId);
                    ListenableFuture<EdgeEvent> saveFuture = ctx.getEdgeEventService().saveAsync(edgeEvent);
                    Futures.addCallback(saveFuture, new FutureCallback<EdgeEvent>() {
                        @Override
                        public void onSuccess(@Nullable EdgeEvent event) {
                            ctx.tellNext(msg, SUCCESS);
                            ctx.onEdgeEventUpdate(ctx.getTenantId(), edgeId);
                        }

                        @Override
                        public void onFailure(Throwable th) {
                            log.warn("[{}] Can't save edge event [{}] for edge [{}]", ctx.getTenantId().getId(), edgeEvent, edgeId.getId(), th);
                            ctx.tellFailure(msg, th);
                        }
                    }, ctx.getDbCallbackExecutor());
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to build edge event", e);
                ctx.tellFailure(msg, e);
            }
        } else {
            ListenableFuture<List<EdgeId>> getEdgeIdsFuture = ctx.getEdgeService().findRelatedEdgeIdsByEntityId(ctx.getTenantId(), msg.getOriginator());
            Futures.addCallback(getEdgeIdsFuture, new FutureCallback<List<EdgeId>>() {
                @Override
                public void onSuccess(@Nullable List<EdgeId> edgeIds) {
                    if (edgeIds != null && !edgeIds.isEmpty()) {
                        for (EdgeId edgeId : edgeIds) {
                            try {
                                EdgeEvent edgeEvent = buildEdgeEvent(msg, ctx);
                                if (edgeEvent == null) {
                                    log.debug("Edge event type is null. Entity Type {}", msg.getOriginator().getEntityType());
                                    ctx.tellFailure(msg, new RuntimeException("Edge event type is null. Entity Type '" + msg.getOriginator().getEntityType() + "'"));
                                } else {
                                    edgeEvent.setEdgeId(edgeId);
                                    ListenableFuture<EdgeEvent> saveFuture = ctx.getEdgeEventService().saveAsync(edgeEvent);
                                    Futures.addCallback(saveFuture, new FutureCallback<EdgeEvent>() {
                                        @Override
                                        public void onSuccess(@Nullable EdgeEvent event) {
                                            ctx.tellNext(msg, SUCCESS);
                                            ctx.onEdgeEventUpdate(ctx.getTenantId(), edgeId);
                                        }

                                        @Override
                                        public void onFailure(Throwable th) {
                                            log.warn("[{}] Can't save edge event [{}] for edge [{}]", ctx.getTenantId().getId(), edgeEvent, edgeId.getId(), th);
                                            ctx.tellFailure(msg, th);
                                        }
                                    }, ctx.getDbCallbackExecutor());
                                }
                            } catch (JsonProcessingException e) {
                                log.error("Failed to build edge event", e);
                                ctx.tellFailure(msg, e);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    ctx.tellFailure(msg, t);
                }

            }, ctx.getDbCallbackExecutor());
        }
    }

    private EdgeEvent buildEdgeEvent(TbMsg msg, TbContext ctx) throws JsonProcessingException {
        String msgType = msg.getType();
        if (DataConstants.ALARM.equals(msgType)) {
            return buildEdgeEvent(ctx.getTenantId(), EdgeEventActionType.ADDED, getUUIDFromMsgData(msg), EdgeEventType.ALARM, null);
        } else {
            EdgeEventType edgeEventTypeByEntityType = EdgeUtils.getEdgeEventTypeByEntityType(msg.getOriginator().getEntityType());
            if (edgeEventTypeByEntityType == null) {
                return null;
            }
            EdgeEventActionType actionType = getEdgeEventActionTypeByMsgType(msgType);
            Map<String, Object> entityBody = new HashMap<>();
            Map<String, String> metadata = msg.getMetaData().getData();
            JsonNode dataJson = json.readTree(msg.getData());
            switch (actionType) {
                case ATTRIBUTES_UPDATED:
                case POST_ATTRIBUTES:
                    entityBody.put("kv", dataJson);
                    entityBody.put(SCOPE, getScope(metadata));
                    break;
                case ATTRIBUTES_DELETED:
                    List<String> keys = json.treeToValue(dataJson.get("attributes"), List.class);
                    entityBody.put("keys", keys);
                    entityBody.put(SCOPE, getScope(metadata));
                    break;
                case TIMESERIES_UPDATED:
                    entityBody.put("data", dataJson);
                    entityBody.put("ts", metadata.get("ts"));
                    break;
            }
            return buildEdgeEvent(ctx.getTenantId(), actionType, msg.getOriginator().getId(), edgeEventTypeByEntityType, json.valueToTree(entityBody));
        }
    }

    private String getScope(Map<String, String> metadata) {
        String scope = metadata.get(SCOPE);
        if (StringUtils.isEmpty(scope)) {
            // TODO: voba - move this to configuration of the node UI or some other place
            scope = DataConstants.SERVER_SCOPE;
        }
        return scope;
    }

    private EdgeEvent buildEdgeEvent(TenantId tenantId, EdgeEventActionType edgeEventAction, UUID entityId, EdgeEventType edgeEventType, JsonNode entityBody) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setEntityId(entityId);
        edgeEvent.setType(edgeEventType);
        edgeEvent.setBody(entityBody);
        return edgeEvent;
    }

    private UUID getUUIDFromMsgData(TbMsg msg) throws JsonProcessingException {
        JsonNode data = json.readTree(msg.getData()).get("id");
        String id = json.treeToValue(data.get("id"), String.class);
        return UUID.fromString(id);
    }

    private EdgeEventActionType getEdgeEventActionTypeByMsgType(String msgType) {
        EdgeEventActionType actionType;
        if (SessionMsgType.POST_TELEMETRY_REQUEST.name().equals(msgType)) {
            actionType = EdgeEventActionType.TIMESERIES_UPDATED;
        } else if (DataConstants.ATTRIBUTES_UPDATED.equals(msgType)) {
            actionType = EdgeEventActionType.ATTRIBUTES_UPDATED;
        } else if (SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msgType)) {
            actionType = EdgeEventActionType.POST_ATTRIBUTES;
        } else {
            actionType = EdgeEventActionType.ATTRIBUTES_DELETED;
        }
        return actionType;
    }

    private boolean isSupportedOriginator(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
            case ASSET:
            case ENTITY_VIEW:
            case DASHBOARD:
            case TENANT:
            case CUSTOMER:
            case EDGE:
                return true;
            default:
                return false;
        }
    }

    private boolean isSupportedMsgType(String msgType) {
        return SessionMsgType.POST_TELEMETRY_REQUEST.name().equals(msgType)
                || SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msgType)
                || DataConstants.ATTRIBUTES_UPDATED.equals(msgType)
                || DataConstants.ATTRIBUTES_DELETED.equals(msgType)
                || DataConstants.ALARM.equals(msgType);
    }

    @Override
    public void destroy() {
    }

}
