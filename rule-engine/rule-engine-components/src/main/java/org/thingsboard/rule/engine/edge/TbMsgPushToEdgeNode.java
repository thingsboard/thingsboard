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
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "push to edge",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Pushes messages to edge",
        nodeDetails = "Pushes messages to edge, if Message Originator assigned to particular edge or is EDGE entity. This node is used only on Cloud instances to push messages from Cloud to Edge. Supports only DEVICE, ENTITY_VIEW, ASSET and EDGE Message Originator(s).",
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbNodeEmptyConfig",
        icon = "cloud_download",
        ruleChainTypes = RuleChainType.CORE
)
public class TbMsgPushToEdgeNode implements TbNode {

    private EmptyNodeConfiguration config;

    private static final ObjectMapper json = new ObjectMapper();

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (DataConstants.EDGE_MSG_SOURCE.equalsIgnoreCase(msg.getMetaData().getValue(DataConstants.MSG_SOURCE_KEY))) {
            log.debug("Ignoring msg from the cloud, msg [{}]", msg);
            return;
        }
        if (isSupportedOriginator(msg.getOriginator().getEntityType())) {
            if (isSupportedMsgType(msg.getType())) {
                ListenableFuture<List<EdgeId>> getEdgeIdsFuture = getEdgeIdsByOriginatorId(ctx, ctx.getTenantId(), msg.getOriginator());
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
                                            }

                                            @Override
                                            public void onFailure(Throwable th) {
                                                log.error("Could not save edge event", th);
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
            } else {
                log.debug("Unsupported msg type {}", msg.getType());
                ctx.tellFailure(msg, new RuntimeException("Unsupported msg type '" + msg.getType() + "'"));
            }
        } else {
            log.debug("Unsupported originator type {}", msg.getOriginator().getEntityType());
            ctx.tellFailure(msg, new RuntimeException("Unsupported originator type '" + msg.getOriginator().getEntityType() + "'"));
        }
    }

    private EdgeEvent buildEdgeEvent(TbMsg msg, TbContext ctx) throws JsonProcessingException {
        if (DataConstants.ALARM.equals(msg.getType())) {
            return buildEdgeEvent(ctx.getTenantId(), ActionType.ADDED, getUUIDFromMsgData(msg), EdgeEventType.ALARM, null);
        } else {
            EdgeEventType edgeEventTypeByEntityType = EdgeUtils.getEdgeEventTypeByEntityType(msg.getOriginator().getEntityType());
            if (edgeEventTypeByEntityType == null) {
                return null;
            }
            ActionType actionType = getActionTypeByMsgType(msg.getType());
            JsonNode entityBody = null;
            JsonNode data = json.readTree(msg.getData());
            if (actionType.equals(ActionType.ATTRIBUTES_UPDATED) || actionType.equals(ActionType.ATTRIBUTES_DELETED)) {
                entityBody = getAttributeEntityBody(actionType, data, msg.getMetaData().getData());
            } else {
                entityBody = data;
            }
            return buildEdgeEvent(ctx.getTenantId(), actionType, msg.getOriginator().getId(), edgeEventTypeByEntityType, entityBody);
        }
    }

    private EdgeEvent buildEdgeEvent(TenantId tenantId, ActionType edgeEventAction, UUID entityId, EdgeEventType edgeEventType, JsonNode entityBody) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeEventAction(edgeEventAction.name());
        edgeEvent.setEntityId(entityId);
        edgeEvent.setEdgeEventType(edgeEventType);
        edgeEvent.setEntityBody(entityBody);
        return edgeEvent;
    }

    private JsonNode getAttributeEntityBody(ActionType actionType, JsonNode data, Map<String, String> metadata) throws JsonProcessingException {
        Map<String, Object> entityData = new HashMap<>();
        switch (actionType) {
            case ATTRIBUTES_UPDATED:
                entityData.put("kv", data);
                break;
            case ATTRIBUTES_DELETED:
                List<String> keys = json.treeToValue(data.get("attributes"), List.class);
                entityData.put("keys", keys);
                break;
        }
        entityData.put("scope", metadata.get("scope"));
        return json.valueToTree(entityData);
    }

    private UUID getUUIDFromMsgData(TbMsg msg) throws JsonProcessingException {
        JsonNode data = json.readTree(msg.getData()).get("id");
        String id = json.treeToValue(data.get("id"), String.class);
        return UUID.fromString(id);
    }

    private ActionType getActionTypeByMsgType(String msgType) {
        ActionType actionType;
        if (SessionMsgType.POST_TELEMETRY_REQUEST.name().equals(msgType)) {
            actionType = ActionType.TIMESERIES_UPDATED;
        } else if (SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msgType)
                || DataConstants.ATTRIBUTES_UPDATED.equals(msgType)) {
            actionType = ActionType.ATTRIBUTES_UPDATED;
        } else {
            actionType = ActionType.ATTRIBUTES_DELETED;
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

    private ListenableFuture<List<EdgeId>> getEdgeIdsByOriginatorId(TbContext ctx, TenantId tenantId, EntityId originatorId) {
        if (EntityType.TENANT.equals(originatorId.getEntityType())) {
            TextPageData<Edge> edgesByTenantId = ctx.getEdgeService().findEdgesByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
            return Futures.immediateFuture(edgesByTenantId.getData().stream().map(IdBased::getId).collect(Collectors.toList()));
        } else {
            ListenableFuture<List<EntityRelation>> future = ctx.getRelationService().findByToAndTypeAsync(tenantId, originatorId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
            return Futures.transform(future, relations -> {
                List<EdgeId> result = new ArrayList<>();
                if (relations != null && relations.size() > 0) {
                    result.add(new EdgeId(relations.get(0).getFrom().getId()));
                }
                return result;
            }, ctx.getDbCallbackExecutor());
        }
    }

    @Override
    public void destroy() {
    }

}
