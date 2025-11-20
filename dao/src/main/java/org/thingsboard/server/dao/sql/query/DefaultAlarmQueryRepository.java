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

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DefaultAlarmQueryRepository implements AlarmQueryRepository {

    private static final Map<String, String> alarmFieldColumnMap = new HashMap<>();

    private static final String ASSIGNEE_EMAIL_KEY = "assigneeEmail";
    private static final String ASSIGNEE_LAST_NAME_KEY = "assigneeLastName";
    private static final String ASSIGNEE_FIRST_NAME_KEY = "assigneeFirstName";
    private static final String ASSIGNEE_ID_KEY = "assigneeId";
    private static final String ASSIGNEE_KEY = "assignee";

    static {
        alarmFieldColumnMap.put("createdTime", ModelConstants.CREATED_TIME_PROPERTY);
        alarmFieldColumnMap.put("ackTs", ModelConstants.ALARM_ACK_TS_PROPERTY);
        alarmFieldColumnMap.put("ackTime", ModelConstants.ALARM_ACK_TS_PROPERTY);
        alarmFieldColumnMap.put("clearTs", ModelConstants.ALARM_CLEAR_TS_PROPERTY);
        alarmFieldColumnMap.put("clearTime", ModelConstants.ALARM_CLEAR_TS_PROPERTY);
        alarmFieldColumnMap.put("assignTime", ModelConstants.ALARM_ASSIGN_TS_PROPERTY);
        alarmFieldColumnMap.put("details", ModelConstants.ADDITIONAL_INFO_PROPERTY);
        alarmFieldColumnMap.put("endTs", ModelConstants.ALARM_END_TS_PROPERTY);
        alarmFieldColumnMap.put("endTime", ModelConstants.ALARM_END_TS_PROPERTY);
        alarmFieldColumnMap.put("startTs", ModelConstants.ALARM_START_TS_PROPERTY);
        alarmFieldColumnMap.put("startTime", ModelConstants.ALARM_START_TS_PROPERTY);
        alarmFieldColumnMap.put("acknowledged", ModelConstants.ALARM_ACKNOWLEDGED_PROPERTY);
        alarmFieldColumnMap.put("cleared", ModelConstants.ALARM_CLEARED_PROPERTY);
        alarmFieldColumnMap.put("type", ModelConstants.ALARM_TYPE_PROPERTY);
        alarmFieldColumnMap.put("severity", ModelConstants.ALARM_SEVERITY_PROPERTY);
        alarmFieldColumnMap.put("originatorId", ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY);
        alarmFieldColumnMap.put("originatorType", ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY);
        alarmFieldColumnMap.put(ASSIGNEE_ID_KEY, ModelConstants.ALARM_ASSIGNEE_ID_PROPERTY);
        alarmFieldColumnMap.put("originator", ModelConstants.ALARM_ORIGINATOR_NAME_PROPERTY);
        alarmFieldColumnMap.put("originatorLabel", ModelConstants.ALARM_ORIGINATOR_LABEL_PROPERTY);
        alarmFieldColumnMap.put(ASSIGNEE_FIRST_NAME_KEY, ModelConstants.ALARM_ASSIGNEE_FIRST_NAME_PROPERTY);
        alarmFieldColumnMap.put(ASSIGNEE_LAST_NAME_KEY, ModelConstants.ALARM_ASSIGNEE_LAST_NAME_PROPERTY);
        alarmFieldColumnMap.put(ASSIGNEE_EMAIL_KEY, ModelConstants.ALARM_ASSIGNEE_EMAIL_PROPERTY);
    }

    private static final String FIELDS_SELECTION = "select a.id as id," +
            " a.created_time as created_time," +
            " a.ack_ts as ack_ts," +
            " a.clear_ts as clear_ts," +
            " a.assign_ts as assign_ts," +
            " a.assignee_id as assignee_id," +
            " a.additional_info as additional_info," +
            " a.end_ts as end_ts," +
            " a.originator_id as originator_id," +
            " a.originator_type as originator_type," +
            " a.propagate as propagate," +
            " a.propagate_to_owner as propagate_to_owner," +
            " a.propagate_to_tenant as propagate_to_tenant," +
            " a.severity as severity," +
            " a.start_ts as start_ts," +
            " a.tenant_id as tenant_id, " +
            " a.customer_id as customer_id, " +
            " a.propagate_relation_types as propagate_relation_types, " +
            " a.type as type, " +
            " a.originator_name as originator_name, " +
            " a.originator_label as originator_label, " +
            " a.assignee_first_name as assignee_first_name, " +
            " a.assignee_last_name as assignee_last_name, " +
            " a.assignee_email as assignee_email, " +
            " a.cleared as cleared, " +
            " a.acknowledged as acknowledged, ";

    private static final String JOIN_ENTITY_ALARMS = "inner join entity_alarm ea on a.id = ea.alarm_id ";

    protected final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    private final DefaultQueryLogComponent queryLog;

    public DefaultAlarmQueryRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate, DefaultQueryLogComponent queryLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.queryLog = queryLog;
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        return transactionTemplate.execute(trStatus -> {
            AlarmDataPageLink pageLink = query.getPageLink();
            SqlQueryContext ctx = new SqlQueryContext(new QueryContext(tenantId, null, EntityType.ALARM));
            ctx.addUuidListParameter("entity_ids", orderedEntityIds.stream().map(EntityId::getId).collect(Collectors.toList()));
            StringBuilder selectPart = new StringBuilder(FIELDS_SELECTION);
            StringBuilder fromPart = new StringBuilder(" from alarm_info a ");
            StringBuilder wherePart = new StringBuilder(" where ");
            StringBuilder sortPart = new StringBuilder(" order by ");
            StringBuilder joinPart = new StringBuilder();
            boolean addAnd = false;
            if (pageLink.isSearchPropagatedAlarms()) {
                selectPart.append(" ea.entity_id as entity_id ");
                fromPart.append(JOIN_ENTITY_ALARMS);
                wherePart.append(buildPermissionsQuery(tenantId, ctx));
                addAnd = true;
            } else {
                selectPart.append(" a.originator_id as entity_id ");
            }
            EntityDataSortOrder sortOrder = pageLink.getSortOrder();

            if (sortOrder != null && EntityKeyType.ALARM_FIELD.equals(sortOrder.getKey().getType()) && ASSIGNEE_KEY.equalsIgnoreCase(sortOrder.getKey().getKey())) {
                sortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, ASSIGNEE_EMAIL_KEY), sortOrder.getDirection());
            }

            List<EntityKey> alarmFields = new ArrayList<>();
            for (EntityKey key : query.getAlarmFields()) {
                if (EntityKeyType.ALARM_FIELD.equals(key.getType()) && ASSIGNEE_KEY.equalsIgnoreCase(key.getKey())) {
                    alarmFields.add(new EntityKey(EntityKeyType.ALARM_FIELD, ASSIGNEE_ID_KEY));
                    alarmFields.add(new EntityKey(EntityKeyType.ALARM_FIELD, ASSIGNEE_FIRST_NAME_KEY));
                    alarmFields.add(new EntityKey(EntityKeyType.ALARM_FIELD, ASSIGNEE_LAST_NAME_KEY));
                    alarmFields.add(new EntityKey(EntityKeyType.ALARM_FIELD, ASSIGNEE_EMAIL_KEY));
                } else {
                    alarmFields.add(key);
                }
            }

            String textSearchQuery = buildTextSearchQuery(ctx, alarmFields, pageLink.getTextSearch());
            if (sortOrder != null && sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
                String sortOrderKey = sortOrder.getKey().getKey();
                if ("status".equalsIgnoreCase(sortOrderKey)) {
                    selectPart.append(", a.status as status ");
                }
                sortPart.append(alarmFieldColumnMap.getOrDefault(sortOrderKey, sortOrderKey))
                        .append(" ").append(sortOrder.getDirection().name());
                if (pageLink.isSearchPropagatedAlarms()) {
                    wherePart.append(" and ea.entity_id in (:entity_ids)");
                } else {
                    addAndIfNeeded(wherePart, addAnd);
                    addAnd = true;
                    wherePart.append(" a.originator_id in (:entity_ids)");
                }
            } else {
                joinPart.append(" inner join (select * from (VALUES");
                int entityIdIdx = 0;
                int lastEntityIdIdx = orderedEntityIds.size() - 1;
                for (EntityId entityId : orderedEntityIds) {
                    joinPart.append("(uuid('").append(entityId.getId().toString()).append("'), ").append(entityIdIdx).append(")");
                    if (entityIdIdx != lastEntityIdIdx) {
                        joinPart.append(",");
                    } else {
                        joinPart.append(")");
                    }
                    entityIdIdx++;
                }
                joinPart.append(" as e(id, priority)) e ");
                if (pageLink.isSearchPropagatedAlarms()) {
                    if (textSearchQuery.isEmpty()) {
                        joinPart.append("on ea.entity_id = e.id");
                    } else {
                        joinPart.append("on a.entity_id = e.id");
                    }
                } else {
                    joinPart.append("on a.originator_id = e.id");
                }
                sortPart.append("e.priority");
            }

            long startTs;
            long endTs;
            if (pageLink.getTimeWindow() > 0) {
                if (pageLink.getStartTs() > 0) {
                    startTs = pageLink.getStartTs();
                    endTs = startTs + pageLink.getTimeWindow();
                } else {
                    endTs = System.currentTimeMillis();
                    startTs = endTs - pageLink.getTimeWindow();
                }
            } else {
                startTs = pageLink.getStartTs();
                endTs = pageLink.getEndTs();
            }

            if (startTs > 0) {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                ctx.addLongParameter("startTime", startTs);
                wherePart.append("a.created_time >= :startTime");
                if (pageLink.isSearchPropagatedAlarms()) {
                    wherePart.append(" and ea.created_time >= :startTime");
                }
            }

            if (endTs > 0) {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                ctx.addLongParameter("endTime", endTs);
                wherePart.append("a.created_time <= :endTime");
                if (pageLink.isSearchPropagatedAlarms()) {
                    wherePart.append(" and ea.created_time <= :endTime");
                }
            }

            if (pageLink.getTypeList() != null && !pageLink.getTypeList().isEmpty()) {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                ctx.addStringListParameter("alarmTypes", pageLink.getTypeList());
                wherePart.append("a.type in (:alarmTypes)");
                if (pageLink.isSearchPropagatedAlarms()) {
                    wherePart.append(" and ea.alarm_type in (:alarmTypes)");
                }
            }

            if (pageLink.getSeverityList() != null && !pageLink.getSeverityList().isEmpty()) {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                ctx.addStringListParameter("alarmSeverities", pageLink.getSeverityList().stream().map(AlarmSeverity::name).collect(Collectors.toList()));
                wherePart.append("a.severity in (:alarmSeverities)");
            }

            AlarmStatusFilter asf = AlarmStatusFilter.from(pageLink.getStatusList());
            if (asf.hasAnyFilter()) {
                if (asf.hasAckFilter()) {
                    addAndIfNeeded(wherePart, addAnd);
                    addAnd = true;
                    ctx.addBooleanParameter("ackStatus", asf.getAckFilter());
                    wherePart.append(" a.acknowledged = :ackStatus");
                }
                if (asf.hasClearFilter()) {
                    addAndIfNeeded(wherePart, addAnd);
                    // addAnd = true; // not needed but stored as an example if someone adds new conditions
                    ctx.addBooleanParameter("clearStatus", asf.getClearFilter());
                    wherePart.append(" a.cleared = :clearStatus");
                }
            }

            if (pageLink.getAssigneeId() != null) {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                ctx.addUuidParameter("assigneeId", pageLink.getAssigneeId().getId());
                wherePart.append(" a.assignee_id = :assigneeId");
            }

            String mainQuery = String.format("%s%s", selectPart, fromPart);
            if (textSearchQuery.isEmpty()) {
                mainQuery = String.format("%s%s%s", mainQuery, joinPart, wherePart);
            } else {
                mainQuery = String.format("select * from (%s%s) a %s WHERE %s", mainQuery, wherePart, joinPart, textSearchQuery);
            }
            String countQuery = String.format("select count(*) from (%s) result", mainQuery);
            long queryTs = System.currentTimeMillis();
            int totalElements;
            try {
                totalElements = jdbcTemplate.queryForObject(countQuery, ctx, Integer.class);
            } finally {
                queryLog.logQuery(ctx, countQuery, System.currentTimeMillis() - queryTs);
            }
            if (totalElements == 0) {
                return AlarmDataAdapter.createAlarmData(pageLink, Collections.emptyList(), totalElements, orderedEntityIds);
            }

            String dataQuery = mainQuery + sortPart;

            int startIndex = pageLink.getPageSize() * pageLink.getPage();
            if (pageLink.getPageSize() > 0) {
                dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
            }
            queryTs = System.currentTimeMillis();
            List<Map<String, Object>> rows;
            try {
                rows = jdbcTemplate.queryForList(dataQuery, ctx);
            } finally {
                queryLog.logQuery(ctx, dataQuery, System.currentTimeMillis() - queryTs);
            }
            return AlarmDataAdapter.createAlarmData(pageLink, rows, totalElements, orderedEntityIds);
        });
    }

    @Override
    public long countAlarmsByQuery(TenantId tenantId, CustomerId customerId, AlarmCountQuery query, Collection<EntityId> orderedEntityIds) {
        SqlQueryContext ctx = new SqlQueryContext(new QueryContext(tenantId, null, EntityType.ALARM));

        if (query.isSearchPropagatedAlarms()) {
            if (query.getEntityFilter() == null) {
                ctx.append("select count(distinct(a.id)) from alarm_info a ");
            } else {
                ctx.append("select count(a.id) from alarm_info a ");
            }
            ctx.append(JOIN_ENTITY_ALARMS);
            if (orderedEntityIds != null) {
                if (orderedEntityIds.isEmpty()) {
                    return 0;
                }
                ctx.addUuidListParameter("entity_filter_entity_ids", orderedEntityIds.stream().map(EntityId::getId).collect(Collectors.toList()));
                ctx.append("where ea.entity_id in (:entity_filter_entity_ids)");
            } else {
                ctx.append("where a.tenant_id = :tenantId and ea.tenant_id = :tenantId");
                ctx.addUuidParameter("tenantId", tenantId.getId());
                if (customerId != null && !customerId.isNullUid()) {
                    ctx.append(" and a.customer_id = :customerId and ea.customer_id = :customerId");
                    ctx.addUuidParameter("customerId", customerId.getId());
                }
            }
        } else {
            ctx.append("select count(id) from alarm_info a ");
            if (orderedEntityIds != null) {
                if (orderedEntityIds.isEmpty()) {
                    return 0;
                }
                ctx.addUuidListParameter("entity_filter_entity_ids", orderedEntityIds.stream().map(EntityId::getId).collect(Collectors.toList()));
                ctx.append("where a.originator_id in (:entity_filter_entity_ids)");
            } else {
                ctx.append("where a.tenant_id = :tenantId");
                ctx.addUuidParameter("tenantId", tenantId.getId());
                if (customerId != null && !customerId.isNullUid()) {
                    ctx.append(" and a.customer_id = :customerId");
                    ctx.addUuidParameter("customerId", customerId.getId());
                }
            }
        }

        long startTs;
        long endTs;
        if (query.getTimeWindow() > 0) {
            endTs = System.currentTimeMillis();
            startTs = endTs - query.getTimeWindow();
        } else {
            startTs = query.getStartTs();
            endTs = query.getEndTs();
        }

        if (startTs > 0) {
            ctx.append(" and a.created_time >= :startTime");
            ctx.addLongParameter("startTime", startTs);
            if (query.isSearchPropagatedAlarms()) {
                ctx.append(" and ea.created_time >= :startTime");
            }
        }

        if (endTs > 0) {
            ctx.append(" and a.created_time <= :endTime");
            ctx.addLongParameter("endTime", endTs);
            if (query.isSearchPropagatedAlarms()) {
                ctx.append(" and ea.created_time <= :endTime");
            }
        }

        if (!CollectionUtils.isEmpty(query.getTypeList())) {
            ctx.append(" and a.type in (:alarmTypes)");
            ctx.addStringListParameter("alarmTypes", query.getTypeList());
            if (query.isSearchPropagatedAlarms()) {
                ctx.append(" and ea.alarm_type in (:alarmTypes)");
            }
        }

        if (query.getSeverityList() != null && !query.getSeverityList().isEmpty()) {
            ctx.append(" and a.severity in (:alarmSeverities)");
            ctx.addStringListParameter("alarmSeverities", query.getSeverityList().stream().map(AlarmSeverity::name).collect(Collectors.toList()));
        }

        AlarmStatusFilter asf = AlarmStatusFilter.from(query.getStatusList());
        if (asf.hasAnyFilter()) {
            if (asf.hasAckFilter()) {
                ctx.append(" and a.acknowledged = :ackStatus");
                ctx.addBooleanParameter("ackStatus", asf.getAckFilter());
            }
            if (asf.hasClearFilter()) {
                ctx.append(" and a.cleared = :clearStatus");
                ctx.addBooleanParameter("clearStatus", asf.getClearFilter());
            }
        }

        if (query.getAssigneeId() != null) {
            ctx.addUuidParameter("assigneeId", query.getAssigneeId().getId());
            ctx.append(" and a.assignee_id = :assigneeId");
        }

        return transactionTemplate.execute(trStatus -> {
            long queryTs = System.currentTimeMillis();
            try {
                return jdbcTemplate.queryForObject(ctx.getQuery(), ctx, Long.class);
            } finally {
                queryLog.logQuery(ctx, ctx.getQuery(), System.currentTimeMillis() - queryTs);
            }
        });
    }

    private String buildTextSearchQuery(SqlQueryContext ctx, List<EntityKey> selectionMapping, String searchText) {
        if (!StringUtils.isEmpty(searchText) && selectionMapping != null && !selectionMapping.isEmpty()) {
            String lowerSearchText = searchText.toLowerCase() + "%";
            List<String> searchPredicates = selectionMapping.stream()
                    .map(mapping -> alarmFieldColumnMap.get(mapping.getKey()))
                    .filter(Objects::nonNull)
                    .map(mapping -> {
                                String paramName = mapping + "_lowerSearchText";
                                ctx.addStringParameter(paramName, lowerSearchText);
                                return String.format("cast(%s as varchar) ILIKE concat('%%', :%s, '%%')", mapping, paramName);
                            }
                    ).collect(Collectors.toList());
            return String.format("%s", String.join(" or ", searchPredicates));
        } else {
            return "";
        }
    }

    private String buildPermissionsQuery(TenantId tenantId, SqlQueryContext ctx) {
        StringBuilder permissionsQuery = new StringBuilder();
        ctx.addUuidParameter("permissions_tenant_id", tenantId.getId());
        permissionsQuery.append(" a.tenant_id = :permissions_tenant_id and ea.tenant_id = :permissions_tenant_id ");
        return permissionsQuery.toString();
    }

    private void addAndIfNeeded(StringBuilder wherePart, boolean addAnd) {
        if (addAnd) {
            wherePart.append(" and ");
        }
    }
}
