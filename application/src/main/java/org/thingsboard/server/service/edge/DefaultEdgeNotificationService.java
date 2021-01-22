/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;

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

    private static final int DEFAULT_LIMIT = 100;

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
    private TbClusterService clusterService;

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
    public Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) throws IOException {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = edgeService.saveEdge(edge);
        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN, EdgeEventActionType.UPDATED, ruleChainId, null);
        return savedEdge;
    }

    private void saveEdgeEvent(TenantId tenantId,
                               EdgeId edgeId,
                               EdgeEventType type,
                               EdgeEventActionType action,
                               EntityId entityId,
                               JsonNode body) {
        log.debug("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], type [{}], action[{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setType(type);
        edgeEvent.setAction(action);
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        edgeEvent.setBody(body);
        ListenableFuture<EdgeEvent> future = edgeEventService.saveAsync(edgeEvent);
        Futures.addCallback(future, new FutureCallback<EdgeEvent>() {
            @Override
            public void onSuccess(@Nullable EdgeEvent result) {
                clusterService.onEdgeEventUpdate(tenantId, edgeId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Can't save edge event [{}] for edge [{}]", tenantId.getId(), edgeEvent, edgeId.getId(), t);
            }
        }, dbCallbackExecutorService);

    }

    @Override
    public void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        try {
            TenantId tenantId = new TenantId(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
            EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
            switch (type) {
                case EDGE:
                    processEdge(tenantId, edgeNotificationMsg);
                    break;
                case USER:
                case ASSET:
                case DEVICE:
                case DEVICE_PROFILE:
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
                    log.debug("Edge event type [{}] is not designed to be pushed to edge", type);
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
            EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
            EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
            ListenableFuture<Edge> edgeFuture;
            switch (actionType) {
                case ASSIGNED_TO_CUSTOMER:
                    CustomerId customerId = mapper.readValue(edgeNotificationMsg.getBody(), CustomerId.class);
                    edgeFuture = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                    Futures.addCallback(edgeFuture, new FutureCallback<Edge>() {
                        @Override
                        public void onSuccess(@Nullable Edge edge) {
                            if (edge != null && !customerId.isNullUid()) {
                                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, customerId, null);
                                PageLink pageLink = new PageLink(DEFAULT_LIMIT);
                                PageData<User> pageData;
                                do {
                                    pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
                                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                                        log.trace("[{}] [{}] user(s) are going to be added to edge.", edge.getId(), pageData.getData().size());
                                        for (User user : pageData.getData()) {
                                            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, EdgeEventActionType.ADDED, user.getId(), null);
                                        }
                                        if (pageData.hasNext()) {
                                            pageLink = pageLink.nextPageLink();
                                        }
                                    }
                                } while (pageData != null && pageData.hasNext());
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.error("Can't find edge by id [{}]", edgeNotificationMsg, t);
                        }
                    }, dbCallbackExecutorService);
                    break;
                case UNASSIGNED_FROM_CUSTOMER:
                    CustomerId customerIdToDelete = mapper.readValue(edgeNotificationMsg.getBody(), CustomerId.class);
                    edgeFuture = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                    Futures.addCallback(edgeFuture, new FutureCallback<Edge>() {
                        @Override
                        public void onSuccess(@Nullable Edge edge) {
                            if (edge != null && !customerIdToDelete.isNullUid()) {
                                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.DELETED, customerIdToDelete, null);
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
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case DELETED:
                processActionForAllEdges(tenantId, type, actionType, entityId);
                break;
        }
    }

    private void processCustomer(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        UUID uuid = new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB());
        CustomerId customerId = new CustomerId(EntityIdFactory.getByEdgeEventTypeAndUuid(type, uuid).getId());
        switch (actionType) {
            case UPDATED:
                PageLink pageLink = new PageLink(DEFAULT_LIMIT);
                PageData<Edge> pageData;
                do {
                    pageData = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        for (Edge edge : pageData.getData()) {
                            saveEdgeEvent(tenantId, edge.getId(), type, actionType, customerId, null);
                        }
                        if (pageData.hasNext()) {
                            pageLink = pageLink.nextPageLink();
                        }
                    }
                } while (pageData != null && pageData.hasNext());
            case DELETED:
                EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB()));
                saveEdgeEvent(tenantId, edgeId, type, actionType, customerId, null);
                break;
        }
    }

    private void processEntity(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type,
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB()));
        ListenableFuture<List<EdgeId>> edgeIdsFuture;
        switch (actionType) {
            case ADDED: // used only for USER entity
            case UPDATED:
            case CREDENTIALS_UPDATED:
                edgeIdsFuture = edgeService.findRelatedEdgeIdsByEntityId(tenantId, entityId);
                Futures.addCallback(edgeIdsFuture, new FutureCallback<List<EdgeId>>() {
                    @Override
                    public void onSuccess(@Nullable List<EdgeId> edgeIds) {
                        if (edgeIds != null && !edgeIds.isEmpty()) {
                            for (EdgeId edgeId : edgeIds) {
                                saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null);
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
                                    CustomerId customerId = mapper.readValue(edgeNotificationMsg.getBody(), CustomerId.class);
                                    ListenableFuture<Edge> future = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                                    Futures.addCallback(future, new FutureCallback<Edge>() {
                                        @Override
                                        public void onSuccess(@Nullable Edge edge) {
                                            if (edge != null && edge.getCustomerId() != null &&
                                                    !edge.getCustomerId().isNullUid() && edge.getCustomerId().equals(customerId)) {
                                                saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null);
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
                saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null);
                break;
            case ASSIGNED_TO_EDGE:
            case UNASSIGNED_FROM_EDGE:
                saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null);
                if (type.equals(EdgeEventType.RULE_CHAIN)) {
                    updateDependentRuleChains(tenantId, new RuleChainId(entityId.getId()), edgeId);
                }
                break;
        }
    }

    private void updateDependentRuleChains(TenantId tenantId, RuleChainId processingRuleChainId, EdgeId edgeId) {
        TimePageLink pageLink = new TimePageLink(DEFAULT_LIMIT);
        PageData<RuleChain> pageData;
        do {
            pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
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
                                            EdgeEventActionType.UPDATED,
                                            ruleChain.getId(),
                                            null);
                                }
                            }
                        }
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
    }

    private void processAlarm(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        AlarmId alarmId = new AlarmId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        ListenableFuture<Alarm> alarmFuture = alarmService.findAlarmByIdAsync(tenantId, alarmId);
        Futures.addCallback(alarmFuture, new FutureCallback<Alarm>() {
            @Override
            public void onSuccess(@Nullable Alarm alarm) {
                if (alarm != null) {
                    EdgeEventType type = EdgeUtils.getEdgeEventTypeByEntityType(alarm.getOriginator().getEntityType());
                    if (type != null) {
                        ListenableFuture<List<EdgeId>> relatedEdgeIdsByEntityIdFuture = edgeService.findRelatedEdgeIdsByEntityId(tenantId, alarm.getOriginator());
                        Futures.addCallback(relatedEdgeIdsByEntityIdFuture, new FutureCallback<List<EdgeId>>() {
                            @Override
                            public void onSuccess(@Nullable List<EdgeId> relatedEdgeIdsByEntityId) {
                                if (relatedEdgeIdsByEntityId != null) {
                                    for (EdgeId edgeId : relatedEdgeIdsByEntityId) {
                                        saveEdgeEvent(tenantId,
                                                edgeId,
                                                EdgeEventType.ALARM,
                                                EdgeEventActionType.valueOf(edgeNotificationMsg.getAction()),
                                                alarmId,
                                                null);
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.warn("[{}] can't find related edge ids by entity id [{}]", tenantId.getId(), alarm.getOriginator(), t);
                            }
                        }, dbCallbackExecutorService);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] can't find alarm by id [{}]", tenantId.getId(), alarmId.getId(), t);
            }
        }, dbCallbackExecutorService);
    }

    private void processRelation(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) throws JsonProcessingException {
        EntityRelation relation = mapper.readValue(edgeNotificationMsg.getBody(), EntityRelation.class);
        if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
            List<ListenableFuture<List<EdgeId>>> futures = new ArrayList<>();
            futures.add(edgeService.findRelatedEdgeIdsByEntityId(tenantId, relation.getTo()));
            futures.add(edgeService.findRelatedEdgeIdsByEntityId(tenantId, relation.getFrom()));
            ListenableFuture<List<List<EdgeId>>> combinedFuture = Futures.allAsList(futures);
            Futures.addCallback(combinedFuture, new FutureCallback<List<List<EdgeId>>>() {
                @Override
                public void onSuccess(@Nullable List<List<EdgeId>> listOfListsEdgeIds) {
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
                                    EdgeEventActionType.valueOf(edgeNotificationMsg.getAction()),
                                    null,
                                    mapper.valueToTree(relation));
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("[{}] can't find related edge ids by relation to id [{}] and relation from id [{}]" ,
                            tenantId.getId(), relation.getTo().getId(), relation.getFrom().getId(), t);
                }
            }, dbCallbackExecutorService);
        }
    }

    private void processActionForAllEdges(TenantId tenantId, EdgeEventType type, EdgeEventActionType actionType, EntityId entityId) {
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        PageData<Edge> pageData;
        do {
            pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                for (Edge edge : pageData.getData()) {
                    saveEdgeEvent(tenantId, edge.getId(), type, actionType, entityId, null);
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
    }
}


