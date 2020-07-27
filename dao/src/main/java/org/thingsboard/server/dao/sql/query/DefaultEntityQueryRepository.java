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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
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
import org.thingsboard.server.common.data.query.EntityViewSearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.EntityTypeFilter;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
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
    private static final String SELECT_PHONE = " CASE WHEN entity.entity_type = 'TENANT' THEN (select phone from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' THEN (select phone from customer where id = entity_id) END as phone";
    private static final String SELECT_ZIP = " CASE WHEN entity.entity_type = 'TENANT' THEN (select zip from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' THEN (select zip from customer where id = entity_id) END as zip";
    private static final String SELECT_ADDRESS_2 = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select address2 from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select address2 from customer where id = entity_id) END as address2";
    private static final String SELECT_ADDRESS = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select address from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select address from customer where id = entity_id) END as address";
    private static final String SELECT_CITY = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select city from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select city from customer where id = entity_id) END as city";
    private static final String SELECT_STATE = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select state from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select state from customer where id = entity_id) END as state";
    private static final String SELECT_COUNTRY = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select country from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select country from customer where id = entity_id) END as country";
    private static final String SELECT_TITLE = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select title from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select title from customer where id = entity_id) END as title";
    private static final String SELECT_LAST_NAME = " CASE WHEN entity.entity_type = 'USER'" +
            " THEN (select last_name from tb_user where id = entity_id) END as last_name";
    private static final String SELECT_FIRST_NAME = " CASE WHEN entity.entity_type = 'USER'" +
            " THEN (select first_name from tb_user where id = entity_id) END as first_name";
    private static final String SELECT_REGION = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select region from tenant where id = entity_id) END as region";
    private static final String SELECT_EMAIL = " CASE" +
            " WHEN entity.entity_type = 'TENANT'" +
            " THEN (select email from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select email from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select email from tb_user where id = entity_id)" +
            " END as email";
    private static final String SELECT_CUSTOMER_ID = "CASE" +
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
    private static final String SELECT_TENANT_ID = "SELECT CASE" +
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
    private static final String SELECT_CREATED_TIME = " CASE" +
            " WHEN entity.entity_type = 'TENANT'" +
            " THEN (select created_time from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select created_time from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select created_time from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'DASHBOARD'" +
            " THEN (select created_time from dashboard where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select created_time from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select created_time from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select created_time from entity_view where id = entity_id)" +
            " END as created_time";
    private static final String SELECT_NAME = " CASE" +
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
    private static final String SELECT_TYPE = " CASE" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select authority from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select type from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select type from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select type from entity_view where id = entity_id)" +
            " ELSE entity.entity_type END as type";
    private static final String SELECT_LABEL = " CASE" +
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

    static {
        entityTableMap.put(EntityType.ASSET, "asset");
        entityTableMap.put(EntityType.DEVICE, "device");
        entityTableMap.put(EntityType.ENTITY_VIEW, "entity_view");
        entityTableMap.put(EntityType.DASHBOARD, "dashboard");
        entityTableMap.put(EntityType.CUSTOMER, "customer");
        entityTableMap.put(EntityType.USER, "tb_user");
        entityTableMap.put(EntityType.TENANT, "tenant");
    }

    private static final String HIERARCHICAL_QUERY_TEMPLATE = " FROM (WITH RECURSIVE related_entities(from_id, from_type, to_id, to_type, relation_type, lvl) AS (" +
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
    private static final String HIERARCHICAL_TO_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE.replace("$in", "to").replace("$out", "from");
    private static final String HIERARCHICAL_FROM_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE.replace("$in", "from").replace("$out", "to");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public DefaultEntityQueryRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        EntityType entityType = resolveEntityType(query.getEntityFilter());
        QueryContext ctx = new QueryContext(new QuerySecurityContext(tenantId, customerId, entityType));
        ctx.append("select count(e.id) from ");
        ctx.append(addEntityTableQuery(ctx, query.getEntityFilter()));
        ctx.append(" e where ");
        ctx.append(buildEntityWhere(ctx, query.getEntityFilter(), Collections.emptyList()));
