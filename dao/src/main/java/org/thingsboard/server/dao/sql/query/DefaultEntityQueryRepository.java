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
package org.thingsboard.server.dao.sql.query;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@SqlDao
@Repository
@Slf4j
public class DefaultEntityQueryRepository implements EntityQueryRepository {

    private static final Map<EntityType, String> entityTableMap = new HashMap<>();
    static {
        entityTableMap.put(EntityType.ASSET, "asset");
        entityTableMap.put(EntityType.DEVICE, "device");
        entityTableMap.put(EntityType.ENTITY_VIEW, "entity_view");
        entityTableMap.put(EntityType.DASHBOARD, "dashboard");
        entityTableMap.put(EntityType.CUSTOMER, "customer");
        entityTableMap.put(EntityType.USER, "tb_user");
        entityTableMap.put(EntityType.TENANT, "tenant");
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        EntityType entityType = resolveEntityType(query.getEntityFilter());
        String countQuery = String.format("select count(e.id) from %s e where %s",
                entityTableMap.get(entityType), this.buildEntityWhere(tenantId, customerId, query.getEntityFilter(),
                        Collections.emptyList(), entityType));
        return ((BigInteger)entityManager.createNativeQuery(countQuery)
                .getSingleResult()).longValue();
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        EntityType entityType = resolveEntityType(query.getEntityFilter());
        EntityDataPageLink pageLink = query.getPageLink();

        List<EntityKeyMapping> mappings = EntityKeyMapping.prepareKeyMapping(query);

        List<EntityKeyMapping> selectionMapping = mappings.stream().filter(EntityKeyMapping::isSelection)
                .collect(Collectors.toList());
        List<EntityKeyMapping> entityFieldsSelectionMapping = selectionMapping.stream().filter(mapping -> !mapping.isLatest())
                .collect(Collectors.toList());
        List<EntityKeyMapping> latestSelectionMapping = selectionMapping.stream().filter(EntityKeyMapping::isLatest)
                .collect(Collectors.toList());

        List<EntityKeyMapping> filterMapping = mappings.stream().filter(EntityKeyMapping::hasFilter)
                .collect(Collectors.toList());
        List<EntityKeyMapping> entityFieldsFiltersMapping = filterMapping.stream().filter(mapping -> !mapping.isLatest())
                .collect(Collectors.toList());
        List<EntityKeyMapping> latestFiltersMapping = filterMapping.stream().filter(EntityKeyMapping::isLatest)
                .collect(Collectors.toList());

        List<EntityKeyMapping> allLatestMappings = mappings.stream().filter(EntityKeyMapping::isLatest)
                .collect(Collectors.toList());


        String entityWhereClause = this.buildEntityWhere(tenantId, customerId, query.getEntityFilter(), entityFieldsFiltersMapping, entityType);
        String latestJoins = EntityKeyMapping.buildLatestJoins(entityType, allLatestMappings);
        String whereClause = this.buildWhere(selectionMapping, latestFiltersMapping, pageLink.getTextSearch());
        String entityFieldsSelection = EntityKeyMapping.buildSelections(entityFieldsSelectionMapping);
        if (!StringUtils.isEmpty(entityFieldsSelection)) {
            entityFieldsSelection = String.format("e.id, '%s', %s", entityType.name(), entityFieldsSelection);
        } else {
            entityFieldsSelection = String.format("e.id, '%s'", entityType.name());
        }
        String latestSelection = EntityKeyMapping.buildSelections(latestSelectionMapping);
        String topSelection = "entities.*";
        if (!StringUtils.isEmpty(latestSelection)) {
            topSelection = topSelection + ", " + latestSelection;
        }

        String fromClause = String.format("from (select %s from (select %s from %s e where %s) entities %s %s) result",
                topSelection,
                entityFieldsSelection,
                entityTableMap.get(entityType),
                entityWhereClause,
                latestJoins,
                whereClause);

        int totalElements = ((BigInteger)entityManager.createNativeQuery(String.format("select count(*) %s", fromClause))
                .getSingleResult()).intValue();

        String dataQuery = String.format("select * %s", fromClause);

        EntityDataSortOrder sortOrder = pageLink.getSortOrder();
        if (sortOrder != null) {
            Optional<EntityKeyMapping> sortOrderMappingOpt = mappings.stream().filter(EntityKeyMapping::isSortOrder).findFirst();
            if (sortOrderMappingOpt.isPresent()) {
                EntityKeyMapping sortOrderMapping = sortOrderMappingOpt.get();
                dataQuery = String.format("%s order by %s", dataQuery, sortOrderMapping.getValueAlias());
                if (sortOrder.getDirection() == EntityDataSortOrder.Direction.ASC) {
                    dataQuery += " asc";
                } else {
                    dataQuery += " desc";
                }
            }
        }
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        if (pageLink.getPageSize() > 0) {
            dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
        }
        List rows = entityManager.createNativeQuery(dataQuery).getResultList();
        return EntityDataAdapter.createEntityData(pageLink, selectionMapping, rows, totalElements);
    }

