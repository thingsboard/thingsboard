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
package org.thingsboard.server.dao.sql.query;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.common.data.query.AssetSearchQueryFilter;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.DeviceSearchQueryFilter;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EdgeSearchQueryFilter;
import org.thingsboard.server.common.data.query.EdgeTypeFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntitySearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.EntityViewSearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DefaultEntityQueryRepository implements EntityQueryRepository {
    private static final Map<EntityType, String> entityTableMap = new HashMap<>();
    private static final Map<EntityType, String> entityNameColumns = new HashMap<>();
    private static final String SELECT_PHONE = " CASE WHEN entity.entity_type = 'TENANT' THEN (select phone from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' THEN (select phone from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER' THEN (select phone from tb_user where id = entity_id) END as phone";
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
            " THEN (select customer_id from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select customer_id from edge where id = entity_id)" +
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
            " THEN (select tenant_id from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select tenant_id from edge where id = entity_id)" +
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
            " THEN (select created_time from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select created_time from edge where id = entity_id)" +
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
            " THEN (select name from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select name from edge where id = entity_id)" +
            " END as name";
    private static final String SELECT_TYPE = " CASE" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select authority from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select type from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select type from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select type from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select type from edge where id = entity_id)" +
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
            " THEN (select name from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select label from edge where id = entity_id)" +
            " END as label";
    private static final String SELECT_ADDITIONAL_INFO = " CASE" +
            " WHEN entity.entity_type = 'TENANT'" +
            " THEN (select additional_info from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select additional_info from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select additional_info from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'DASHBOARD'" +
            " THEN (select '' from dashboard where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select additional_info from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select additional_info from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select additional_info from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select additional_info from edge where id = entity_id)" +
            " END as additional_info";

    private static final String SELECT_RELATED_PARENT_ID = "entity.parent_id AS parent_id";

    private static final String SELECT_API_USAGE_STATE = "(select aus.id, aus.created_time, aus.tenant_id, aus.entity_id, " +
            "coalesce((select title from tenant where id = aus.entity_id), (select title from customer where id = aus.entity_id)) as name " +
            "from api_usage_state as aus)";

    static {
        entityTableMap.put(EntityType.ASSET, "asset");
        entityTableMap.put(EntityType.DEVICE, "device");
        entityTableMap.put(EntityType.ENTITY_VIEW, "entity_view");
        entityTableMap.put(EntityType.DASHBOARD, "dashboard");
        entityTableMap.put(EntityType.CUSTOMER, "customer");
        entityTableMap.put(EntityType.USER, "tb_user");
        entityTableMap.put(EntityType.TENANT, "tenant");
        entityTableMap.put(EntityType.API_USAGE_STATE, SELECT_API_USAGE_STATE);
        entityTableMap.put(EntityType.EDGE, "edge");
        entityTableMap.put(EntityType.RULE_CHAIN, "rule_chain");
        entityTableMap.put(EntityType.DEVICE_PROFILE, "device_profile");
        entityTableMap.put(EntityType.ASSET_PROFILE, "asset_profile");
        entityTableMap.put(EntityType.TENANT_PROFILE, "tenant_profile");
        entityTableMap.put(EntityType.QUEUE_STATS, "queue_stats");

        entityNameColumns.put(EntityType.DEVICE, "name");
        entityNameColumns.put(EntityType.CUSTOMER, "title");
        entityNameColumns.put(EntityType.DASHBOARD, "title");
        entityNameColumns.put(EntityType.RULE_CHAIN, "name");
        entityNameColumns.put(EntityType.RULE_NODE, "name");
        entityNameColumns.put(EntityType.OTA_PACKAGE, "title");
        entityNameColumns.put(EntityType.ASSET_PROFILE, "name");
        entityNameColumns.put(EntityType.ASSET, "name");
        entityNameColumns.put(EntityType.DEVICE_PROFILE, "name");
        entityNameColumns.put(EntityType.USER, "email");
        entityNameColumns.put(EntityType.TENANT_PROFILE, "name");
        entityNameColumns.put(EntityType.TENANT, "title");
        entityNameColumns.put(EntityType.WIDGETS_BUNDLE, "title");
        entityNameColumns.put(EntityType.ENTITY_VIEW, "name");
        entityNameColumns.put(EntityType.TB_RESOURCE, "search_text");
        entityNameColumns.put(EntityType.EDGE, "name");
        entityNameColumns.put(EntityType.QUEUE, "name");
        entityNameColumns.put(EntityType.QUEUE_STATS, "queue_name");
    }

    public static EntityType[] RELATION_QUERY_ENTITY_TYPES = new EntityType[]{
            EntityType.TENANT, EntityType.CUSTOMER, EntityType.USER, EntityType.DASHBOARD, EntityType.ASSET, EntityType.DEVICE, EntityType.ENTITY_VIEW};

    private static final String HIERARCHICAL_QUERY_TEMPLATE = " FROM (WITH RECURSIVE related_entities(from_id, from_type, to_id, to_type, lvl, path) AS (" +
            " SELECT from_id, from_type, to_id, to_type," +
            "        1 as lvl," +
            "        ARRAY[$in_id] as path" + // initial path
            " FROM relation " +
            " WHERE $in_id $rootIdCondition and $in_type = :relation_root_type and relation_type_group = 'COMMON'" +
            " GROUP BY from_id, from_type, to_id, to_type, lvl, path" +
            " UNION ALL" +
            " SELECT r.from_id, r.from_type, r.to_id, r.to_type," +
            "        (re.lvl + 1) as lvl, " +
            "        (re.path || ARRAY[r.$in_id]) as path" +
            " FROM relation r" +
            " INNER JOIN related_entities re ON" +
            " r.$in_id = re.$out_id and r.$in_type = re.$out_type and" +
            " relation_type_group = 'COMMON' " +
            " AND r.$in_id NOT IN (SELECT * FROM unnest(re.path)) " +
            " %s" +
            " GROUP BY r.from_id, r.from_type, r.to_id, r.to_type, (re.lvl + 1), (re.path || ARRAY[r.$in_id])" +
            " )" +
            " SELECT re.$out_id entity_id, re.$out_type entity_type, $parenIdExp max(r_int.lvl) lvl" +
            " from related_entities r_int" +
            "  INNER JOIN relation re ON re.from_id = r_int.from_id AND re.from_type = r_int.from_type" +
            "                         AND re.to_id = r_int.to_id AND re.to_type = r_int.to_type" +
            "                         AND re.relation_type_group = 'COMMON'" +
            " %s GROUP BY entity_id, entity_type $parenIdSelection) entity";

    private static final String HIERARCHICAL_TO_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "")
            .replace("$parenIdSelection", "")
            .replace("$in", "to").replace("$out", "from")
            .replace("$rootIdCondition", "= :relation_root_id");
    private static final String HIERARCHICAL_TO_MR_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "re.$in_id parent_id, ")
            .replace("$parenIdSelection", ", parent_id")
            .replace("$in", "to").replace("$out", "from")
            .replace("$rootIdCondition", "in (:relation_root_ids)");

    private static final String HIERARCHICAL_FROM_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "")
            .replace("$parenIdSelection", "")
            .replace("$in", "from").replace("$out", "to")
            .replace("$rootIdCondition", "= :relation_root_id");
    private static final String HIERARCHICAL_FROM_MR_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "re.$in_id parent_id, ")
            .replace("$parenIdSelection", ", parent_id")
            .replace("$in", "from").replace("$out", "to")
            .replace("$rootIdCondition", "in (:relation_root_ids)");

    @Getter
    @Value("${sql.relations.max_level:50}")
    int maxLevelAllowed; //This value has to be reasonable small to prevent infinite recursion as early as possible

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final DefaultQueryLogComponent queryLog;

    public DefaultEntityQueryRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate, DefaultQueryLogComponent queryLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.queryLog = queryLog;
    }

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        EntityType entityType = resolveEntityType(query.getEntityFilter());
        SqlQueryContext ctx = new SqlQueryContext(new QueryContext(tenantId, customerId, entityType, TenantId.SYS_TENANT_ID.equals(tenantId)));
        if (query.getKeyFilters() == null || query.getKeyFilters().isEmpty()) {
            ctx.append("select count(e.id) from ");
            ctx.append(addEntityTableQuery(ctx, query.getEntityFilter()));
            ctx.append(" e where ");
            ctx.append(buildEntityWhere(ctx, query.getEntityFilter(), Collections.emptyList()));

            return transactionTemplate.execute(status -> {
                long startTs = System.currentTimeMillis();
                try {
                    return jdbcTemplate.queryForObject(ctx.getQuery(), ctx, Long.class);
                } finally {
                    queryLog.logQuery(ctx, ctx.getQuery(), System.currentTimeMillis() - startTs);
                }
            });
        } else {
            List<EntityKeyMapping> mappings = EntityKeyMapping.prepareEntityCountKeyMapping(query);

            List<EntityKeyMapping> selectionMapping = mappings.stream().filter(EntityKeyMapping::isSelection)
                    .collect(Collectors.toList());
            List<EntityKeyMapping> entityFieldsSelectionMapping = selectionMapping.stream().filter(mapping -> !mapping.isLatest())
                    .collect(Collectors.toList());

            List<EntityKeyMapping> filterMapping = mappings.stream().filter(EntityKeyMapping::hasFilter)
                    .collect(Collectors.toList());
            List<EntityKeyMapping> entityFieldsFiltersMapping = filterMapping.stream().filter(mapping -> !mapping.isLatest() && mapping.getEntityKeyColumn() != null)
                    .collect(Collectors.toList());

            List<EntityKeyMapping> allLatestMappings = mappings.stream().filter(EntityKeyMapping::isLatest)
                    .collect(Collectors.toList());


            String entityWhereClause = DefaultEntityQueryRepository.this.buildEntityWhere(ctx, query.getEntityFilter(), entityFieldsFiltersMapping);
            String aliasWhereQuery = DefaultEntityQueryRepository.this.buildAliasWhereQuery(ctx, query.getEntityFilter(), selectionMapping, "");
            String latestJoinsCnt = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), entityType, allLatestMappings, true);
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

            String fromClauseCount = String.format("from (select %s from (select %s from %s e where %s) entities %s ) result %s",
                    "entities.*",
                    entityFieldsSelection,
                    addEntityTableQuery(ctx, query.getEntityFilter()),
                    entityWhereClause,
                    latestJoinsCnt,
                    aliasWhereQuery);

            String countQuery = String.format("select count(id) %s", fromClauseCount);

            return transactionTemplate.execute(status -> {
                long startTs = System.currentTimeMillis();
                try {
                    return jdbcTemplate.queryForObject(countQuery, ctx, Long.class);
                } finally {
                    queryLog.logQuery(ctx, countQuery, System.currentTimeMillis() - startTs);
                }
            });
        }
    }

    @Override
    public PageData<EntityData> findEntityDataByQueryInternal(EntityDataQuery query) {
        return findEntityDataByQuery(null, null, query, true);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        return findEntityDataByQuery(tenantId, customerId, query, false);
    }

    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query, boolean ignorePermissionCheck) {
        return transactionTemplate.execute(status -> {
            EntityType entityType = resolveEntityType(query.getEntityFilter());
            SqlQueryContext ctx = new SqlQueryContext(new QueryContext(tenantId, customerId, entityType, ignorePermissionCheck));
            EntityDataPageLink pageLink = query.getPageLink();

            List<EntityKeyMapping> mappings = EntityKeyMapping.prepareKeyMapping(entityType, query);

            List<EntityKeyMapping> selectionMapping = mappings.stream().filter(EntityKeyMapping::isSelection)
                    .collect(Collectors.toList());
            List<EntityKeyMapping> entityFieldsSelectionMapping = selectionMapping.stream().filter(mapping -> !mapping.isLatest())
                    .collect(Collectors.toList());
            List<EntityKeyMapping> latestSelectionMapping = selectionMapping.stream().filter(EntityKeyMapping::isLatest)
                    .collect(Collectors.toList());

            List<EntityKeyMapping> filterMapping = mappings.stream().filter(EntityKeyMapping::hasFilter)
                    .collect(Collectors.toList());
            List<EntityKeyMapping> entityFieldsFiltersMapping = filterMapping.stream().filter(mapping -> !mapping.isLatest() && mapping.getEntityKeyColumn() != null)
                    .collect(Collectors.toList());

            List<EntityKeyMapping> allLatestMappings = mappings.stream().filter(EntityKeyMapping::isLatest)
                    .collect(Collectors.toList());


            String entityWhereClause = DefaultEntityQueryRepository.this.buildEntityWhere(ctx, query.getEntityFilter(), entityFieldsFiltersMapping);
            String latestJoinsCnt = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), entityType, allLatestMappings, true);
            String latestJoinsData = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), entityType, allLatestMappings, false);
            String aliasWhereQuery = DefaultEntityQueryRepository.this.buildAliasWhereQuery(ctx, query.getEntityFilter(), selectionMapping, pageLink.getTextSearch());
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

            String fromClauseCount = String.format("from (select %s from (select %s from %s e where %s) entities %s ) result %s",
                    "entities.*",
                    entityFieldsSelection,
                    addEntityTableQuery(ctx, query.getEntityFilter()),
                    entityWhereClause,
                    latestJoinsCnt,
                    aliasWhereQuery);

            String fromClauseData = String.format("from (select %s from (select %s from %s e where %s) entities %s ) result %s",
                    topSelection,
                    entityFieldsSelection,
                    addEntityTableQuery(ctx, query.getEntityFilter()),
                    entityWhereClause,
                    latestJoinsData,
                    aliasWhereQuery);

            if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
                //Unfortunately, we need to sacrifice performance in case of full text search, because it is applied to all joined records.
                fromClauseCount = fromClauseData;
            }
            String countQuery = String.format("select count(id) %s", fromClauseCount);

            long startTs = System.currentTimeMillis();
            int totalElements;
            try {
                totalElements = jdbcTemplate.queryForObject(countQuery, ctx, Integer.class);
            } finally {
                queryLog.logQuery(ctx, countQuery, System.currentTimeMillis() - startTs);
            }

            if (totalElements == 0) {
                return new PageData<>();
            }
            String dataQuery = String.format("select * %s", fromClauseData);

            EntityDataSortOrder sortOrder = pageLink.getSortOrder();
            if (sortOrder != null) {
                Optional<EntityKeyMapping> sortOrderMappingOpt = mappings.stream().filter(EntityKeyMapping::isSortOrder).findFirst();
                if (sortOrderMappingOpt.isPresent()) {
                    EntityKeyMapping sortOrderMapping = sortOrderMappingOpt.get();
                    String direction = sortOrder.getDirection() == EntityDataSortOrder.Direction.ASC ? "asc" : "desc";
                    if (sortOrderMapping.getEntityKey().getType() == EntityKeyType.ENTITY_FIELD) {
                        dataQuery = String.format("%s order by %s %s, result.id %s", dataQuery, sortOrderMapping.getValueAlias(), direction, direction);
                    } else {
                        dataQuery = String.format("%s order by %s %s, %s %s, result.id %s", dataQuery,
                                sortOrderMapping.getSortOrderNumAlias(), direction, sortOrderMapping.getSortOrderStrAlias(), direction, direction);
                    }
                }
            }
            int startIndex = pageLink.getPageSize() * pageLink.getPage();
            if (pageLink.getPageSize() > 0) {
                dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
            }
            startTs = System.currentTimeMillis();
            List<Map<String, Object>> rows;
            try {
                rows = jdbcTemplate.queryForList(dataQuery, ctx);
            } finally {
                queryLog.logQuery(ctx, dataQuery, System.currentTimeMillis() - startTs);
            }
            return EntityDataAdapter.createEntityData(pageLink, selectionMapping, rows, totalElements);
        });
    }

    private String buildEntityWhere(SqlQueryContext ctx, EntityFilter entityFilter, List<EntityKeyMapping> entityFieldsFilters) {
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

    private String buildPermissionQuery(SqlQueryContext ctx, EntityFilter entityFilter) {
        if (ctx.isIgnorePermissionCheck() || (ctx.getTenantId().isSysTenantId() &&
                (ctx.getEntityType() == EntityType.TENANT || ctx.getEntityType() == EntityType.TENANT_PROFILE))) {
            return "1=1";
        }
        switch (entityFilter.getType()) {
            case RELATIONS_QUERY:
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
            case ENTITY_VIEW_SEARCH_QUERY:
            case EDGE_SEARCH_QUERY:
                return this.defaultPermissionQuery(ctx);
            case API_USAGE_STATE:
                CustomerId filterCustomerId = ((ApiUsageStateFilter) entityFilter).getCustomerId();
                if (ctx.getCustomerId() != null && !ctx.getCustomerId().isNullUid()) {
                    if (filterCustomerId != null && !filterCustomerId.equals(ctx.getCustomerId())) {
                        throw new SecurityException("Customer is not allowed to query other customer's data");
                    }
                    filterCustomerId = ctx.getCustomerId();
                }

                ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
                if (filterCustomerId != null) {
                    ctx.addUuidParameter("permissions_customer_id", filterCustomerId.getId());
                    return "e.tenant_id=:permissions_tenant_id and e.entity_id=:permissions_customer_id";
                } else {
                    return "e.tenant_id=:permissions_tenant_id and e.entity_id=:permissions_tenant_id";
                }
            default:
                if (ctx.getEntityType() == EntityType.TENANT) {
                    ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
                    return "e.id=:permissions_tenant_id";
                } else {
                    return this.defaultPermissionQuery(ctx);
                }
        }
    }

    private String defaultPermissionQuery(SqlQueryContext ctx) {
        ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
        if (ctx.getCustomerId() != null && !ctx.getCustomerId().isNullUid()) {
            ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
            if (ctx.getEntityType() == EntityType.CUSTOMER) {
                return "e.tenant_id=:permissions_tenant_id and e.id=:permissions_customer_id";
            } else if (ctx.getEntityType() == EntityType.API_USAGE_STATE) {
                return "e.tenant_id=:permissions_tenant_id and e.entity_id=:permissions_customer_id";
            } else if (ctx.getEntityType() == EntityType.DASHBOARD) {
                return "e.tenant_id=:permissions_tenant_id and e.assigned_customers like concat('%', :permissions_customer_id, '%')";
            } else {
                return "e.tenant_id=:permissions_tenant_id and e.customer_id=:permissions_customer_id";
            }
        } else {
            return "e.tenant_id=:permissions_tenant_id";
        }
    }

    private String buildEntityFilterQuery(SqlQueryContext ctx, EntityFilter entityFilter) {
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
            case EDGE_TYPE:
                return this.typeQuery(ctx, entityFilter);
            case RELATIONS_QUERY:
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
            case ENTITY_VIEW_SEARCH_QUERY:
            case EDGE_SEARCH_QUERY:
            case API_USAGE_STATE:
            case ENTITY_TYPE:
                return "";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String addEntityTableQuery(SqlQueryContext ctx, EntityFilter entityFilter) {
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
            case EDGE_SEARCH_QUERY:
                EdgeSearchQueryFilter edgeQuery = (EdgeSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, edgeQuery, EntityType.EDGE, edgeQuery.getEdgeTypes());
            default:
                return entityTableMap.get(ctx.getEntityType());
        }
    }

    private String entitySearchQuery(SqlQueryContext ctx, EntitySearchQueryFilter entityFilter, EntityType entityType, List<String> types) {
        EntityId rootId = entityFilter.getRootEntity();
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = "SELECT tenant_id, customer_id, id, created_time, type, name, additional_info "
                + (entityType.equals(EntityType.ENTITY_VIEW) ? "" : ", label ")
                + "FROM " + entityType.name() + " WHERE id in ( SELECT entity_id";
        String from = getQueryTemplate(entityFilter.getDirection(), false);
        String whereFilter = " WHERE";
        if (!StringUtils.isEmpty(entityFilter.getRelationType())) {
            ctx.addStringParameter("where_relation_type", entityFilter.getRelationType());
            whereFilter += " re.relation_type = :where_relation_type AND";
        }
        String toOrFrom = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from");
        whereFilter += " re." + (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from") + "_type = :where_entity_type";
        if (entityFilter.isFetchLastLevelOnly()) {
            String fromOrTo = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "from" : "to");
            StringBuilder notExistsPart = new StringBuilder();
            notExistsPart.append(" NOT EXISTS (SELECT 1 from relation nr ")
                    .append(whereFilter.replaceAll("re\\.", "nr\\."))
                    .append(" and ")
                    .append("nr.").append(fromOrTo).append("_id").append(" = re.").append(toOrFrom).append("_id")
                    .append(" and ")
                    .append("nr.").append(fromOrTo).append("_type").append(" = re.").append(toOrFrom).append("_type");
            notExistsPart.append(" and nr.relation_type_group = 'COMMON'"); // hit the index, the same condition are on the recursive query
            notExistsPart.append(")");
            whereFilter += " and ( r_int.lvl = " + entityFilter.getMaxLevel() + " OR " + notExistsPart.toString() + ")";
        }
        from = String.format(from, lvlFilter, whereFilter);
        String query = "( " + selectFields + from + ")";
        if (types != null && !types.isEmpty()) {
            query += " and type in (:relation_sub_types)";
            ctx.addStringListParameter("relation_sub_types", types);
        }
        query += " )";
        ctx.addUuidParameter("relation_root_id", rootId.getId());
        ctx.addStringParameter("relation_root_type", rootId.getEntityType().name());
        ctx.addStringParameter("where_entity_type", entityType.name());
        return query;
    }

    private String relationQuery(SqlQueryContext ctx, RelationsQueryFilter entityFilter) {
        EntityId rootId = entityFilter.getRootEntity();
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = SELECT_TENANT_ID + ", " + SELECT_CUSTOMER_ID
                + ", " + SELECT_CREATED_TIME + ", " +
                " entity.entity_id as id,"
                + SELECT_TYPE + ", " + SELECT_NAME + ", " + SELECT_LABEL + ", " +
                SELECT_FIRST_NAME + ", " + SELECT_LAST_NAME + ", " + SELECT_EMAIL + ", " + SELECT_REGION + ", " +
                SELECT_TITLE + ", " + SELECT_COUNTRY + ", " + SELECT_STATE + ", " + SELECT_CITY + ", " +
                SELECT_ADDRESS + ", " + SELECT_ADDRESS_2 + ", " + SELECT_ZIP + ", " + SELECT_PHONE + ", " +
                SELECT_ADDITIONAL_INFO + (entityFilter.isMultiRoot() ? (", " + SELECT_RELATED_PARENT_ID) : "") +
                ", entity.entity_type as entity_type";
        /*
        * FIXME:
        *  target entities are duplicated in result list, if search direction is TO and multiple relations are references to target entity
        * */
        String from = getQueryTemplate(entityFilter.getDirection(), entityFilter.isMultiRoot());

        if (entityFilter.isMultiRoot()) {
            ctx.addUuidListParameter("relation_root_ids", entityFilter.getMultiRootEntityIds().stream().map(UUID::fromString).collect(Collectors.toList()));
            ctx.addStringParameter("relation_root_type", entityFilter.getMultiRootEntitiesType().name());
        } else {
            ctx.addUuidParameter("relation_root_id", rootId.getId());
            ctx.addStringParameter("relation_root_type", rootId.getEntityType().name());
        }

        StringBuilder whereFilter = new StringBuilder();

        boolean noConditions = true;
        boolean single = entityFilter.getFilters() != null && entityFilter.getFilters().size() == 1;
        if (entityFilter.getFilters() != null && !entityFilter.getFilters().isEmpty()) {
            int entityTypeFilterIdx = 0;
            for (RelationEntityTypeFilter etf : entityFilter.getFilters()) {
                String etfCondition = buildEtfCondition(ctx, etf, entityFilter.getDirection(), entityTypeFilterIdx++);
                if (!etfCondition.isEmpty()) {
                    if (noConditions) {
                        noConditions = false;
                    } else {
                        whereFilter.append(" OR ");
                    }
                    if (!single) {
                        whereFilter.append(" (");
                    }
                    whereFilter.append(etfCondition);
                    if (!single) {
                        whereFilter.append(" )");
                    }
                }
            }
        }
        if (noConditions) {
            whereFilter.append(" re.")
                    .append(entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from")
                    .append("_type in (:where_entity_types").append(")");
            ctx.addStringListParameter("where_entity_types", Arrays.stream(RELATION_QUERY_ENTITY_TYPES).map(EntityType::name).collect(Collectors.toList()));
        } else {
            if (!single) {
                whereFilter = new StringBuilder()
                        .append(entityFilter.isNegate() ? " NOT (" : "(")
                        .append(whereFilter).append(")");
            } else if (entityFilter.isNegate()) {
                whereFilter = new StringBuilder()
                        .append(" NOT (")
                        .append(whereFilter).append(")");
            }
        }

        if (entityFilter.isFetchLastLevelOnly()) {
            String toOrFrom = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from");
            String fromOrTo = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "from" : "to");

            StringBuilder notExistsPart = new StringBuilder();
            notExistsPart.append(" NOT EXISTS (SELECT 1 from relation nr WHERE ");
            notExistsPart
                    .append("nr.").append(fromOrTo).append("_id").append(" = re.").append(toOrFrom).append("_id")
                    .append(" and ")
                    .append("nr.").append(fromOrTo).append("_type").append(" = re.").append(toOrFrom).append("_type")
                    .append(" and ")
                    .append(whereFilter.toString().replaceAll("re\\.", "nr\\."));
            notExistsPart.append(" and nr.relation_type_group = 'COMMON'"); // hit the index, the same condition are on the recursive query
            notExistsPart.append(")");
            whereFilter.append(" and ( r_int.lvl = ").append(entityFilter.getMaxLevel()).append(" OR ").append(notExistsPart.toString()).append(")");
        }
        from = String.format(from, lvlFilter, " WHERE " + whereFilter);
        return "( " + selectFields + from + ")";
    }

    private String buildEtfCondition(SqlQueryContext ctx, RelationEntityTypeFilter etf, EntitySearchDirection direction, int entityTypeFilterIdx) {
        StringBuilder whereFilter = new StringBuilder();
        String relationType = etf.getRelationType();
        List<EntityType> entityTypes = etf.getEntityTypes();
        List<String> whereEntityTypes;
        if (entityTypes == null || entityTypes.isEmpty()) {
            whereEntityTypes = Collections.emptyList();
        } else {
            whereEntityTypes = etf.getEntityTypes().stream().map(EntityType::name).collect(Collectors.toList());
        }
        boolean hasRelationType = !StringUtils.isEmpty(relationType);
        if (hasRelationType) {
            ctx.addStringParameter("where_relation_type" + entityTypeFilterIdx, relationType);
            if (etf.isNegate()) {
                whereFilter.append("re.relation_type != :where_relation_type").append(entityTypeFilterIdx);
            } else {
                whereFilter.append("re.relation_type = :where_relation_type").append(entityTypeFilterIdx);
            }
        }
        if (!whereEntityTypes.isEmpty()) {
            if (hasRelationType) {
                whereFilter.append(" and ");
            }
            whereFilter.append("re.")
                    .append(direction.equals(EntitySearchDirection.FROM) ? "to" : "from")
                    .append("_type in (:where_entity_types").append(entityTypeFilterIdx).append(")");
            ctx.addStringListParameter("where_entity_types" + entityTypeFilterIdx, whereEntityTypes);
        }
        return whereFilter.toString();
    }

    String getLvlFilter(int maxLevel) {
        return "and re.lvl <= " + (getMaxLevel(maxLevel) - 1);
    }

    int getMaxLevel(int maxLevel) {
        return (maxLevel <= 0 || maxLevel > this.maxLevelAllowed) ? this.maxLevelAllowed : maxLevel;
    }

    private String getQueryTemplate(EntitySearchDirection direction, boolean isMultiRoot) {
        String from;
        if (direction.equals(EntitySearchDirection.FROM)) {
            from = isMultiRoot ? HIERARCHICAL_FROM_MR_QUERY_TEMPLATE : HIERARCHICAL_FROM_QUERY_TEMPLATE;
        } else {
            from = isMultiRoot ? HIERARCHICAL_TO_MR_QUERY_TEMPLATE : HIERARCHICAL_TO_QUERY_TEMPLATE;
        }
        return from;
    }

    private String buildAliasWhereQuery(SqlQueryContext ctx, EntityFilter entityFilter, List<EntityKeyMapping> selectionMapping, String searchText) {
        List<EntityKeyMapping> aliasFiltersMapping = selectionMapping.stream().filter(mapping -> !mapping.isLatest() && mapping.getEntityKeyColumn() == null)
                .collect(Collectors.toList());
        String entityFieldsQuery = EntityKeyMapping.buildQuery(ctx, aliasFiltersMapping, entityFilter.getType());
        String searchTextQuery = buildTextSearchQuery(ctx, selectionMapping, searchText);
        String result = "";
        if (!entityFieldsQuery.isEmpty()) {
            result += " where (" + entityFieldsQuery + ")";
        }
        if (!searchTextQuery.isEmpty()) {
            result += (result.isEmpty() ? " where " : " and ") + "(" + searchTextQuery + ") ";
        }
        return result;
    }

    private String buildTextSearchQuery(SqlQueryContext ctx, List<EntityKeyMapping> selectionMapping, String searchText) {
        if (!StringUtils.isEmpty(searchText) && !selectionMapping.isEmpty()) {
            String sqlSearchText = "%" + searchText + "%";
            ctx.addStringParameter("lowerSearchTextParam", sqlSearchText);
            List<String> searchAliases = selectionMapping.stream().filter(EntityKeyMapping::isSearchable).map(EntityKeyMapping::getValueAlias).collect(Collectors.toList());
            String searchAliasesExpression;
            if (searchAliases.size() > 1) {
                searchAliasesExpression = "CONCAT(" + String.join(" , ", searchAliases) + ")";
            } else {
                searchAliasesExpression = searchAliases.get(0);
            }
            return String.format(" %s ILIKE :%s", searchAliasesExpression, "lowerSearchTextParam");
        } else {
            return "";
        }
    }

    private String singleEntityQuery(SqlQueryContext ctx, SingleEntityFilter filter) {
        ctx.addUuidParameter("entity_filter_single_entity_id", filter.getSingleEntity().getId());
        return "e.id=:entity_filter_single_entity_id";
    }

    private String entityListQuery(SqlQueryContext ctx, EntityListFilter filter) {
        ctx.addUuidListParameter("entity_filter_entity_ids", filter.getEntityList().stream().map(UUID::fromString).collect(Collectors.toList()));
        return "e.id in (:entity_filter_entity_ids)";
    }

    private String entityNameQuery(SqlQueryContext ctx, EntityNameFilter filter) {
        ctx.addStringParameter("entity_filter_name_filter", filter.getEntityNameFilter());
        String nameColumn = getNameColumn(filter.getEntityType());
        if (filter.getEntityNameFilter().startsWith("%") || filter.getEntityNameFilter().endsWith("%")) {
            return String.format("e.%s ilike :entity_filter_name_filter", nameColumn);
        }

        return String.format("e.%s ilike concat(:entity_filter_name_filter, '%%')", nameColumn);
    }

    private String typeQuery(SqlQueryContext ctx, EntityFilter filter) {
        List<String> types;
        String name;
        String nameColumn;
        switch (filter.getType()) {
            case ASSET_TYPE:
                types = ((AssetTypeFilter) filter).getAssetTypes();
                name = ((AssetTypeFilter) filter).getAssetNameFilter();
                nameColumn = getNameColumn(EntityType.ASSET);
                break;
            case DEVICE_TYPE:
                types = ((DeviceTypeFilter) filter).getDeviceTypes();
                name = ((DeviceTypeFilter) filter).getDeviceNameFilter();
                nameColumn = getNameColumn(EntityType.DEVICE);
                break;
            case ENTITY_VIEW_TYPE:
                types = ((EntityViewTypeFilter) filter).getEntityViewTypes();
                name = ((EntityViewTypeFilter) filter).getEntityViewNameFilter();
                nameColumn = getNameColumn(EntityType.ENTITY_VIEW);
                break;
            case EDGE_TYPE:
                types = ((EdgeTypeFilter) filter).getEdgeTypes();
                name = ((EdgeTypeFilter) filter).getEdgeNameFilter();
                nameColumn = getNameColumn(EntityType.EDGE);
                break;
            default:
                throw new RuntimeException("Not supported!");
        }
        String typesFilter = "e.type in (:entity_filter_type_query_types)";
        ctx.addStringListParameter("entity_filter_type_query_types", types);
        if (!StringUtils.isEmpty(name)) {
            ctx.addStringParameter("entity_filter_type_query_name", name);
            if (name.startsWith("%") || name.endsWith("%")) {
                return typesFilter + " and e." + nameColumn + " ilike :entity_filter_type_query_name";
            }
            return typesFilter + " and e." + nameColumn + " ilike concat(:entity_filter_type_query_name, '%%')";
        } else {
            return typesFilter;
        }
    }

    private String getNameColumn(EntityType entityType) {
        String nameColumn = entityNameColumns.get(entityType);
        if (nameColumn == null) {
            log.error("Name column is not defined in the entityNameColumns map for entity type {}.", entityType);
            throw new RuntimeException("Name column is not defined for entity type: " + entityType);
        }
        return nameColumn;
    }

    public static EntityType resolveEntityType(EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return ((SingleEntityFilter) entityFilter).getSingleEntity().getEntityType();
            case ENTITY_LIST:
                return ((EntityListFilter) entityFilter).getEntityType();
            case ENTITY_NAME:
                return ((EntityNameFilter) entityFilter).getEntityType();
            case ENTITY_TYPE:
                return ((EntityTypeFilter) entityFilter).getEntityType();
            case ASSET_TYPE:
            case ASSET_SEARCH_QUERY:
                return EntityType.ASSET;
            case DEVICE_TYPE:
            case DEVICE_SEARCH_QUERY:
                return EntityType.DEVICE;
            case ENTITY_VIEW_TYPE:
            case ENTITY_VIEW_SEARCH_QUERY:
                return EntityType.ENTITY_VIEW;
            case EDGE_TYPE:
            case EDGE_SEARCH_QUERY:
                return EntityType.EDGE;
            case RELATIONS_QUERY:
                RelationsQueryFilter rgf = (RelationsQueryFilter) entityFilter;
                return rgf.isMultiRoot() ? rgf.getMultiRootEntitiesType() : rgf.getRootEntity().getEntityType();
            case API_USAGE_STATE:
                return EntityType.API_USAGE_STATE;
            default:
                throw new RuntimeException("Not implemented!");
        }
    }
}
