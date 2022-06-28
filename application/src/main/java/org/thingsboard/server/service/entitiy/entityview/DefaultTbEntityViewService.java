/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.entityview;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@AllArgsConstructor
@Slf4j
public class DefaultTbEntityViewService extends AbstractTbEntityService implements TbEntityViewService {

    private final EntityViewService entityViewService;
    private final AttributesService attributesService;
    private final TelemetrySubscriptionService tsSubService;
    private final TimeseriesService tsService;

    final Map<TenantId, Map<EntityId, List<EntityView>>> localCache = new ConcurrentHashMap<>();

    @Override
    public EntityView save(EntityView entityView, EntityView existingEntityView, User user) throws ThingsboardException {
        ActionType actionType = entityView.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = entityView.getTenantId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.saveEntityView(entityView));
            this.updateEntityViewAttributes(tenantId, savedEntityView, existingEntityView, user);
            notificationEntityService.notifyCreateOrUpdateEntity(savedEntityView.getTenantId(), savedEntityView.getId(), savedEntityView,
                    null, actionType, user);
            localCache.computeIfAbsent(savedEntityView.getTenantId(), (k) -> new ConcurrentReferenceHashMap<>()).clear();
            tbClusterService.broadcastEntityStateChangeEvent(savedEntityView.getTenantId(), savedEntityView.getId(),
                    entityView.getId() == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(user.getTenantId(), emptyId(EntityType.ENTITY_VIEW), entityView, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void updateEntityViewAttributes(TenantId tenantId, EntityView savedEntityView, EntityView oldEntityView, User user) throws ThingsboardException {
        List<ListenableFuture<?>> futures = new ArrayList<>();

        if (oldEntityView != null) {
            if (oldEntityView.getKeys() != null && oldEntityView.getKeys().getAttributes() != null) {
                futures.add(deleteAttributesFromEntityView(oldEntityView, DataConstants.CLIENT_SCOPE, oldEntityView.getKeys().getAttributes().getCs(), user));
                futures.add(deleteAttributesFromEntityView(oldEntityView, DataConstants.SERVER_SCOPE, oldEntityView.getKeys().getAttributes().getSs(), user));
                futures.add(deleteAttributesFromEntityView(oldEntityView, DataConstants.SHARED_SCOPE, oldEntityView.getKeys().getAttributes().getSh(), user));
            }
            List<String> tsKeys = oldEntityView.getKeys() != null && oldEntityView.getKeys().getTimeseries() != null ?
                    oldEntityView.getKeys().getTimeseries() : Collections.emptyList();
            futures.add(deleteLatestFromEntityView(oldEntityView, tsKeys, user));
        }
        if (savedEntityView.getKeys() != null) {
            if (savedEntityView.getKeys().getAttributes() != null) {
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.CLIENT_SCOPE, savedEntityView.getKeys().getAttributes().getCs(), user));
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SERVER_SCOPE, savedEntityView.getKeys().getAttributes().getSs(), user));
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SHARED_SCOPE, savedEntityView.getKeys().getAttributes().getSh(), user));
            }
            futures.add(copyLatestFromEntityToEntityView(tenantId, savedEntityView));
        }
        for (ListenableFuture<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failed to copy attributes to entity view", e);
            }
        }
    }

    @Override
    public void delete(EntityView entityView, User user) throws ThingsboardException {
        TenantId tenantId = entityView.getTenantId();
        EntityViewId entityViewId = entityView.getId();
        try {
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(tenantId, entityViewId);
            entityViewService.deleteEntityView(tenantId, entityViewId);
            notificationEntityService.notifyDeleteEntity(tenantId, entityViewId, entityView, entityView.getCustomerId(), ActionType.DELETED,
                    relatedEdgeIds, user, entityViewId.toString());

            localCache.computeIfAbsent(tenantId, (k) -> new ConcurrentReferenceHashMap<>()).clear();
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityViewId, ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    ActionType.DELETED, user, e, entityViewId.toString());
            throw e;
        }
    }

    @Override
    public EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException {
        CustomerId customerId = customer.getId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToCustomer(tenantId, entityViewId, customerId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, entityViewId, customerId, savedEntityView,
                    ActionType.ASSIGNED_TO_CUSTOMER, user, true, entityViewId.toString(), customerId.toString(), customer.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    ActionType.ASSIGNED_TO_CUSTOMER, user, e, entityViewId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    public EntityView assignEntityViewToPublicCustomer(TenantId tenantId, CustomerId customerId, Customer publicCustomer,
                                                       EntityViewId entityViewId, User user) throws ThingsboardException {
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToCustomer(tenantId,
                    entityViewId, publicCustomer.getId()));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, entityViewId, customerId, savedEntityView,
                    ActionType.ASSIGNED_TO_CUSTOMER, user, false, savedEntityView.getEntityId().toString(),
                    publicCustomer.getId().toString(), publicCustomer.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    ActionType.ASSIGNED_TO_CUSTOMER, user, e, entityViewId.toString());
            throw e;
        }
    }

    @Override
    public EntityView assignEntityViewToEdge(TenantId tenantId, CustomerId customerId, EntityViewId entityViewId, Edge edge, User user) throws ThingsboardException {
        EdgeId edgeId = edge.getId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToEdge(tenantId, entityViewId, edgeId));
            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, entityViewId, customerId,
                    edgeId, savedEntityView, ActionType.ASSIGNED_TO_EDGE, user, savedEntityView.getEntityId().toString(),
                    edgeId.toString(), edge.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    ActionType.ASSIGNED_TO_EDGE, user, e, entityViewId.toString(), edgeId.toString());
            throw e;
        }
    }

    @Override
    public EntityView unassignEntityViewFromEdge(TenantId tenantId, CustomerId customerId, EntityView entityView,
                                                 Edge edge, User user) throws ThingsboardException {
        EntityViewId entityViewId = entityView.getId();
        EdgeId edgeId = edge.getId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.unassignEntityViewFromEdge(tenantId, entityViewId, edgeId));
            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, entityViewId, customerId,
                    edgeId, entityView, ActionType.UNASSIGNED_FROM_EDGE, user, entityViewId.toString(),
                    edgeId.toString(), edge.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    ActionType.UNASSIGNED_FROM_EDGE, user, e, entityViewId.toString(), edgeId.toString());
            throw e;
        }
    }

    @Override
    public EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException {
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.unassignEntityViewFromCustomer(tenantId, entityViewId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, entityViewId, customer.getId(), savedEntityView,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, user, true, customer.getId().toString(), customer.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, user, e, entityViewId.toString());
            throw e;
        }
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId) {
        Map<EntityId, List<EntityView>> localCacheByTenant = localCache.computeIfAbsent(tenantId, (k) -> new ConcurrentReferenceHashMap<>());
        List<EntityView> fromLocalCache = localCacheByTenant.get(entityId);
        if (fromLocalCache != null) {
            return Futures.immediateFuture(fromLocalCache);
        }

        ListenableFuture<List<EntityView>> future = entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId);

        return Futures.transform(future, (entityViewList) -> {
            localCacheByTenant.put(entityId, entityViewList);
            return entityViewList;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void onComponentLifecycleMsg(ComponentLifecycleMsg componentLifecycleMsg) {
        Map<EntityId, List<EntityView>> localCacheByTenant = localCache.computeIfAbsent(componentLifecycleMsg.getTenantId(), (k) -> new ConcurrentReferenceHashMap<>());
        EntityViewId entityViewId = new EntityViewId(componentLifecycleMsg.getEntityId().getId());
        deleteOldCacheValue(localCacheByTenant, entityViewId);
        if (componentLifecycleMsg.getEvent() != ComponentLifecycleEvent.DELETED) {
            EntityView entityView = entityViewService.findEntityViewById(componentLifecycleMsg.getTenantId(), entityViewId);
            if (entityView != null) {
                localCacheByTenant.remove(entityView.getEntityId());
            }
        }
    }

    private void deleteOldCacheValue(Map<EntityId, List<EntityView>> localCacheByTenant, EntityViewId entityViewId) {
        for (var entry : localCacheByTenant.entrySet()) {
            EntityView toDelete = null;
            for (EntityView view : entry.getValue()) {
                if (entityViewId.equals(view.getId())) {
                    toDelete = view;
                    break;
                }
            }
            if (toDelete != null) {
                entry.getValue().remove(toDelete);
                break;
            }
        }
    }

    private ListenableFuture<List<Void>> copyAttributesFromEntityToEntityView(EntityView entityView, String scope, Collection<String> keys, User user) throws ThingsboardException {
        EntityViewId entityId = entityView.getId();
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<AttributeKvEntry>> getAttrFuture = attributesService.find(entityView.getTenantId(), entityView.getEntityId(), scope, keys);
            return Futures.transform(getAttrFuture, attributeKvEntries -> {
                List<AttributeKvEntry> attributes;
                if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
                    attributes =
                            attributeKvEntries.stream()
                                    .filter(attributeKvEntry -> {
                                        long startTime = entityView.getStartTimeMs();
                                        long endTime = entityView.getEndTimeMs();
                                        long lastUpdateTs = attributeKvEntry.getLastUpdateTs();
                                        return startTime == 0 && endTime == 0 ||
                                                (endTime == 0 && startTime < lastUpdateTs) ||
                                                (startTime == 0 && endTime > lastUpdateTs) ||
                                                (startTime < lastUpdateTs && endTime > lastUpdateTs);
                                    }).collect(Collectors.toList());
                    tsSubService.saveAndNotify(entityView.getTenantId(), entityId, scope, attributes, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            try {
                                logAttributesUpdated(entityView.getTenantId(), user, entityId, scope, attributes, null);
                            } catch (ThingsboardException e) {
                                log.error("Failed to log attribute updates", e);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            try {
                                logAttributesUpdated(entityView.getTenantId(), user, entityId, scope, attributes, t);
                            } catch (ThingsboardException e) {
                                log.error("Failed to log attribute updates", e);
                            }
                        }
                    });
                }
                return null;
            }, MoreExecutors.directExecutor());
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<List<Void>> copyLatestFromEntityToEntityView(TenantId tenantId, EntityView entityView) {
        EntityViewId entityId = entityView.getId();
        List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                entityView.getKeys().getTimeseries() : Collections.emptyList();
        long startTs = entityView.getStartTimeMs();
        long endTs = entityView.getEndTimeMs() == 0 ? Long.MAX_VALUE : entityView.getEndTimeMs();
        ListenableFuture<List<String>> keysFuture;
        if (keys.isEmpty()) {
            keysFuture = Futures.transform(tsService.findAllLatest(tenantId,
                    entityView.getEntityId()), latest -> latest.stream().map(TsKvEntry::getKey).collect(Collectors.toList()), MoreExecutors.directExecutor());
        } else {
            keysFuture = Futures.immediateFuture(keys);
        }
        ListenableFuture<List<TsKvEntry>> latestFuture = Futures.transformAsync(keysFuture, fetchKeys -> {
            List<ReadTsKvQuery> queries = fetchKeys.stream().filter(key -> !isBlank(key)).map(key -> new BaseReadTsKvQuery(key, startTs, endTs, 1, "DESC")).collect(Collectors.toList());
            if (!queries.isEmpty()) {
                return tsService.findAll(tenantId, entityView.getEntityId(), queries);
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
        return Futures.transform(latestFuture, latestValues -> {
            if (latestValues != null && !latestValues.isEmpty()) {
                tsSubService.saveLatestAndNotify(entityView.getTenantId(), entityId, latestValues, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                    }

                    @Override
                    public void onFailure(Throwable t) {
                    }
                });
            }
            return null;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> deleteAttributesFromEntityView(EntityView entityView, String scope, List<String> keys, User user) {
        EntityViewId entityId = entityView.getId();
        SettableFuture<Void> resultFuture = SettableFuture.create();
        if (keys != null && !keys.isEmpty()) {
            tsSubService.deleteAndNotify(entityView.getTenantId(), entityId, scope, keys, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    try {
                        logAttributesDeleted(entityView.getTenantId(), user, entityId, scope, keys, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log attribute delete", e);
                    }
                    resultFuture.set(tmp);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logAttributesDeleted(entityView.getTenantId(), user, entityId, scope, keys, t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log attribute delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        } else {
            resultFuture.set(null);
        }
        return resultFuture;
    }

    private ListenableFuture<Void> deleteLatestFromEntityView(EntityView entityView, List<String> keys, User user) {
        EntityViewId entityId = entityView.getId();
        SettableFuture<Void> resultFuture = SettableFuture.create();
        if (keys != null && !keys.isEmpty()) {
            tsSubService.deleteLatest(entityView.getTenantId(), entityId, keys, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, keys, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.set(tmp);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, keys, t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        } else {
            tsSubService.deleteAllLatest(entityView.getTenantId(), entityId, new FutureCallback<Collection<String>>() {
                @Override
                public void onSuccess(@Nullable Collection<String> keys) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, new ArrayList<>(keys), null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, Collections.emptyList(), t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        }
        return resultFuture;
    }

    private void logAttributesUpdated(TenantId tenantId, User user, EntityId entityId, String scope, List<AttributeKvEntry> attributes, Throwable e) throws ThingsboardException {
        notificationEntityService.logEntityAction(tenantId, entityId, ActionType.ATTRIBUTES_UPDATED, user, toException(e), scope, attributes);
    }

    private void logAttributesDeleted(TenantId tenantId, User user, EntityId entityId, String scope, List<String> keys, Throwable e) throws ThingsboardException {
        notificationEntityService.logEntityAction(tenantId, entityId, ActionType.ATTRIBUTES_DELETED, user, toException(e), scope, keys);
    }

    private void logTimeseriesDeleted(TenantId tenantId, User user, EntityId entityId, List<String> keys, Throwable e) throws ThingsboardException {
        notificationEntityService.logEntityAction(tenantId, entityId, ActionType.TIMESERIES_DELETED, user, toException(e), keys);
    }

    public static Exception toException(Throwable error) {
        return error != null ? (Exception.class.isInstance(error) ? (Exception) error : new Exception(error)) : null;
    }
}
