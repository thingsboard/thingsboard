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
package org.thingsboard.server.dao.entity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasEmail;
import org.thingsboard.server.common.data.HasLabel;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTitle;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.Optional;

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
    EntityServiceBeanFactory entityServiceBeanFactory;

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        log.trace("Executing countEntitiesByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateEntityCountQuery(query);
        return this.entityQueryDao.countEntitiesByQuery(tenantId, customerId, query);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        log.trace("Executing findEntityDataByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateEntityDataQuery(query);
        return this.entityQueryDao.findEntityDataByQuery(tenantId, customerId, query);
    }

    @Override
    public Optional<String> fetchEntityName(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityName [{}]", entityId);
        TbEntityService tbEntityService = entityServiceBeanFactory.getServiceByEntityType(entityId.getEntityType());
        Optional<HasId<?>> hasIdOpt = tbEntityService.fetchEntity(tenantId, entityId);
        if (hasIdOpt.isPresent()) {
            HasId<?> hasId = hasIdOpt.get();
            if (hasId instanceof HasName) {
                HasName hasName = (HasName) hasId;
                return Optional.ofNullable(hasName.getName());
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> fetchEntityLabel(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityLabel [{}]", entityId);
        TbEntityService tbEntityService = entityServiceBeanFactory.getServiceByEntityType(entityId.getEntityType());
        Optional<HasId<?>> entityOpt = tbEntityService.fetchEntity(tenantId, entityId);
        String entityLabel = null;
        if (entityOpt.isPresent()) {
            HasId<?> entity = entityOpt.get();
            if (entity instanceof HasTitle) {
                entityLabel = ((HasTitle) entity).getTitle();
            }
            if (entity instanceof HasLabel && entityLabel == null) {
                entityLabel = ((HasLabel) entity).getLabel();
            }
            if (entity instanceof HasEmail && entityLabel == null) {
                entityLabel = ((HasEmail) entity).getEmail();
            }
            if (entity instanceof HasName && entityLabel == null) {
                entityLabel = ((HasName) entity).getName();
            }
        }
        return Optional.ofNullable(entityLabel);
    }

    @Override
    public CustomerId fetchEntityCustomerId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityCustomerId [{}]", entityId);
        TbEntityService tbEntityService = entityServiceBeanFactory.getServiceByEntityType(entityId.getEntityType());
        Optional<HasId<?>> hasIdOpt = tbEntityService.fetchEntity(tenantId, entityId);
        if (hasIdOpt.isPresent()) {
            HasId<?> hasId = hasIdOpt.get();
            if (hasId instanceof HasCustomerId) {
                HasCustomerId hasCustomerId = (HasCustomerId) hasId;
                return hasCustomerId.getCustomerId();
            }
        }
        return NULL_CUSTOMER_ID;
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
