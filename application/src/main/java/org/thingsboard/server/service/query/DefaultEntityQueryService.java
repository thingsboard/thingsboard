/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.KvUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.AvailableEntityKeys;
import org.thingsboard.server.common.data.query.AvailableEntityKeysV2;
import org.thingsboard.server.common.data.query.AvailableEntityKeysV2.KeyInfo;
import org.thingsboard.server.common.data.query.AvailableEntityKeysV2.KeySample;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.FilterPredicateType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.SimpleKeyFilterPredicate;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.Futures.immediateFuture;

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
        if (query.getKeyFilters() != null) {
            resolveDynamicValuesInPredicates(
                    query.getKeyFilters().stream()
                            .map(KeyFilter::getPredicate)
                            .collect(Collectors.toList()),
                    securityUser
            );
        }
        return entityService.findEntityDataByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    private void resolveDynamicValuesInPredicates(List<KeyFilterPredicate> predicates, SecurityUser user) {
        predicates.forEach(predicate -> {
            if (predicate.getType() == FilterPredicateType.COMPLEX) {
                resolveDynamicValuesInPredicates(
                        ((ComplexFilterPredicate) predicate).getPredicates(),
                        user
                );
            } else {
                setResolvedValue(user, (SimpleKeyFilterPredicate<?>) predicate);
            }
        });
    }

    private void setResolvedValue(SecurityUser user, SimpleKeyFilterPredicate<?> predicate) {
        DynamicValue<?> dynamicValue = predicate.getValue().getDynamicValue();
        if (dynamicValue != null && dynamicValue.getResolvedValue() == null) {
            resolveDynamicValue(dynamicValue, user, predicate.getType());
        }
    }

    private <T> void resolveDynamicValue(DynamicValue<T> dynamicValue, SecurityUser user, FilterPredicateType predicateType) {
        EntityId entityId = switch (dynamicValue.getSourceType()) {
            case CURRENT_TENANT -> user.getTenantId();
            case CURRENT_CUSTOMER -> user.getCustomerId();
            case CURRENT_USER -> user.getId();
            default -> throw new RuntimeException("Not supported operation for source type: {" + dynamicValue.getSourceType() + "}");
        };

        try {
            Optional<AttributeKvEntry> valueOpt = attributesService.find(user.getTenantId(), entityId,
                    AttributeScope.SERVER_SCOPE, dynamicValue.getSourceAttribute()).get();

            if (valueOpt.isPresent()) {
                AttributeKvEntry entry = valueOpt.get();
                Object resolved = null;
                switch (predicateType) {
                    case STRING:
                        resolved = KvUtil.getStringValue(entry);
                        break;
                    case NUMERIC:
                        resolved = KvUtil.getDoubleValue(entry);
                        break;
                    case BOOLEAN:
                        resolved = KvUtil.getBoolValue(entry);
                        break;
                    case COMPLEX:
                        break;
                }

                dynamicValue.setResolvedValue((T) resolved);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
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
            PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(securityUser.getTenantId(), query, entitiesMap.keySet());
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
    public long countAlarmsByQuery(SecurityUser securityUser, AlarmCountQuery query) {
        if (query.getEntityFilter() != null) {
            EntityDataQuery entityDataQuery = this.buildEntityDataQuery(query);
            PageData<EntityData> entities = entityService.findEntityDataByQuery(securityUser.getTenantId(),
                    securityUser.getCustomerId(), entityDataQuery);
            if (entities.getTotalElements() > 0) {
                List<EntityId> entityIds = entities.getData().stream().map(EntityData::getEntityId).toList();
                return alarmService.countAlarmsByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query, entityIds);
            } else {
                return 0;
            }
        }
        return alarmService.countAlarmsByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    private EntityDataQuery buildEntityDataQuery(AlarmCountQuery query) {
        EntityDataPageLink edpl = new EntityDataPageLink(maxEntitiesPerAlarmSubscription, 0, null,
                new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, EntityKeyMapping.CREATED_TIME)));
        return new EntityDataQuery(query.getEntityFilter(), edpl, null, null, query.getKeyFilters());
    }

    private EntityDataQuery buildEntityDataQuery(AlarmDataQuery query) {
        EntityDataSortOrder sortOrder = query.getPageLink().getSortOrder();
        EntityDataSortOrder entitiesSortOrder;
        if (sortOrder == null || sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
            entitiesSortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, EntityKeyMapping.CREATED_TIME));
        } else {
            entitiesSortOrder = sortOrder;
        }
        EntityDataPageLink edpl = new EntityDataPageLink(maxEntitiesPerAlarmSubscription, 0, null, entitiesSortOrder);
        return new EntityDataQuery(query.getEntityFilter(), edpl, query.getEntityFields(), query.getLatestValues(), query.getKeyFilters());
    }

    @Override
    public ListenableFuture<AvailableEntityKeys> getKeysByQuery(SecurityUser securityUser, TenantId tenantId, EntityDataQuery query,
                                                                boolean isTimeseries, boolean isAttributes, AttributeScope scope) {
        if (!isAttributes && !isTimeseries) {
            return immediateFuture(AvailableEntityKeys.none());
        }

        List<EntityId> ids = findEntityDataByQuery(securityUser, query).getData().stream()
                .map(EntityData::getEntityId)
                .toList();
        if (ids.isEmpty()) {
            return immediateFuture(AvailableEntityKeys.none());
        }

        Set<EntityType> types = ids.stream().map(EntityId::getEntityType).collect(Collectors.toSet());
        ListenableFuture<List<String>> timeseriesKeysFuture;
        ListenableFuture<List<String>> attributesKeysFuture;

        if (isTimeseries) {
            timeseriesKeysFuture = timeseriesService.findAllKeysByEntityIdsAsync(tenantId, ids);
        } else {
            timeseriesKeysFuture = immediateFuture(Collections.emptyList());
        }

        if (isAttributes) {
            Map<EntityType, List<EntityId>> typesMap = ids.stream().collect(Collectors.groupingBy(EntityId::getEntityType));
            List<ListenableFuture<List<String>>> futures = new ArrayList<>(typesMap.size());
            typesMap.forEach((type, entityIds) -> futures.add(dbCallbackExecutor.submit(() -> attributesService.findAllKeysByEntityIdsAndScope(tenantId, entityIds, scope))));
            attributesKeysFuture = Futures.transform(Futures.allAsList(futures), lists -> {
                if (CollectionUtils.isEmpty(lists)) {
                    return Collections.emptyList();
                }
                return lists.stream().flatMap(List::stream).distinct().sorted().toList();
            }, dbCallbackExecutor);
        } else {
            attributesKeysFuture = immediateFuture(Collections.emptyList());
        }

        return Futures.whenAllComplete(timeseriesKeysFuture, attributesKeysFuture)
                .call(() -> {
                    try {
                        return new AvailableEntityKeys(types, Futures.getDone(timeseriesKeysFuture), Futures.getDone(attributesKeysFuture));
                    } catch (ExecutionException e) {
                        throw new ThingsboardException(e.getCause(), ThingsboardErrorCode.DATABASE);
                    }
                }, dbCallbackExecutor);
    }

    @Override
    public ListenableFuture<AvailableEntityKeysV2> findAvailableEntityKeysByQuery(SecurityUser securityUser, EntityDataQuery query,
                                                                                  boolean includeTimeseries, boolean includeAttributes,
                                                                                  Set<AttributeScope> scopes, boolean includeSamples) {
        if (!includeTimeseries && !includeAttributes) {
            return Futures.immediateFailedFuture(
                    new IllegalArgumentException("At least one of 'includeTimeseries' or 'includeAttributes' must be true"));
        }

        return Futures.transformAsync(findEntityIdsByQueryAsync(securityUser, query), ids -> {
            if (ids.isEmpty()) {
                return immediateFuture(new AvailableEntityKeysV2(
                        Collections.emptySet(),
                        includeTimeseries ? Collections.emptyList() : null,
                        includeAttributes ? Collections.emptyMap() : null));
            }

            TenantId tenantId = securityUser.getTenantId();
            Set<EntityType> entityTypes = ids.stream().map(EntityId::getEntityType).collect(Collectors.toSet());

            var tsFuture = includeTimeseries ? fetchTimeseriesKeys(tenantId, ids, includeSamples) : null;

            Set<AttributeScope> effectiveScopes = includeAttributes
                    ? resolveAttributeScopes(scopes, entityTypes) : Collections.emptySet();
            var attrFutures = effectiveScopes.stream()
                    .map(scope -> fetchAttributeKeys(tenantId, ids, scope, includeSamples))
                    .toList();

            return assembleResult(entityTypes, tsFuture, attrFutures);
        }, dbCallbackExecutor);
    }

    private ListenableFuture<List<EntityId>> findEntityIdsByQueryAsync(SecurityUser securityUser, EntityDataQuery query) {
        return Futures.transform(entityService.findEntityDataByQueryAsync(securityUser.getTenantId(), securityUser.getCustomerId(), query),
                page -> page.getData().stream()
                        .map(EntityData::getEntityId)
                        .toList(),
                dbCallbackExecutor);
    }

    private static Set<AttributeScope> resolveAttributeScopes(Set<AttributeScope> requestedScopes, Set<EntityType> entityTypes) {
        boolean hasDevices = entityTypes.contains(EntityType.DEVICE);
        Set<AttributeScope> scopes;
        if (CollectionUtils.isNotEmpty(requestedScopes)) {
            scopes = requestedScopes;
        } else { // auto-determine scopes
            scopes = hasDevices
                    ? Set.of(AttributeScope.SERVER_SCOPE, AttributeScope.CLIENT_SCOPE, AttributeScope.SHARED_SCOPE)
                    : Collections.singleton(AttributeScope.SERVER_SCOPE);
        }
        // Non-device entities only support SERVER_SCOPE
        if (!hasDevices) {
            return scopes.contains(AttributeScope.SERVER_SCOPE)
                    ? Collections.singleton(AttributeScope.SERVER_SCOPE)
                    : Collections.emptySet();
        }
        return scopes;
    }

    private ListenableFuture<List<KeyInfo>> fetchTimeseriesKeys(TenantId tenantId, List<EntityId> entityIds, boolean includeSamples) {
        if (includeSamples) {
            return Futures.transform(
                    timeseriesService.findLatestByEntityIdsAsync(tenantId, entityIds),
                    entries -> toKeyInfos(entries, true),
                    dbCallbackExecutor);
        }
        return Futures.transform(
                timeseriesService.findAllKeysByEntityIdsAsync(tenantId, entityIds),
                keys -> keys.stream().sorted().map(k -> new KeyInfo(k, null)).toList(),
                dbCallbackExecutor);
    }

    private ListenableFuture<Map.Entry<AttributeScope, List<KeyInfo>>> fetchAttributeKeys(
            TenantId tenantId, List<EntityId> entityIds, AttributeScope scope, boolean includeSamples) {
        if (includeSamples) {
            return Futures.transform(
                    attributesService.findLatestByEntityIdsAndScopeAsync(tenantId, entityIds, scope),
                    entries -> Map.entry(scope, toKeyInfos(entries, true)),
                    dbCallbackExecutor);
        }
        return Futures.transform(
                attributesService.findAllKeysByEntityIdsAndScopeAsync(tenantId, entityIds, scope),
                keys -> Map.entry(scope, keys.stream().sorted().map(k -> new KeyInfo(k, null)).toList()),
                dbCallbackExecutor);
    }

    private ListenableFuture<AvailableEntityKeysV2> assembleResult(
            Set<EntityType> entityTypes,
            ListenableFuture<List<KeyInfo>> tsFuture,
            List<ListenableFuture<Map.Entry<AttributeScope, List<KeyInfo>>>> attrFutures) {
        var allAttrFuture = attrFutures.isEmpty()
                ? immediateFuture(List.<Map.Entry<AttributeScope, List<KeyInfo>>>of())
                : Futures.allAsList(attrFutures);

        List<ListenableFuture<?>> allFutures = new ArrayList<>();
        if (tsFuture != null) {
            allFutures.add(tsFuture);
        }
        allFutures.add(allAttrFuture);

        var finalTsFuture = tsFuture;
        return Futures.whenAllComplete(allFutures)
                .call(() -> {
                    List<KeyInfo> tsKeys = finalTsFuture != null ? Futures.getDone(finalTsFuture) : null;
                    Map<AttributeScope, List<KeyInfo>> attrMap = attrFutures.isEmpty() ? null : new TreeMap<>();
                    if (attrMap != null) {
                        for (var entry : Futures.getDone(allAttrFuture)) {
                            attrMap.put(entry.getKey(), entry.getValue());
                        }
                    }
                    return new AvailableEntityKeysV2(entityTypes, tsKeys, attrMap);
                }, dbCallbackExecutor);
    }

    private static List<KeyInfo> toKeyInfos(List<? extends KvEntry> entries, boolean includeSamples) {
        return entries.stream()
                .map(e -> new KeyInfo(e.getKey(), includeSamples ? toKeySample(e) : null))
                .sorted(Comparator.comparing(KeyInfo::key))
                .toList();
    }

    private static KeySample toKeySample(KvEntry entry) {
        long ts = entry instanceof TsKvEntry tsKv ? tsKv.getTs()
                : entry instanceof AttributeKvEntry attr ? attr.getLastUpdateTs()
                : 0;
        JsonNode value = entry.getDataType() == DataType.JSON
                ? JacksonUtil.toJsonNode(entry.getJsonValue().get())
                : JacksonUtil.valueToTree(entry.getValue());
        return new KeySample(ts, value);
    }

}
