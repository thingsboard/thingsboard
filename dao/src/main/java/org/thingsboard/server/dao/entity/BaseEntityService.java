/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.entity;

import com.google.common.util.concurrent.FluentFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasEmail;
import org.thingsboard.server.common.data.HasLabel;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTitle;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edqs.query.EdqsRequest;
import org.thingsboard.server.common.data.edqs.query.EdqsResponse;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NameLabelAndCustomerDetails;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.msg.edqs.EdqsApiService;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.common.stats.EdqsStatsService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.common.data.id.EntityId.NULL_UUID;
import static org.thingsboard.server.common.data.query.EntityFilterType.ENTITY_NAME;
import static org.thingsboard.server.common.data.query.EntityFilterType.ENTITY_TYPE;
import static org.thingsboard.server.dao.service.Validator.validateEntityDataPageLink;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseEntityService extends AbstractEntityService implements EntityService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final CustomerId NULL_CUSTOMER_ID = new CustomerId(NULL_UUID);

    private static final int MAX_ENTITY_IDS_SIZE = 1024;
    private static final Set<EntityFilterType> EXCLUDED_TYPES_FROM_OPTIMIZATION = Set.of(
            EntityFilterType.ENTITY_LIST, EntityFilterType.SINGLE_ENTITY, EntityFilterType.RELATIONS_QUERY);

    @Autowired
    private EntityQueryDao entityQueryDao;

    @Autowired
    @Lazy
    private EntityServiceRegistry entityServiceRegistry;

    @Autowired
    private EdqsService edqsService;

    @Autowired
    @Lazy
    private EdqsApiService edqsApiService;

    @Autowired
    private EdqsStatsService edqsStatsService;

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        log.trace("Executing countEntitiesByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateEntityCountQuery(query);

        long startNs = System.nanoTime();
        Long result;
        if (edqsService.isApiEnabled() && validForEdqs(query) && !tenantId.isSysTenantId()) {
            EdqsRequest request = EdqsRequest.builder()
                    .entityCountQuery(query)
                    .build();
            EdqsResponse response = processEdqsRequest(tenantId, customerId, request);
            result = response.getEntityCountQueryResult();
        } else {
            result = entityQueryDao.countEntitiesByQuery(tenantId, customerId, query);
        }
        edqsStatsService.reportEntityCountQuery(tenantId, query, System.nanoTime() - startNs);
        return result;
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        log.trace("Executing findEntityDataByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateEntityDataQuery(query);

        long startNs = System.nanoTime();
        PageData<EntityData> result;
        if (edqsService.isApiEnabled() && validForEdqs(query)) {
            EdqsRequest request = EdqsRequest.builder()
                    .entityDataQuery(query)
                    .build();
            EdqsResponse response = processEdqsRequest(tenantId, customerId, request);
            result = response.getEntityDataQueryResult();
        } else {
            if (!isValidForOptimization(query)) {
                result = entityQueryDao.findEntityDataByQuery(tenantId, customerId, query);
            } else {
                // 1 step - find entity data by filter and sort columns
                PageData<EntityData> entityDataByQuery = findEntityIdsByFilterAndSorterColumns(tenantId, customerId, query);
                if (entityDataByQuery == null || entityDataByQuery.getData().isEmpty()) {
                    result = entityDataByQuery;
                } else {
                    // 2 step - find entity data by entity ids from the 1st step
                    List<EntityData> entities = fetchEntityDataByIdsFromInitialQuery(tenantId, customerId, query, entityDataByQuery.getData());
                    result = new PageData<>(entities, entityDataByQuery.getTotalPages(), entityDataByQuery.getTotalElements(), entityDataByQuery.hasNext());
                }
            }
        }
        edqsStatsService.reportEntityDataQuery(tenantId, query, System.nanoTime() - startNs);
        return result;
    }

    private boolean validForEdqs(EntityCountQuery query) { // for compatibility with PE
        return true;
    }

    private EdqsResponse processEdqsRequest(TenantId tenantId, CustomerId customerId, EdqsRequest request) {
        EdqsResponse response;
        try {
            log.debug("[{}] Sending request to EDQS: {}", tenantId, request);
            response = edqsApiService.processRequest(tenantId, customerId, request).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        log.debug("[{}] Received response from EDQS: {}", tenantId, response);
        if (response.getError() != null) {
            throw new RuntimeException(response.getError());
        }
        return response;
    }

    @Override
    public Optional<String> fetchEntityName(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityName [{}]", entityId);
        return fetchAndConvert(tenantId, entityId, this::getName);
    }

    @Override
    public Optional<String> fetchEntityLabel(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityLabel [{}]", entityId);
        return fetchAndConvert(tenantId, entityId, this::getLabel);
    }

    @Override
    public Optional<CustomerId> fetchEntityCustomerId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityCustomerId [{}]", entityId);
        return fetchAndConvert(tenantId, entityId, this::getCustomerId);
    }

    @Override
    public FluentFuture<Optional<CustomerId>> fetchEntityCustomerIdAsync(TenantId tenantId, EntityId entityId) {
        return fetchAndConvertAsync(tenantId, entityId, this::getCustomerId);
    }

    @Override
    public Optional<NameLabelAndCustomerDetails> fetchNameLabelAndCustomerDetails(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchNameLabelAndCustomerDetails [{}]", entityId);
        return fetchAndConvert(tenantId, entityId, this::getNameLabelAndCustomerDetails);
    }

    @Override
    public Optional<HasId<?>> fetchEntity(TenantId tenantId, EntityId entityId) {
        return fetchAndConvert(tenantId, entityId, Function.identity());
    }

    @Override
    public Map<EntityId, EntityInfo> fetchEntityInfos(TenantId tenantId, CustomerId customerId, Set<EntityId> entityIds) {
        Map<EntityId, EntityInfo> infos = new HashMap<>();
        entityIds.stream()
                .collect(Collectors.groupingBy(EntityId::getEntityType))
                .forEach((entityType, ids) -> {
                    EntityListFilter filter = new EntityListFilter();
                    filter.setEntityType(entityType);
                    filter.setEntityList(ids.stream().map(Object::toString).toList());
                    EntityDataQuery query = new EntityDataQuery(filter, new EntityDataPageLink(ids.size(), 0, null, null),
                            List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, ModelConstants.NAME_PROPERTY)), Collections.emptyList(), Collections.emptyList());

                    entityQueryDao.findEntityDataByQuery(tenantId, customerId, query).getData().forEach(entityData -> {
                        EntityId entityId = entityData.getEntityId();
                        Optional.ofNullable(entityData.getLatest().get(EntityKeyType.ENTITY_FIELD))
                                .map(fields -> fields.get(ModelConstants.NAME_PROPERTY))
                                .map(TsValue::getValue).ifPresent(name -> {
                                    infos.put(entityId, new EntityInfo(entityId, name));
                                });
                    });
                });
        return infos;
    }

    private <T> Optional<T> fetchAndConvert(TenantId tenantId, EntityId entityId, Function<HasId<?>, T> converter) {
        EntityDaoService entityDaoService = entityServiceRegistry.getServiceByEntityType(entityId.getEntityType());
        Optional<HasId<?>> entityOpt = entityDaoService.findEntity(tenantId, entityId);
        return entityOpt.map(converter);
    }

    private <T> FluentFuture<Optional<T>> fetchAndConvertAsync(TenantId tenantId, EntityId entityId, Function<HasId<?>, T> converter) {
        EntityDaoService entityDaoService = entityServiceRegistry.getServiceByEntityType(entityId.getEntityType());
        return entityDaoService.findEntityAsync(tenantId, entityId)
                .transform(entityOpt -> entityOpt.map(converter), directExecutor());
    }

    private String getName(HasId<?> entity) {
        return entity instanceof HasName ? ((HasName) entity).getName() : null;
    }

    private String getLabel(HasId<?> entity) {
        if (entity instanceof HasTitle && StringUtils.isNotEmpty(((HasTitle) entity).getTitle())) {
            return ((HasTitle) entity).getTitle();
        }
        if (entity instanceof HasLabel && StringUtils.isNotEmpty(((HasLabel) entity).getLabel())) {
            return ((HasLabel) entity).getLabel();
        }
        if (entity instanceof HasEmail && StringUtils.isNotEmpty(((HasEmail) entity).getEmail())) {
            return ((HasEmail) entity).getEmail();
        }
        if (entity instanceof HasName && StringUtils.isNotEmpty(((HasName) entity).getName())) {
            return ((HasName) entity).getName();
        }
        return null;
    }

    private CustomerId getCustomerId(HasId<?> entity) {
        if (entity instanceof HasCustomerId hasCustomerId) {
            CustomerId customerId = hasCustomerId.getCustomerId();
            if (customerId == null) {
                customerId = NULL_CUSTOMER_ID;
            }
            return customerId;
        }
        return NULL_CUSTOMER_ID;
    }

    private NameLabelAndCustomerDetails getNameLabelAndCustomerDetails(HasId<?> entity) {
        return new NameLabelAndCustomerDetails(getName(entity), getLabel(entity), getCustomerId(entity));
    }

    private static void validateEntityCountQuery(EntityCountQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("Query must be specified.");
        } else if (query.getEntityFilter() == null) {
            throw new IncorrectParameterException("Query entity filter must be specified.");
        } else if (query.getEntityFilter().getType() == null) {
            throw new IncorrectParameterException("Query entity filter type must be specified.");
        } else if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            validateRelationQuery((RelationsQueryFilter) query.getEntityFilter());
        } else if (query.getEntityFilter().getType().equals(ENTITY_TYPE)) {
            validateEntityTypeQuery((EntityTypeFilter) query.getEntityFilter());
        } else if (query.getEntityFilter().getType().equals(ENTITY_NAME)) {
            validateEntityNameQuery((EntityNameFilter) query.getEntityFilter());
        }
    }

    private static void validateEntityDataQuery(EntityDataQuery query) {
        validateEntityCountQuery(query);
        validateEntityDataPageLink(query.getPageLink());
    }

    private static void validateEntityTypeQuery(EntityTypeFilter filter) {
        if (filter.getEntityType() == null) {
            throw new IncorrectParameterException("Entity type is required");
        }
    }

    private static void validateEntityNameQuery(EntityNameFilter filter) {
        if (filter.getEntityType() == null) {
            throw new IncorrectParameterException("Entity type is required");
        }
    }

    private static void validateRelationQuery(RelationsQueryFilter queryFilter) {
        if (queryFilter.isMultiRoot() && queryFilter.getMultiRootEntitiesType() == null) {
            throw new IncorrectParameterException("Multi-root relation query filter should contain 'multiRootEntitiesType'");
        }
        if (queryFilter.isMultiRoot() && CollectionUtils.isEmpty(queryFilter.getMultiRootEntityIds())) {
            throw new IncorrectParameterException("Multi-root relation query filter should contain 'multiRootEntityIds' array that contains string representation of UUIDs");
        }
        if (!queryFilter.isMultiRoot() && queryFilter.getRootEntity() == null) {
            throw new IncorrectParameterException("Relation query filter root entity should not be blank");
        }
    }

    private boolean isValidForOptimization(EntityDataQuery query) {
        if (StringUtils.isNotEmpty(query.getPageLink().getTextSearch())) {
            return false;
        }

        if (EXCLUDED_TYPES_FROM_OPTIMIZATION.contains(query.getEntityFilter().getType())) {
            return false;
        }

        if ((query.getEntityFields() == null || query.getEntityFields().isEmpty()) &&
                (query.getLatestValues() == null || query.getLatestValues().isEmpty())) {
            return false;
        }

        Set<EntityKey> filteringKeys = new HashSet<>(Optional.ofNullable(query.getKeyFilters()).orElse(Collections.emptyList()).stream().map(KeyFilter::getKey).toList());
        Set<EntityKey> entityFields = new HashSet<>(Optional.ofNullable(query.getEntityFields()).orElse(Collections.emptyList()));
        Set<EntityKey> latestValues = new HashSet<>(Optional.ofNullable(query.getLatestValues()).orElse(Collections.emptyList()));

        return !(filteringKeys.containsAll(entityFields) && filteringKeys.containsAll(latestValues));
    }

    private PageData<EntityData> findEntityIdsByFilterAndSorterColumns(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        List<EntityKey> entityFields = null;
        List<EntityKey> latestValues = null;
        if (query.getPageLink().getSortOrder() != null) {
            if (query.getEntityFields() != null) {
                entityFields = query.getEntityFields().stream()
                        .filter(entityKey -> entityKey.getKey().equals(query.getPageLink().getSortOrder().getKey().getKey()))
                        .collect(Collectors.toList());
            }
            if (query.getLatestValues() != null) {
                latestValues = query.getLatestValues().stream()
                        .filter(entityKey -> entityKey.getKey().equals(query.getPageLink().getSortOrder().getKey().getKey()))
                        .collect(Collectors.toList());
            }
        }
        EntityDataQuery entityQuery = new EntityDataQuery(query.getEntityFilter(), query.getPageLink(), entityFields, latestValues, query.getKeyFilters());
        return this.entityQueryDao.findEntityDataByQuery(tenantId, customerId, entityQuery);
    }

    private List<EntityData> fetchEntityDataByIdsFromInitialQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query, List<EntityData> initialQueryResult) {
        List<EntityData> result = new ArrayList<>();

        List<String> entityIds = initialQueryResult.stream().map(d -> d.getEntityId().getId().toString()).collect(Collectors.toList());
        EntityType entityType = initialQueryResult.get(0).getEntityId().getEntityType();

        if (entityIds.size() > MAX_ENTITY_IDS_SIZE) {
            List<List<String>> chunks = new ArrayList<>();
            for (int i = 0; i < entityIds.size(); i += MAX_ENTITY_IDS_SIZE) {
                chunks.add(entityIds.subList(i, Math.min(entityIds.size(), i + MAX_ENTITY_IDS_SIZE)));
            }
            for (List<String> chunk : chunks) {
                result.addAll(findEntityDataByEntityIds(tenantId, customerId, query, chunk, entityType, chunk.size()));
            }
        } else {
            result.addAll(findEntityDataByEntityIds(tenantId, customerId, query, entityIds, entityType, query.getPageLink().getPageSize()));
        }
        return result;
    }

    private List<EntityData> findEntityDataByEntityIds(TenantId tenantId, CustomerId customerId, EntityDataQuery query,
                                                       List<String> entityIds, EntityType entityType, int pageSize) {
        EntityListFilter filter = new EntityListFilter();
        filter.setEntityType(entityType);
        filter.setEntityList(entityIds);

        EntityDataPageLink pageLink = new EntityDataPageLink(pageSize, 0, null, query.getPageLink().getSortOrder());
        EntityDataQuery entityQuery = new EntityDataQuery(filter, pageLink, query.getEntityFields(), query.getLatestValues(), null);
        return this.entityQueryDao.findEntityDataByQuery(tenantId, customerId, entityQuery).getData();
    }

}
