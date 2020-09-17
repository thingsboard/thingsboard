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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private UserService userService;

    @Autowired
    private RuleChainService ruleChainService;

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
        return edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink, true);
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
        edgeEvent.setType(edgeEventType);
        edgeEvent.setAction(edgeEventAction.name());
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        edgeEvent.setBody(entityBody);
        edgeEventService.saveAsync(edgeEvent);
    }

    @Override
    public void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        try {
            TenantId tenantId = new TenantId(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
            EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
            switch (edgeEventType) {
                case EDGE:
                    processEdge(tenantId, edgeNotificationMsg);
                    break;
                case USER:
                case ASSET:
                case DEVICE:
                case ENTITY_VIEW:
                case DASHBOARD:
                case RULE_CHAIN:
                    processEntity(tenantId, edgeNotificationMsg);
                    break;
                case CUSTOMER:
                    processCustomer(tenantId, edgeNotificationMsg);
                    break;
                case WIDGETS_BUNDLE:
                case WIDGET_TYPE:
                    processWidgetBundleOrWidgetType(tenantId, edgeNotificationMsg);
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

    private void processEdge(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        try {
            ActionType edgeEventActionType = ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction());
            EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB()));
            ListenableFuture<Edge> edgeFuture;
            switch (edgeEventActionType) {
                case ASSIGNED_TO_CUSTOMER:
                    CustomerId customerId = mapper.readValue(edgeNotificationMsg.getEntityBody(), CustomerId.class);
                    edgeFuture = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                    Futures.addCallback(edgeFuture, new FutureCallback<Edge>() {
                        @Override
                        public void onSuccess(@Nullable Edge edge) {
                            if (edge != null && !customerId.isNullUid()) {
                                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, ActionType.ADDED, customerId, null);
                                TextPageData<User> pageData = userService.findCustomerUsers(tenantId, customerId, new TextPageLink(Integer.MAX_VALUE));
                                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                                    log.trace("[{}] [{}] user(s) are going to be added to edge.", edge.getId(), pageData.getData().size());
                                    for (User user : pageData.getData()) {
                                        saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, ActionType.ADDED, user.getId(), null);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.error("Can't find edge by id [{}]", edgeNotificationMsg, t);
                        }
                    }, dbCallbackExecutorService);
                    break;
                case UNASSIGNED_FROM_CUSTOMER:
                    CustomerId customerIdToDelete = mapper.readValue(edgeNotificationMsg.getEntityBody(), CustomerId.class);
                    edgeFuture = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                    Futures.addCallback(edgeFuture, new FutureCallback<Edge>() {
                        @Override
                        public void onSuccess(@Nullable Edge edge) {
                            if (edge != null && !customerIdToDelete.isNullUid()) {
                                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, ActionType.DELETED, customerIdToDelete, null);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.error("Can't find edge by id [{}]", edgeNotificationMsg, t);
                        }
                    }, dbCallbackExecutorService);
                    break;
            }
        } catch (Exception e) {
            log.error("Exception during processing edge event", e);
        }
    }

    private void processWidgetBundleOrWidgetType(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        ActionType edgeEventActionType = ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction());
        EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(edgeEventType, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (edgeEventActionType) {
            case ADDED:
            case UPDATED:
            case DELETED:
                TextPageData<Edge> edgesByTenantId = edgeService.findEdgesByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
                if (edgesByTenantId != null && edgesByTenantId.getData() != null && !edgesByTenantId.getData().isEmpty()) {
                    for (Edge edge : edgesByTenantId.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), edgeEventType, edgeEventActionType, entityId, null);
                    }
                }
                break;
        }
    }

    private void processCustomer(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        ActionType edgeEventActionType = ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction());
        EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(edgeEventType, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        TextPageData<Edge> edgesByTenantId = edgeService.findEdgesByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
        if (edgesByTenantId != null && edgesByTenantId.getData() != null && !edgesByTenantId.getData().isEmpty()) {
            for (Edge edge : edgesByTenantId.getData()) {
                switch (edgeEventActionType) {
                    case UPDATED:
                        if (!edge.getCustomerId().isNullUid() && edge.getCustomerId().equals(entityId)) {
                            saveEdgeEvent(tenantId, edge.getId(), edgeEventType, edgeEventActionType, entityId, null);
                        }
                        break;
                    case DELETED:
                        saveEdgeEvent(tenantId, edge.getId(), edgeEventType, edgeEventActionType, entityId, null);
                        break;
                }
            }
        }
    }

    private void processEntity(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        ActionType edgeEventActionType = ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction());
        EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(edgeEventType,
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        ListenableFuture<List<EdgeId>> edgeIdsFuture;
        switch (edgeEventActionType) {
            case ADDED: // used only for USER entity
            case UPDATED:
            case CREDENTIALS_UPDATED:
                edgeIdsFuture = edgeService.findRelatedEdgeIdsByEntityId(tenantId, entityId);
                Futures.addCallback(edgeIdsFuture, new FutureCallback<List<EdgeId>>() {
                    @Override
                    public void onSuccess(@Nullable List<EdgeId> edgeIds) {
                        if (edgeIds != null && !edgeIds.isEmpty()) {
                            for (EdgeId edgeId : edgeIds) {
                                saveEdgeEvent(tenantId, edgeId, edgeEventType, edgeEventActionType, entityId, null);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Throwable throwable) {
                        log.error("Failed to find related edge ids [{}]", edgeNotificationMsg, throwable);
                    }
                }, dbCallbackExecutorService);
                break;
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                edgeIdsFuture = edgeService.findRelatedEdgeIdsByEntityId(tenantId, entityId);
                Futures.addCallback(edgeIdsFuture, new FutureCallback<List<EdgeId>>() {
                    @Override
                    public void onSuccess(@Nullable List<EdgeId> edgeIds) {
                        if (edgeIds != null && !edgeIds.isEmpty()) {
                            for (EdgeId edgeId : edgeIds) {
                                try {
                                    CustomerId customerId = mapper.readValue(edgeNotificationMsg.getEntityBody(), CustomerId.class);
                                    ListenableFuture<Edge> future = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                                    Futures.addCallback(future, new FutureCallback<Edge>() {
                                        @Override
                                        public void onSuccess(@Nullable Edge edge) {
                                            if (edge != null && edge.getCustomerId() != null &&
                                                    !edge.getCustomerId().isNullUid() && edge.getCustomerId().equals(customerId)) {
                                                saveEdgeEvent(tenantId, edgeId, edgeEventType, edgeEventActionType, entityId, null);
                                            }
                                        }
                                        @Override
                                        public void onFailure(Throwable throwable) {
                                            log.error("Failed to find edge by id [{}]", edgeNotificationMsg, throwable);
                                        }
                                    }, dbCallbackExecutorService);
                                } catch (Exception e) {
                                    log.error("Can't parse customer id from entity body [{}]", edgeNotificationMsg, e);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        log.error("Failed to find related edge ids [{}]", edgeNotificationMsg, throwable);
                    }
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
                if (edgeEventType.equals(EdgeEventType.RULE_CHAIN)) {
                    updateDependentRuleChains(tenantId, new RuleChainId(entityId.getId()), edgeId);
                }
                break;
        }
    }

    private void updateDependentRuleChains(TenantId tenantId, RuleChainId processingRuleChainId, EdgeId edgeId) {
        ListenableFuture<TimePageData<RuleChain>> future = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, new TimePageLink(Integer.MAX_VALUE));
        Futures.addCallback(future, new FutureCallback<TimePageData<RuleChain>>() {
            @Override
            public void onSuccess(@Nullable TimePageData<RuleChain> pageData) {
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    for (RuleChain ruleChain : pageData.getData()) {
                        if (!ruleChain.getId().equals(processingRuleChainId)) {
                            List<RuleChainConnectionInfo> connectionInfos =
                                    ruleChainService.loadRuleChainMetaData(ruleChain.getTenantId(), ruleChain.getId()).getRuleChainConnections();
                            if (connectionInfos != null && !connectionInfos.isEmpty()) {
                                for (RuleChainConnectionInfo connectionInfo : connectionInfos) {
                                    if (connectionInfo.getTargetRuleChainId().equals(processingRuleChainId)) {
                                        saveEdgeEvent(tenantId,
                                                edgeId,
                                                EdgeEventType.RULE_CHAIN_METADATA,
                                                ActionType.UPDATED,
                                                ruleChain.getId(),
                                                null);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Exception during updating dependent rule chains on sync!", t);
            }
        }, dbCallbackExecutorService);
    }

    private void processAlarm(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        AlarmId alarmId = new AlarmId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        ListenableFuture<Alarm> alarmFuture = alarmService.findAlarmByIdAsync(tenantId, alarmId);
        Futures.transform(alarmFuture, alarm -> {
            if (alarm != null) {
                EdgeEventType edgeEventType = getEdgeQueueTypeByEntityType(alarm.getOriginator().getEntityType());
                if (edgeEventType != null) {
                    ListenableFuture<List<EdgeId>> relatedEdgeIdsByEntityIdFuture = edgeService.findRelatedEdgeIdsByEntityId(tenantId, alarm.getOriginator());
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

    private void processRelation(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) throws JsonProcessingException {
        EntityRelation relation = mapper.readValue(edgeNotificationMsg.getEntityBody(), EntityRelation.class);
        if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
            List<ListenableFuture<List<EdgeId>>> futures = new ArrayList<>();
            futures.add(edgeService.findRelatedEdgeIdsByEntityId(tenantId, relation.getTo()));
            futures.add(edgeService.findRelatedEdgeIdsByEntityId(tenantId, relation.getFrom()));
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
                                mapper.valueToTree(relation));
                    }
                }
                return null;
            }, dbCallbackExecutorService);
        }
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