//        log.error("QUERY: {}", ctx.getQuery());
//        Arrays.asList(ctx.getParameterNames()).forEach(param -> log.error("QUERY PARAM: {}->{}", param, ctx.getValue(param)));
        return transactionTemplate.execute(status -> jdbcTemplate.queryForObject(ctx.getQuery(), ctx, Long.class));
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        return transactionTemplate.execute(status -> {
            EntityType entityType = resolveEntityType(query.getEntityFilter());
            QueryContext ctx = new QueryContext(new QuerySecurityContext(tenantId, customerId, entityType));
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


            String entityWhereClause = DefaultEntityQueryRepository.this.buildEntityWhere(ctx, query.getEntityFilter(), entityFieldsFiltersMapping);
            String latestJoins = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), entityType, allLatestMappings);
            String whereClause = DefaultEntityQueryRepository.this.buildWhere(ctx, latestFiltersMapping, query.getEntityFilter().getType());
            String textSearchQuery = DefaultEntityQueryRepository.this.buildTextSearchQuery(ctx, selectionMapping, pageLink.getTextSearch());
            String entityFieldsSelection = EntityKeyMapping.buildSelections(entityFieldsSelectionMapping, query.getEntityFilter().getType(), entityType);
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
            String latestSelection = EntityKeyMapping.buildSelections(latestSelectionMapping, query.getEntityFilter().getType(), entityType);
            String topSelection = "entities.*";
            if (!StringUtils.isEmpty(latestSelection)) {
                topSelection = topSelection + ", " + latestSelection;
            }

            String fromClause = String.format("from (select %s from (select %s from %s e where %s) entities %s %s) result %s",
                    topSelection,
                    entityFieldsSelection,
                    addEntityTableQuery(ctx, query.getEntityFilter()),
                    entityWhereClause,
                    latestJoins,
                    whereClause,
                    textSearchQuery);

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
//            log.error("QUERY: {}", dataQuery);
//            Arrays.asList(ctx.getParameterNames()).forEach(param -> log.error("QUERY PARAM: {}->{}", param, ctx.getValue(param)));
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataQuery, ctx);
            return EntityDataAdapter.createEntityData(pageLink, selectionMapping, rows, totalElements);
        });
    }

    private String buildEntityWhere(QueryContext ctx, EntityFilter entityFilter, List<EntityKeyMapping> entityFieldsFilters) {
        String permissionQuery = this.buildPermissionQuery(ctx, entityFilter);
        String entityFilterQuery = this.buildEntityFilterQuery(ctx, entityFilter);
        String entityFieldsQuery = EntityKeyMapping.buildQuery(ctx, entityFieldsFilters, entityFilter.getType());
        String result = permissionQuery;
        if (!entityFilterQuery.isEmpty()) {
            result += " and (" + entityFilterQuery + ")";
        }
        if (!entityFieldsQuery.isEmpty()) {
            result += " and (" + entityFieldsQuery + ")";
        }
        return result;
    }

    private String buildPermissionQuery(QueryContext ctx, EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case RELATIONS_QUERY:
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
            case ENTITY_VIEW_SEARCH_QUERY:
                return this.defaultPermissionQuery(ctx);
            default:
                if (ctx.getEntityType() == EntityType.TENANT) {
                    ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
                    return "e.id=:permissions_tenant_id";
                } else {
                    return this.defaultPermissionQuery(ctx);
                }
        }
    }

    private String defaultPermissionQuery(QueryContext ctx) {
        ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
        if (ctx.getCustomerId() != null && !ctx.getCustomerId().isNullUid()) {
            ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
            if (ctx.getEntityType() == EntityType.CUSTOMER) {
                return "e.tenant_id=:permissions_tenant_id and e.id=:permissions_customer_id";
            } else {
                return "e.tenant_id=:permissions_tenant_id and e.customer_id=:permissions_customer_id";
            }
        } else {
            return "e.tenant_id=:permissions_tenant_id";
        }
    }

    private String buildEntityFilterQuery(QueryContext ctx, EntityFilter entityFilter) {
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
            case ENTITY_VIEW_SEARCH_QUERY:
                return "";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String addEntityTableQuery(QueryContext ctx, EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case RELATIONS_QUERY:
                return relationQuery(ctx, (RelationsQueryFilter) entityFilter);
            case DEVICE_SEARCH_QUERY:
                DeviceSearchQueryFilter deviceQuery = (DeviceSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, deviceQuery, EntityType.DEVICE, deviceQuery.getDeviceTypes());
            case ASSET_SEARCH_QUERY:
                AssetSearchQueryFilter assetQuery = (AssetSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, assetQuery, EntityType.ASSET, assetQuery.getAssetTypes());
            case ENTITY_VIEW_SEARCH_QUERY:
                EntityViewSearchQueryFilter entityViewQuery = (EntityViewSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, entityViewQuery, EntityType.ENTITY_VIEW, entityViewQuery.getEntityViewTypes());
            default:
                return entityTableMap.get(ctx.getEntityType());
        }
    }

    private String entitySearchQuery(QueryContext ctx, EntitySearchQueryFilter entityFilter, EntityType entityType, List<String> types) {
        EntityId rootId = entityFilter.getRootEntity();
        //TODO: fetch last level only.
        //TODO: fetch distinct records.
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = "SELECT tenant_id, customer_id, id, created_time, type, name, label FROM " + entityType.name() + " WHERE id in ( SELECT entity_id";
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

    private String relationQuery(QueryContext ctx, RelationsQueryFilter entityFilter) {
        EntityId rootId = entityFilter.getRootEntity();
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = SELECT_TENANT_ID + ", " + SELECT_CUSTOMER_ID
                + ", " + SELECT_CREATED_TIME + ", " +
                " entity.entity_id as id,"
                + SELECT_TYPE + ", " + SELECT_NAME + ", " + SELECT_LABEL + ", " +
                SELECT_FIRST_NAME + ", " + SELECT_LAST_NAME + ", " + SELECT_EMAIL + ", " + SELECT_REGION + ", " +
                SELECT_TITLE + ", " + SELECT_COUNTRY + ", " + SELECT_STATE + ", " + SELECT_CITY + ", " +
                SELECT_ADDRESS + ", " + SELECT_ADDRESS_2 + ", " + SELECT_ZIP + ", " + SELECT_PHONE +
                ", entity.entity_type as entity_type";
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
                List<String> whereEntityTypes = etf.getEntityTypes().stream().map(EntityType::name).collect(Collectors.toList());
                whereFilter
                        .append(" re.relation_type = :where_relation_type").append(entityTypeFilterIdx);
                if (!whereEntityTypes.isEmpty()) {
                    whereFilter.append(" and re.")
                            .append(entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from")
                            .append("_type in (:where_entity_types").append(entityTypeFilterIdx).append(")");
                }
                if (!single) {
                    whereFilter.append(" )");
                }
                ctx.addStringParameter("where_relation_type" + entityTypeFilterIdx, relationType);
                if (!whereEntityTypes.isEmpty()) {
                    ctx.addStringListParameter("where_entity_types" + entityTypeFilterIdx, whereEntityTypes);
                }
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

    private String buildWhere(QueryContext ctx, List<EntityKeyMapping> latestFiltersMapping, EntityFilterType filterType) {
        String latestFilters = EntityKeyMapping.buildQuery(ctx, latestFiltersMapping, filterType);
        if (!StringUtils.isEmpty(latestFilters)) {
            return String.format("where %s", latestFilters);
        } else {
            return "";
        }
    }

    private String buildTextSearchQuery(QueryContext ctx, List<EntityKeyMapping> selectionMapping, String searchText) {
        if (!StringUtils.isEmpty(searchText) && !selectionMapping.isEmpty()) {
            String lowerSearchText = searchText.toLowerCase() + "%";
            List<String> searchPredicates = selectionMapping.stream().map(mapping -> {
                        String paramName = mapping.getValueAlias() + "_lowerSearchText";
                        ctx.addStringParameter(paramName, lowerSearchText);
                        return String.format("LOWER(%s) LIKE concat('%%', :%s, '%%')", mapping.getValueAlias(), paramName);
                    }
            ).collect(Collectors.toList());
            return String.format(" WHERE %s", String.join(" or ", searchPredicates));
        } else {
            return "";
        }
    }

    private String singleEntityQuery(QueryContext ctx, SingleEntityFilter filter) {
        ctx.addUuidParameter("entity_filter_single_entity_id", filter.getSingleEntity().getId());
        return "e.id=:entity_filter_single_entity_id";
    }

    private String entityListQuery(QueryContext ctx, EntityListFilter filter) {
        ctx.addUuidListParameter("entity_filter_entity_ids", filter.getEntityList().stream().map(UUID::fromString).collect(Collectors.toList()));
        return "e.id in (:entity_filter_entity_ids)";
    }

    private String entityNameQuery(QueryContext ctx, EntityNameFilter filter) {
        ctx.addStringParameter("entity_filter_name_filter", filter.getEntityNameFilter());
        return "lower(e.search_text) like lower(concat(:entity_filter_name_filter, '%%'))";
    }

    private String typeQuery(QueryContext ctx, EntityFilter filter) {
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
