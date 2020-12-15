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
package org.thingsboard.server.service.query;

import com.datastax.oss.driver.internal.core.util.CollectionsUtils;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@TbCoreComponent
public class DefaultEntityQueryService implements EntityQueryService {

    @Autowired
    private EntityService entityService;

    @Autowired
    private AlarmService alarmService;

    @Value("${server.ws.max_entities_per_alarm_subscription:1000}")
    private int maxEntitiesPerAlarmSubscription;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    private TimeseriesService timeseriesService;

    @Autowired
    private AttributesService attributesService;

    @Override
    public long countEntitiesByQuery(SecurityUser securityUser, EntityCountQuery query) {
        return entityService.countEntitiesByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(SecurityUser securityUser, EntityDataQuery query) {
        return entityService.findEntityDataByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQuery(SecurityUser securityUser, AlarmDataQuery query) {
        EntityDataQuery entityDataQuery = this.buildEntityDataQuery(query);
        PageData<EntityData> entities = entityService.findEntityDataByQuery(securityUser.getTenantId(),
                securityUser.getCustomerId(), entityDataQuery);
        if (entities.getTotalElements() > 0) {
            LinkedHashMap<EntityId, EntityData> entitiesMap = new LinkedHashMap<>();
            for (EntityData entityData : entities.getData()) {
                entitiesMap.put(entityData.getEntityId(), entityData);
            }
            PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(securityUser.getTenantId(),
                    securityUser.getCustomerId(), query, entitiesMap.keySet());
            for (AlarmData alarmData : alarms.getData()) {
                EntityId entityId = alarmData.getEntityId();
                if (entityId != null) {
                    EntityData entityData = entitiesMap.get(entityId);
                    if (entityData != null) {
                        alarmData.getLatest().putAll(entityData.getLatest());
                    }
                }
            }
            return alarms;
        } else {
            return new PageData<>();
        }
    }

    @Override
    public void getKeysByQueryCallback(SecurityUser securityUser, TenantId tenantId, EntityDataQuery query,
                                       boolean isTimeseries, boolean isAttributes, DeferredResult<ResponseEntity> response) {
        if (!isAttributes && !isTimeseries) {
            getEmptyResponseCallback(response);
            return;
        }

        List<EntityId> ids = this.findEntityDataByQuery(securityUser, query).getData().stream()
                .map(EntityData::getEntityId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            getEmptyResponseCallback(response);
            return;
        }

        Set<EntityType> types = ids.stream().map(EntityId::getEntityType).collect(Collectors.toSet());
        ListenableFuture<List<String>> timeseriesKeysFuture;
        ListenableFuture<List<String>> attributesKeysFuture;

        if (isTimeseries) {
            timeseriesKeysFuture = dbCallbackExecutor.submit(() -> timeseriesService.findAllKeysByEntityIds(tenantId, ids));
        } else {
            timeseriesKeysFuture = null;
        }

        if (isAttributes) {
            Map<EntityType, List<EntityId>> typesMap = ids.stream().collect(Collectors.groupingBy(EntityId::getEntityType));
            List<ListenableFuture<List<String>>> futures = new ArrayList<>(typesMap.size());
            typesMap.forEach((type, entityIds) -> futures.add(dbCallbackExecutor.submit(() -> attributesService.findAllKeysByEntityIds(tenantId, type, entityIds))));
            attributesKeysFuture = Futures.transform(Futures.allAsList(futures), lists -> {
                if (CollectionUtils.isEmpty(lists)) {
                    return null;
                }

                return lists.stream().flatMap(List::stream).distinct().sorted().collect(Collectors.toList());
            }, dbCallbackExecutor);
        } else {
            attributesKeysFuture = null;
        }

        if (timeseriesKeysFuture != null && attributesKeysFuture != null) {
            Futures.whenAllComplete(timeseriesKeysFuture, attributesKeysFuture).call(() -> {
                try {
                    getResponseCallback(response, types, timeseriesKeysFuture.get(), attributesKeysFuture.get());
                } catch (Exception e) {
                    log.error("Failed to fetch timeseries and attributes keys!", e);
                    AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
                }

                return null;
            }, dbCallbackExecutor);
        } else if (timeseriesKeysFuture != null) {
            Futures.addCallback(timeseriesKeysFuture, new FutureCallback<List<String>>() {
                @Override
                public void onSuccess(@Nullable List<String> keys) {
                    getResponseCallback(response, types, keys, null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Failed to fetch timeseries keys!", t);
                    AccessValidator.handleError(t, response, HttpStatus.INTERNAL_SERVER_ERROR);
                }

            }, dbCallbackExecutor);
        } else {
            Futures.addCallback(attributesKeysFuture, new FutureCallback<List<String>>() {
                @Override
                public void onSuccess(@Nullable List<String> keys) {
                    getResponseCallback(response, types, null, keys);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Failed to fetch attributes keys!", t);
                    AccessValidator.handleError(t, response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }, dbCallbackExecutor);
        }
    }

    private void getResponseCallback(DeferredResult<ResponseEntity> response, Set<EntityType> types, List<String> timeseriesKeys, List<String> attributesKeys) {
        ObjectNode json = JacksonUtil.newObjectNode();
        addItemsToArrayNode(json.putArray("types"), types);
        addItemsToArrayNode(json.putArray("timeseriesKeys"), timeseriesKeys);
        addItemsToArrayNode(json.putArray("attributesKeys"), attributesKeys);

        response.setResult(new ResponseEntity(json, HttpStatus.OK));
    }

    private void getEmptyResponseCallback(DeferredResult<ResponseEntity> response) {
        getResponseCallback(response, null, null, null);
    }

    private void addItemsToArrayNode(ArrayNode arrayNode, Collection<?> collection) {
        if (!CollectionUtils.isEmpty(collection)) {
            collection.forEach(item -> arrayNode.add(item.toString()));
        }
    }

    private EntityDataQuery buildEntityDataQuery(AlarmDataQuery query) {
        EntityDataSortOrder sortOrder = query.getPageLink().getSortOrder();
        EntityDataSortOrder entitiesSortOrder;
        if (sortOrder == null || sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
            entitiesSortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, ModelConstants.CREATED_TIME_PROPERTY));
        } else {
            entitiesSortOrder = sortOrder;
        }
        EntityDataPageLink edpl = new EntityDataPageLink(maxEntitiesPerAlarmSubscription, 0, null, entitiesSortOrder);
        return new EntityDataQuery(query.getEntityFilter(), edpl, query.getEntityFields(), query.getLatestValues(), query.getKeyFilters());
    }
}
