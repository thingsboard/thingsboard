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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeNotificationService implements EdgeNotificationService {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    private ExecutorService tsCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public TimePageData<EdgeEvent> findEdgeEvents(TenantId tenantId, EdgeId edgeId, TimePageLink pageLink) {
        return edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink);
    }

    @Override
    public Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) throws IOException {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = edgeService.saveEdge(edge);
        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN, ActionType.UPDATED, ruleChainId, null);
        return savedEdge;
    }

    private void saveEdgeEvent(TenantId tenantId,
                               EdgeId edgeId,
                               EdgeEventType edgeEventType,
                               ActionType edgeEventAction,
                               EntityId entityId,
                               JsonNode entityBody) {
        log.debug("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], edgeEventType [{}], edgeEventAction[{}], entityId [{}], entityBody [{}]",
                tenantId, edgeId, edgeEventType, edgeEventAction, entityId, entityBody);

        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeEventType(edgeEventType);
        edgeEvent.setEdgeEventAction(edgeEventAction.name());
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        edgeEvent.setEntityBody(entityBody);
        edgeEventService.saveAsync(edgeEvent);
    }

    @Override
    public void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        try {
            TenantId tenantId = new TenantId(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
            EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
            switch (edgeEventType) {
                // TODO: voba - handle edge updates
                // case EDGE:
                case ASSET:
                case DEVICE:
                case ENTITY_VIEW:
                case DASHBOARD:
                case RULE_CHAIN:
                    processEntities(tenantId, edgeNotificationMsg);
                    break;
                case ALARM:
                    processAlarm(tenantId, edgeNotificationMsg);
                    break;
                case RELATION:
                    processRelation(tenantId, edgeNotificationMsg);
                    break;
                default:
                    log.debug("Edge event type [{}] is not designed to be pushed to edge", edgeEventType);
            }
        } catch (Exception e) {
            callback.onFailure(e);
            log.error("Can't push to edge updates, edgeNotificationMsg [{}]", edgeNotificationMsg, e);
        } finally {
            callback.onSuccess();
        }
    }

    private void processEntities(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        ActionType edgeEventActionType = ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction());
        EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(edgeEventType, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (edgeEventActionType) {
            // TODO: voba - ADDED is not required for CE version ?
            // case ADDED:
            case UPDATED:
                ListenableFuture<List<EdgeId>> edgeIdsFuture = findRelatedEdgeIdsByEntityId(tenantId, entityId);
                Futures.transform(edgeIdsFuture, edgeIds -> {
                    if (edgeIds != null && !edgeIds.isEmpty()) {
                        for (EdgeId edgeId : edgeIds) {
                            try {
                                saveEdgeEvent(tenantId, edgeId, edgeEventType, edgeEventActionType, entityId, null);
                            } catch (Exception e) {
                                log.error("[{}] Failed to push event to edge, edgeId [{}], edgeEventType [{}], edgeEventActionType [{}], entityId [{}]",
                                        tenantId, edgeId, edgeEventType, edgeEventActionType, entityId, e);
                            }
                        }
                    }
                    return null;
                }, dbCallbackExecutorService);
                break;
            case DELETED:
                TextPageData<Edge> edgesByTenantId = edgeService.findEdgesByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
                if (edgesByTenantId != null && edgesByTenantId.getData() != null && !edgesByTenantId.getData().isEmpty()) {
                    for (Edge edge : edgesByTenantId.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), edgeEventType, edgeEventActionType, entityId, null);
                    }
                }
                break;
            case ASSIGNED_TO_EDGE:
            case UNASSIGNED_FROM_EDGE:
                EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB()));
                saveEdgeEvent(tenantId, edgeId, edgeEventType, edgeEventActionType, entityId, null);
                break;
            case RELATIONS_DELETED:
                // TODO: voba - add support for relations deleted
                break;
        }
    }

    private void processAlarm(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        AlarmId alarmId = new AlarmId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        ListenableFuture<Alarm> alarmFuture = alarmService.findAlarmByIdAsync(tenantId, alarmId);
        Futures.transform(alarmFuture, alarm -> {
            if (alarm != null) {
                EdgeEventType edgeEventType = getEdgeQueueTypeByEntityType(alarm.getOriginator().getEntityType());
                if (edgeEventType != null) {
                    ListenableFuture<List<EdgeId>> relatedEdgeIdsByEntityIdFuture = findRelatedEdgeIdsByEntityId(tenantId, alarm.getOriginator());
                    Futures.transform(relatedEdgeIdsByEntityIdFuture, relatedEdgeIdsByEntityId -> {
                        if (relatedEdgeIdsByEntityId != null) {
                            for (EdgeId edgeId : relatedEdgeIdsByEntityId) {
                                saveEdgeEvent(tenantId,
                                        edgeId,
                                        EdgeEventType.ALARM,
                                        ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction()),
                                        alarmId,
                                        null);
                            }
                        }
                        return null;
                    }, dbCallbackExecutorService);
                }
            }
            return null;
        }, dbCallbackExecutorService);
    }

    private void processRelation(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EntityRelation entityRelation = mapper.convertValue(edgeNotificationMsg.getEntityBody(), EntityRelation.class);
        List<ListenableFuture<List<EdgeId>>> futures = new ArrayList<>();
        futures.add(findRelatedEdgeIdsByEntityId(tenantId, entityRelation.getTo()));
        futures.add(findRelatedEdgeIdsByEntityId(tenantId, entityRelation.getFrom()));
        ListenableFuture<List<List<EdgeId>>> combinedFuture = Futures.allAsList(futures);
        Futures.transform(combinedFuture, listOfListsEdgeIds -> {
            Set<EdgeId> uniqueEdgeIds = new HashSet<>();
            if (listOfListsEdgeIds != null && !listOfListsEdgeIds.isEmpty()) {
                for (List<EdgeId> listOfListsEdgeId : listOfListsEdgeIds) {
                    if (listOfListsEdgeId != null) {
                        uniqueEdgeIds.addAll(listOfListsEdgeId);
                    }
                }
            }
            if (!uniqueEdgeIds.isEmpty()) {
                for (EdgeId edgeId : uniqueEdgeIds) {
                    saveEdgeEvent(tenantId,
                            edgeId,
                            EdgeEventType.RELATION,
                            ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction()),
                            null,
                            mapper.valueToTree(entityRelation));
                }
            }
            return null;
        }, dbCallbackExecutorService);
    }

    private ListenableFuture<List<EdgeId>> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId) {
        switch (entityId.getEntityType()) {
            case DEVICE:
            case ASSET:
            case ENTITY_VIEW:
                ListenableFuture<List<EntityRelation>> originatorEdgeRelationsFuture =
                        relationService.findByToAndTypeAsync(tenantId, entityId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
                return Futures.transform(originatorEdgeRelationsFuture, originatorEdgeRelations -> {
                    if (originatorEdgeRelations != null && originatorEdgeRelations.size() > 0) {
                        return Collections.singletonList(new EdgeId(originatorEdgeRelations.get(0).getFrom().getId()));
                    } else {
                        return Collections.emptyList();
                    }
                }, dbCallbackExecutorService);
            case DASHBOARD:
                return convertToEdgeIds(edgeService.findEdgesByTenantIdAndDashboardId(tenantId, new DashboardId(entityId.getId())));
            case RULE_CHAIN:
                return convertToEdgeIds(edgeService.findEdgesByTenantIdAndRuleChainId(tenantId, new RuleChainId(entityId.getId())));
            default:
                return Futures.immediateFuture(Collections.emptyList());
        }
    }

    private ListenableFuture<List<EdgeId>> convertToEdgeIds(ListenableFuture<List<Edge>> future) {
        return Futures.transform(future, edges -> {
            if (edges != null && !edges.isEmpty()) {
                return edges.stream().map(IdBased::getId).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }, dbCallbackExecutorService);
    }

    private EdgeEventType getEdgeQueueTypeByEntityType(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return EdgeEventType.DEVICE;
            case ASSET:
                return EdgeEventType.ASSET;
            case ENTITY_VIEW:
                return EdgeEventType.ENTITY_VIEW;
            default:
                log.debug("Unsupported entity type: [{}]", entityType);
                return null;
        }
    }
}