    private String buildEntityWhere(TenantId tenantId,
                              CustomerId customerId,
                              EntityFilter entityFilter,
                              List<EntityKeyMapping> entityFieldsFilters,
                              EntityType entityType) {
        String permissionQuery = this.buildPermissionQuery(tenantId, customerId, entityType);
        String entityFilterQuery = this.buildEntityFilterQuery(entityFilter);
        if (!entityFieldsFilters.isEmpty()) {
            String entityFieldsQuery = EntityKeyMapping.buildQuery(entityFieldsFilters);
            return String.join(" and ", permissionQuery, entityFilterQuery, entityFieldsQuery);
        } else {
            return String.join(" and ", permissionQuery, entityFilterQuery);
        }
    }

    private String buildPermissionQuery(TenantId tenantId, CustomerId customerId, EntityType entityType) {
        String permissionQuery = String.format("e.tenant_id='%s'", UUIDConverter.fromTimeUUID(tenantId.getId()));
        if (entityType != EntityType.TENANT && entityType != EntityType.CUSTOMER) {
            permissionQuery = String.format("%s and e.customer_id='%s'", permissionQuery, UUIDConverter.fromTimeUUID(customerId.getId()));
        }
        return permissionQuery;
    }

    private String buildEntityFilterQuery(EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return this.singleEntityQuery((SingleEntityFilter) entityFilter);
            case ENTITY_LIST:
                return this.entityListQuery((EntityListFilter) entityFilter);
            case ENTITY_NAME:
                return this.entityNameQuery((EntityNameFilter) entityFilter);
            case ASSET_TYPE:
            case DEVICE_TYPE:
            case ENTITY_VIEW_TYPE:
                return this.typeQuery(entityFilter);
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String buildWhere(List<EntityKeyMapping> selectionMapping, List<EntityKeyMapping> latestFiltersMapping, String searchText) {
        String latestFilters = EntityKeyMapping.buildQuery(latestFiltersMapping);
        String textSearchQuery = this.buildTextSearchQuery(selectionMapping, searchText);
        String query;
        if (!StringUtils.isEmpty(latestFilters) && !StringUtils.isEmpty(textSearchQuery)) {
            query = String.join(" AND ", latestFilters, textSearchQuery);
        } else if (!StringUtils.isEmpty(latestFilters)) {
            query = latestFilters;
        } else {
            query = textSearchQuery;
        }
        if (!StringUtils.isEmpty(query)) {
            return String.format("where %s", query);
        } else {
            return "";
        }
    }

    private String buildTextSearchQuery(List<EntityKeyMapping> selectionMapping, String searchText) {
        if (!StringUtils.isEmpty(searchText) && !selectionMapping.isEmpty()) {
            String lowerSearchText = searchText.toLowerCase() + "%";
            List<String> searchPredicates = selectionMapping.stream().map(mapping -> String.format("LOWER(%s) LIKE '%s'",
                    mapping.getValueAlias(), lowerSearchText)).collect(Collectors.toList());
            return String.format("(%s)", String.join(" or ", searchPredicates));
        } else {
            return null;
        }
    }

    private String singleEntityQuery(SingleEntityFilter filter) {
        return String.format("e.id='%s'", UUIDConverter.fromTimeUUID(filter.getSingleEntity().getId()));
    }

    private String entityListQuery(EntityListFilter filter) {
        return String.format("e.id in (%s)",
                filter.getEntityList().stream().map(UUID::fromString).map(UUIDConverter::fromTimeUUID)
                        .map(s -> String.format("'%s'", s)).collect(Collectors.joining(",")));
    }

    private String entityNameQuery(EntityNameFilter filter) {
        return String.format("lower(e.search_text) like lower(concat(%s, '%%'))", filter.getEntityNameFilter());
    }

    private String typeQuery(EntityFilter filter) {
        String type;
        String name;
        switch (filter.getType()) {
            case ASSET_TYPE:
                type = ((AssetTypeFilter)filter).getAssetType();
                name = ((AssetTypeFilter)filter).getAssetNameFilter();
                break;
            case DEVICE_TYPE:
                type = ((DeviceTypeFilter)filter).getDeviceType();
                name = ((DeviceTypeFilter)filter).getDeviceNameFilter();
                break;
            case ENTITY_VIEW_TYPE:
                type = ((EntityViewTypeFilter)filter).getEntityViewType();
                name = ((EntityViewTypeFilter)filter).getEntityViewNameFilter();
                break;
            default:
                throw new RuntimeException("Not supported!");
        }
        return String.format("e.type = '%s' and lower(e.search_text) like lower(concat('%s', '%%'))", type, name);
    }

    private EntityType resolveEntityType(EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return ((SingleEntityFilter)entityFilter).getSingleEntity().getEntityType();
            case ENTITY_LIST:
                return ((EntityListFilter)entityFilter).getEntityType();
            case ENTITY_NAME:
                return ((EntityNameFilter)entityFilter).getEntityType();
            case ASSET_TYPE:
            case ASSET_SEARCH_QUERY:
                return EntityType.ASSET;
            case DEVICE_TYPE:
            case DEVICE_SEARCH_QUERY:
                return EntityType.DEVICE;
            case ENTITY_VIEW_TYPE:
            case ENTITY_VIEW_SEARCH_QUERY:
                return EntityType.ENTITY_VIEW;
           default:
               throw new RuntimeException("Not implemented!");
        }
    }
}
