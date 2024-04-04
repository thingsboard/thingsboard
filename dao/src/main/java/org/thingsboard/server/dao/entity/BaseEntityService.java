/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasEmail;
import org.thingsboard.server.common.data.HasLabel;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTitle;
import org.thingsboard.server.common.data.StringUtils;
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
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.id.EntityId.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateEntityDataPageLink;
import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * Created by ashvayka on 04.05.17.
 */
@Service
@Slf4j
public class BaseEntityService extends AbstractEntityService implements EntityService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final CustomerId NULL_CUSTOMER_ID = new CustomerId(NULL_UUID);

    @Autowired
    private EntityQueryDao entityQueryDao;

    @Autowired
    @Lazy
    EntityServiceRegistry entityServiceRegistry;

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        log.trace("Executing countEntitiesByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateEntityCountQuery(query);
        return this.entityQueryDao.countEntitiesByQuery(tenantId, customerId, query);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        log.trace("Executing findEntityDataByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateEntityDataQuery(query);

        if (EntityFilterType.RELATIONS_QUERY.equals(query.getEntityFilter().getType())
                || EntityFilterType.SINGLE_ENTITY.equals(query.getEntityFilter().getType())
                || StringUtils.isNotEmpty(query.getPageLink().getTextSearch())) {
            return this.entityQueryDao.findEntityDataByQuery(tenantId, customerId, query);
        }

        // 1 step - find entity data by filter and sort columns
        PageData<EntityData> entityDataByQuery = findEntityIdsByFilterAndSorterColumns(tenantId, customerId, query);
        if (entityDataByQuery == null || entityDataByQuery.getData().isEmpty()) {
            return entityDataByQuery;
        }
        // 2 step - find entity data by entity ids from the 1st step
        PageData<EntityData> result = findEntityDataByEntityIds(tenantId, customerId, query, entityDataByQuery.getData());
        return new PageData<>(result.getData(), entityDataByQuery.getTotalPages(), entityDataByQuery.getTotalElements(), entityDataByQuery.hasNext());
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

    private PageData<EntityData> findEntityDataByEntityIds(TenantId tenantId, CustomerId customerId, EntityDataQuery query, List<EntityData> data) {
        List<String> entityIds = data.stream().map(d -> d.getEntityId().getId().toString()).toList();
        EntityType entityType = data.isEmpty() ? null : data.get(0).getEntityId().getEntityType();

        EntityListFilter filter = new EntityListFilter();
        filter.setEntityType(entityType);
        filter.setEntityList(entityIds);

        EntityDataPageLink pageLink = new EntityDataPageLink(query.getPageLink().getPageSize(), 0, null, query.getPageLink().getSortOrder());
        EntityDataQuery entityQuery = new EntityDataQuery(filter, pageLink, query.getEntityFields(), query.getLatestValues(), null);
        return this.entityQueryDao.findEntityDataByQuery(tenantId, customerId, entityQuery);
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
    public Optional<NameLabelAndCustomerDetails> fetchNameLabelAndCustomerDetails(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchNameLabelAndCustomerDetails [{}]", entityId);
        return fetchAndConvert(tenantId, entityId, this::getNameLabelAndCustomerDetails);
    }

    private <T> Optional<T> fetchAndConvert(TenantId tenantId, EntityId entityId, Function<HasId<?>, T> converter) {
        EntityDaoService entityDaoService = entityServiceRegistry.getServiceByEntityType(entityId.getEntityType());
        Optional<HasId<?>> entityOpt = entityDaoService.findEntity(tenantId, entityId);
        return entityOpt.map(converter);
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
        }
    }

    private static void validateEntityDataQuery(EntityDataQuery query) {
        validateEntityCountQuery(query);
        validateEntityDataPageLink(query.getPageLink());
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

}
