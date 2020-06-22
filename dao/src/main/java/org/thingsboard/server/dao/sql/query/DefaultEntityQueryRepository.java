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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AssetSearchQueryFilter;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.DeviceSearchQueryFilter;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntitySearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.EntityTypeFilter;
import org.thingsboard.server.dao.util.SqlDao;

import java.math.BigInteger;
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
    //TODO: rafactoring to protect from SQL injections;
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

    public static final String HIERARCHICAL_QUERY_TEMPLATE = " FROM (WITH RECURSIVE related_entities(from_id, from_type, to_id, to_type, relation_type, lvl) AS (" +
            " SELECT from_id, from_type, to_id, to_type, relation_type, 1 as lvl" +
            " FROM relation" +
            " WHERE $in_id = :relation_root_id and $in_type = :relation_root_type and relation_type_group = 'COMMON'" +
            " UNION ALL" +
            " SELECT r.from_id, r.from_type, r.to_id, r.to_type, r.relation_type, lvl + 1" +
            " FROM relation r" +
            " INNER JOIN related_entities re ON" +
            " r.$in_id = re.$out_id and r.$in_type = re.$out_type and" +
            " relation_type_group = 'COMMON' %s)" +
            " SELECT re.$out_id entity_id, re.$out_type entity_type, re.lvl lvl" +
            " from related_entities re" +
            " %s ) entity";
    public static final String HIERARCHICAL_TO_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE.replace("$in", "to").replace("$out", "from");
    public static final String HIERARCHICAL_FROM_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE.replace("$in", "from").replace("$out", "to");

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        EntityType entityType = resolveEntityType(query.getEntityFilter());
        EntityQueryContext ctx = new EntityQueryContext();
        ctx.append("select count(e.id) from ");
        ctx.append(addEntityTableQuery(ctx, query.getEntityFilter(), entityType));
        ctx.append(" e where ");
        ctx.append(buildEntityWhere(ctx, tenantId, customerId, query.getEntityFilter(), Collections.emptyList(), entityType));
        return jdbcTemplate.queryForObject(ctx.getQuery(), ctx, Long.class);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        EntityQueryContext ctx = new EntityQueryContext();
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


        String entityWhereClause = this.buildEntityWhere(ctx, tenantId, customerId, query.getEntityFilter(), entityFieldsFiltersMapping, entityType);
        String latestJoins = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), entityType, allLatestMappings);
        String whereClause = this.buildWhere(ctx, selectionMapping, latestFiltersMapping, pageLink.getTextSearch());
        String entityFieldsSelection = EntityKeyMapping.buildSelections(entityFieldsSelectionMapping);
        String entityTypeStr;
        if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            entityTypeStr = "e.entity_type";
        } else {
            entityTypeStr = "'" + entityType.name() + "'";
        }
        if (!StringUtils.isEmpty(entityFieldsSelection)) {
            entityFieldsSelection = String.format("e.id id, %s entity_type, %s", entityTypeStr, entityFieldsSelection);
        } else {
            entityFieldsSelection = String.format("e.id id, %s entity_type", entityTypeStr);
        }
        String latestSelection = EntityKeyMapping.buildSelections(latestSelectionMapping);
        String topSelection = "entities.*";
        if (!StringUtils.isEmpty(latestSelection)) {
            topSelection = topSelection + ", " + latestSelection;
        }

        String fromClause = String.format("from (select %s from (select %s from %s e where %s) entities %s %s) result",
                topSelection,
                entityFieldsSelection,
                addEntityTableQuery(ctx, query.getEntityFilter(), entityType),
                entityWhereClause,
                latestJoins,
                whereClause);


        int totalElements = jdbcTemplate.queryForObject(String.format("select count(*) %s", fromClause), ctx, Integer.class);

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
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataQuery, ctx);
        return EntityDataAdapter.createEntityData(pageLink, selectionMapping, rows, totalElements);
    }

    private String buildEntityWhere(EntityQueryContext ctx,
                                    TenantId tenantId,
                                    CustomerId customerId,
                                    EntityFilter entityFilter,
                                    List<EntityKeyMapping> entityFieldsFilters,
                                    EntityType entityType) {
        String permissionQuery = this.buildPermissionQuery(ctx, entityFilter, tenantId, customerId, entityType);
        String entityFilterQuery = this.buildEntityFilterQuery(ctx, entityFilter);
        String result = permissionQuery;
        if (!entityFilterQuery.isEmpty()) {
            result += " and " + entityFilterQuery;
        }
        if (!entityFieldsFilters.isEmpty()) {
            result += " and " + entityFieldsFilters;
        }
        return result;
    }

    private String buildPermissionQuery(EntityQueryContext ctx, EntityFilter entityFilter, TenantId tenantId, CustomerId customerId, EntityType entityType) {
        switch (entityFilter.getType()) {
            case RELATIONS_QUERY:
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
                ctx.addUuidParameter("permissions_tenant_id", tenantId.getId());
                ctx.addUuidParameter("permissions_customer_id", customerId.getId());
                return "e.tenant_id=:permissions_tenant_id and e.customer_id=:permissions_customer_id";
            default:
                if (entityType == EntityType.TENANT) {
                    ctx.addUuidParameter("permissions_tenant_id", tenantId.getId());
                    return "e.id=:permissions_tenant_id";
                } else if (entityType == EntityType.CUSTOMER) {
                    ctx.addUuidParameter("permissions_tenant_id", tenantId.getId());
                    ctx.addUuidParameter("permissions_customer_id", customerId.getId());
                    return "e.tenant_id=:permissions_tenant_id and e.id=:permissions_customer_id";
                } else {
                    ctx.addUuidParameter("permissions_tenant_id", tenantId.getId());
                    ctx.addUuidParameter("permissions_customer_id", customerId.getId());
                    return "e.tenant_id=:permissions_tenant_id and e.customer_id=:permissions_customer_id";
                }
        }
    }

    private String buildEntityFilterQuery(EntityQueryContext ctx, EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return this.singleEntityQuery(ctx, (SingleEntityFilter) entityFilter);
            case ENTITY_LIST:
                return this.entityListQuery(ctx, (EntityListFilter) entityFilter);
            case ENTITY_NAME:
                return this.entityNameQuery(ctx, (EntityNameFilter) entityFilter);
            case ASSET_TYPE:
            case DEVICE_TYPE:
            case ENTITY_VIEW_TYPE:
                return this.typeQuery(ctx, entityFilter);
            case RELATIONS_QUERY:
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
                return "";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String addEntityTableQuery(EntityQueryContext ctx, EntityFilter entityFilter, EntityType entityType) {
        switch (entityFilter.getType()) {
            case RELATIONS_QUERY:
                return relationQuery(ctx, (RelationsQueryFilter) entityFilter);
            case DEVICE_SEARCH_QUERY:
                DeviceSearchQueryFilter deviceQuery = (DeviceSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, deviceQuery, EntityType.DEVICE, deviceQuery.getDeviceTypes());
            case ASSET_SEARCH_QUERY:
                AssetSearchQueryFilter assetQuery = (AssetSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, assetQuery, EntityType.ASSET, assetQuery.getAssetTypes());
            default:
                return entityTableMap.get(entityType);
        }
    }

    private String entitySearchQuery(EntityQueryContext ctx, EntitySearchQueryFilter entityFilter, EntityType entityType, List<String> types) {
        EntityId rootId = entityFilter.getRootEntity();
        //TODO: fetch last level only.
        //TODO: fetch distinct records.
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = "SELECT tenant_id, customer_id, id, type, name, label FROM " + entityType.name() + " WHERE id in ( SELECT entity_id";
        String from = getQueryTemplate(entityFilter.getDirection());
        String whereFilter = " WHERE re.relation_type = :where_relation_type AND re.to_type = :where_entity_type";

        from = String.format(from, lvlFilter, whereFilter);
        String query = "( " + selectFields + from + ")";
        if (types != null && !types.isEmpty()) {
            query += " and type in (:relation_sub_types)";
            ctx.addStringListParameter("relation_sub_types", types);
        }
        query += " )";
        ctx.addUuidParameter("relation_root_id", rootId.getId());
        ctx.addStringParameter("relation_root_type", rootId.getEntityType().name());
        ctx.addStringParameter("where_relation_type", entityFilter.getRelationType());
        ctx.addStringParameter("where_entity_type", entityType.name());
        return query;
    }

    private String relationQuery(EntityQueryContext ctx, RelationsQueryFilter entityFilter) {
        EntityId rootId = entityFilter.getRootEntity();
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = getSelectTenantId() + ", " + getSelectCustomerId() + ", " +
                " entity.entity_id as id," + getSelectType() + ", " + getSelectName() + ", " +
                getSelectLabel() + ", entity.entity_type as entity_type";
        String from = getQueryTemplate(entityFilter.getDirection());
        ctx.addUuidParameter("relation_root_id", rootId.getId());
        ctx.addStringParameter("relation_root_type", rootId.getEntityType().name());

        StringBuilder whereFilter;
        if (entityFilter.getFilters() != null && !entityFilter.getFilters().isEmpty()) {
            whereFilter = new StringBuilder(" WHERE ");
            boolean first = true;
            boolean single = entityFilter.getFilters().size() == 1;
            int entityTypeFilterIdx = 0;
            for (EntityTypeFilter etf : entityFilter.getFilters()) {
                if (first) {
                    first = false;
                } else {
                    whereFilter.append(" AND ");
                }
                String relationType = etf.getRelationType();
                if (!single) {
                    whereFilter.append(" (");
                }
                whereFilter.append(" re.relation_type = :where_relation_type").append(entityTypeFilterIdx).append(" and re.")
                        .append(entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from")
                        .append("_type in (:where_entity_types").append(entityTypeFilterIdx).append(")");
                if (!single) {
                    whereFilter.append(" )");
                }
                ctx.addStringParameter("where_relation_type" + entityTypeFilterIdx, relationType);
                ctx.addStringListParameter("where_entity_types" + entityTypeFilterIdx, etf.getEntityTypes().stream().map(EntityType::name).collect(Collectors.toList()));
                entityTypeFilterIdx++;
            }
        } else {
            whereFilter = new StringBuilder();
        }
        from = String.format(from, lvlFilter, whereFilter);
        return "( " + selectFields + from + ")";
    }

    private String getLvlFilter(int maxLevel) {
        return maxLevel > 0 ? ("and lvl <= " + (maxLevel - 1)) : "";
    }

    private String getQueryTemplate(EntitySearchDirection direction) {
        String from;
        if (direction.equals(EntitySearchDirection.FROM)) {
            from = HIERARCHICAL_FROM_QUERY_TEMPLATE;
        } else {
            from = HIERARCHICAL_TO_QUERY_TEMPLATE;
        }
        return from;
    }

    private String getSelectTenantId() {
        return "SELECT CASE" +
                " WHEN entity.entity_type = 'TENANT' THEN entity_id" +
                " WHEN entity.entity_type = 'CUSTOMER'" +
                " THEN (select tenant_id from customer where id = entity_id)" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select tenant_id from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'DASHBOARD'" +
                " THEN (select tenant_id from dashboard where id = entity_id)" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN (select tenant_id from asset where id = entity_id)" +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN (select tenant_id from device where id = entity_id)" +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN (select tenant_id from entity_view where id = entity_id)" +
                " END as tenant_id";
    }

    private String getSelectCustomerId() {
        return "CASE" +
                " WHEN entity.entity_type = 'TENANT'" +
                " THEN UUID('" + TenantId.NULL_UUID + "')" +
                " WHEN entity.entity_type = 'CUSTOMER' THEN entity_id" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select customer_id from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'DASHBOARD'" +
                //TODO: parse assigned customers or use contains?
                " THEN NULL" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN (select customer_id from asset where id = entity_id)" +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN (select customer_id from device where id = entity_id)" +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN (select customer_id from entity_view where id = entity_id)" +
                " END as customer_id";
    }

    private String getSelectName() {
        return " CASE" +
                " WHEN entity.entity_type = 'TENANT'" +
                " THEN (select title from tenant where id = entity_id)" +
                " WHEN entity.entity_type = 'CUSTOMER' " +
                " THEN (select title from customer where id = entity_id)" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select CONCAT (first_name, ' ', last_name) from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'DASHBOARD'" +
                " THEN (select title from dashboard where id = entity_id)" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN (select name from asset where id = entity_id)" +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN (select name from device where id = entity_id)" +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN (select name from entity_view where id = entity_id)" +
                " END as name";
    }

    private String getSelectType() {
        return " CASE" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select authority from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN (select type from asset where id = entity_id)" +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN (select type from device where id = entity_id)" +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN (select type from entity_view where id = entity_id)" +
                " ELSE entity.entity_type END as type";
    }

    private String getSelectLabel() {
        return " CASE" +
                " WHEN entity.entity_type = 'TENANT'" +
                " THEN (select title from tenant where id = entity_id)" +
                " WHEN entity.entity_type = 'CUSTOMER' " +
                " THEN (select title from customer where id = entity_id)" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select CONCAT (first_name, ' ', last_name) from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'DASHBOARD'" +
                " THEN (select title from dashboard where id = entity_id)" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN (select label from asset where id = entity_id)" +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN (select label from device where id = entity_id)" +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN (select name from entity_view where id = entity_id)" +
                " END as label";
    }

    private String buildWhere(EntityQueryContext ctx, List<EntityKeyMapping> selectionMapping, List<EntityKeyMapping> latestFiltersMapping, String searchText) {
        String latestFilters = EntityKeyMapping.buildQuery(ctx, latestFiltersMapping);
        String textSearchQuery = this.buildTextSearchQuery(ctx, selectionMapping, searchText);
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

    private String buildTextSearchQuery(EntityQueryContext ctx, List<EntityKeyMapping> selectionMapping, String searchText) {
        if (!StringUtils.isEmpty(searchText) && !selectionMapping.isEmpty()) {
            String lowerSearchText = searchText.toLowerCase() + "%";
            List<String> searchPredicates = selectionMapping.stream().map(mapping -> {
                        String paramName = mapping.getValueAlias() + "_lowerSearchText";
                        ctx.addStringParameter(paramName, lowerSearchText);
                        return String.format("LOWER(%s) LIKE :%s", mapping.getValueAlias(), paramName);
                    }
            ).collect(Collectors.toList());
            return String.format("(%s)", String.join(" or ", searchPredicates));
        } else {
            return null;
        }

    }

    private String singleEntityQuery(EntityQueryContext ctx, SingleEntityFilter filter) {
        ctx.addUuidParameter("entity_filter_single_entity_id", filter.getSingleEntity().getId());
        return "e.id=:entity_filter_single_entity_id";
    }

    private String entityListQuery(EntityQueryContext ctx, EntityListFilter filter) {
        ctx.addUuidListParameter("entity_filter_entity_ids", filter.getEntityList().stream().map(UUID::fromString).collect(Collectors.toList()));
        return "e.id in (:entity_filter_entity_ids)";
    }

    private String entityNameQuery(EntityQueryContext ctx, EntityNameFilter filter) {
        ctx.addStringParameter("entity_filter_name_filter", filter.getEntityNameFilter());
        return "lower(e.search_text) like lower(concat(:entity_filter_name_filter, '%%'))";
    }

    private String typeQuery(EntityQueryContext ctx, EntityFilter filter) {
        String type;
        String name;
        switch (filter.getType()) {
            case ASSET_TYPE:
                type = ((AssetTypeFilter) filter).getAssetType();
                name = ((AssetTypeFilter) filter).getAssetNameFilter();
                break;
            case DEVICE_TYPE:
                type = ((DeviceTypeFilter) filter).getDeviceType();
                name = ((DeviceTypeFilter) filter).getDeviceNameFilter();
                break;
            case ENTITY_VIEW_TYPE:
                type = ((EntityViewTypeFilter) filter).getEntityViewType();
                name = ((EntityViewTypeFilter) filter).getEntityViewNameFilter();
                break;
            default:
                throw new RuntimeException("Not supported!");
        }
        ctx.addStringParameter("entity_filter_type_query_type", type);
        ctx.addStringParameter("entity_filter_type_query_name", name);
        return "e.type = :entity_filter_type_query_type and lower(e.search_text) like lower(concat(:entity_filter_type_query_name, '%%'))";
    }

    private EntityType resolveEntityType(EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return ((SingleEntityFilter) entityFilter).getSingleEntity().getEntityType();
            case ENTITY_LIST:
                return ((EntityListFilter) entityFilter).getEntityType();
            case ENTITY_NAME:
                return ((EntityNameFilter) entityFilter).getEntityType();
            case ASSET_TYPE:
            case ASSET_SEARCH_QUERY:
                return EntityType.ASSET;
            case DEVICE_TYPE:
            case DEVICE_SEARCH_QUERY:
                return EntityType.DEVICE;
            case ENTITY_VIEW_TYPE:
            case ENTITY_VIEW_SEARCH_QUERY:
                return EntityType.ENTITY_VIEW;
            case RELATIONS_QUERY:
                return ((RelationsQueryFilter) entityFilter).getRootEntity().getEntityType();
            default:
                throw new RuntimeException("Not implemented!");
        }
    }
}
