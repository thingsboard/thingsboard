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
package org.thingsboard.server.dao.sql.query;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DefaultAlarmQueryRepository implements AlarmQueryRepository {

    private static final Map<String, String> alarmFieldColumnMap = new HashMap<>();

    static {
        alarmFieldColumnMap.put("createdTime", ModelConstants.CREATED_TIME_PROPERTY);
        alarmFieldColumnMap.put("ackTs", ModelConstants.ALARM_ACK_TS_PROPERTY);
        alarmFieldColumnMap.put("ackTime", ModelConstants.ALARM_ACK_TS_PROPERTY);
        alarmFieldColumnMap.put("clearTs", ModelConstants.ALARM_CLEAR_TS_PROPERTY);
        alarmFieldColumnMap.put("clearTime", ModelConstants.ALARM_CLEAR_TS_PROPERTY);
        alarmFieldColumnMap.put("details", ModelConstants.ADDITIONAL_INFO_PROPERTY);
        alarmFieldColumnMap.put("endTs", ModelConstants.ALARM_END_TS_PROPERTY);
        alarmFieldColumnMap.put("endTime", ModelConstants.ALARM_END_TS_PROPERTY);
        alarmFieldColumnMap.put("startTs", ModelConstants.ALARM_START_TS_PROPERTY);
        alarmFieldColumnMap.put("startTime", ModelConstants.ALARM_START_TS_PROPERTY);
        alarmFieldColumnMap.put("status", ModelConstants.ALARM_STATUS_PROPERTY);
        alarmFieldColumnMap.put("type", ModelConstants.ALARM_TYPE_PROPERTY);
        alarmFieldColumnMap.put("severity", ModelConstants.ALARM_SEVERITY_PROPERTY);
        alarmFieldColumnMap.put("originatorId", ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY);
        alarmFieldColumnMap.put("originatorType", ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY);
        alarmFieldColumnMap.put("originator", "originator_name");
    }

    private static final String SELECT_ORIGINATOR_NAME = " COALESCE(CASE" +
            " WHEN a.originator_type = " + EntityType.TENANT.ordinal() +
            " THEN (select title from tenant where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.CUSTOMER.ordinal() +
            " THEN (select title from customer where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.USER.ordinal() +
            " THEN (select email from tb_user where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.DASHBOARD.ordinal() +
            " THEN (select title from dashboard where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.ASSET.ordinal() +
            " THEN (select name from asset where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.DEVICE.ordinal() +
            " THEN (select name from device where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.ENTITY_VIEW.ordinal() +
            " THEN (select name from entity_view where id = a.originator_id)" +
            " END, 'Deleted') as originator_name";

    private static final String FIELDS_SELECTION = "select a.id as id," +
            " a.created_time as created_time," +
            " a.ack_ts as ack_ts," +
            " a.clear_ts as clear_ts," +
            " a.additional_info as additional_info," +
            " a.end_ts as end_ts," +
            " a.originator_id as originator_id," +
            " a.originator_type as originator_type," +
            " a.propagate as propagate," +
            " a.propagate_to_owner as propagate_to_owner," +
            " a.propagate_to_tenant as propagate_to_tenant," +
            " a.severity as severity," +
            " a.start_ts as start_ts," +
            " a.status as status, " +
            " a.tenant_id as tenant_id, " +
            " a.customer_id as customer_id, " +
            " a.propagate_relation_types as propagate_relation_types, " +
            " a.type as type," + SELECT_ORIGINATOR_NAME + ", ";

    private static final String JOIN_ENTITY_ALARMS = "inner join entity_alarm ea on a.id = ea.alarm_id";

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
        return transactionTemplate.execute(status -> {
            AlarmDataPageLink pageLink = query.getPageLink();
            QueryContext ctx = new QueryContext(new QuerySecurityContext(tenantId, null, EntityType.ALARM));
            ctx.addUuidListParameter("entity_ids", orderedEntityIds.stream().map(EntityId::getId).collect(Collectors.toList()));
            StringBuilder selectPart = new StringBuilder(FIELDS_SELECTION);
            StringBuilder fromPart = new StringBuilder(" from alarm a ");
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
            if (sortOrder != null && sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
                String sortOrderKey = sortOrder.getKey().getKey();
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
                    joinPart.append("on ea.entity_id = e.id");
                } else {
                    joinPart.append("on a.originator_id = e.id");
                }
                sortPart.append("e.priority");
            }

            long startTs;
            long endTs;
            if (pageLink.getTimeWindow() > 0) {
                endTs = System.currentTimeMillis();
                startTs = endTs - pageLink.getTimeWindow();
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

            if (pageLink.getStatusList() != null && !pageLink.getStatusList().isEmpty()) {
                Set<AlarmStatus> statusSet = toStatusSet(pageLink.getStatusList());
                if (!statusSet.isEmpty()) {
                    addAndIfNeeded(wherePart, addAnd);
                    addAnd = true;
                    ctx.addStringListParameter("alarmStatuses", statusSet.stream().map(AlarmStatus::name).collect(Collectors.toList()));
                    wherePart.append(" a.status in (:alarmStatuses)");
                }
            }

            String textSearchQuery = buildTextSearchQuery(ctx, query.getAlarmFields(), pageLink.getTextSearch());
            String mainQuery;
            if (!textSearchQuery.isEmpty()) {
                mainQuery = selectPart.toString() + fromPart.toString() + wherePart.toString();
                mainQuery = String.format("select * from (%s) a %s WHERE %s", mainQuery, joinPart, textSearchQuery);
            } else {
                mainQuery = selectPart.toString() + fromPart.toString() + joinPart.toString() + wherePart.toString();
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

    private String buildTextSearchQuery(QueryContext ctx, List<EntityKey> selectionMapping, String searchText) {
        if (!StringUtils.isEmpty(searchText) && selectionMapping != null && !selectionMapping.isEmpty()) {
            String lowerSearchText = searchText.toLowerCase() + "%";
            List<String> searchPredicates = selectionMapping.stream()
                    .map(mapping -> alarmFieldColumnMap.get(mapping.getKey()))
                    .filter(Objects::nonNull)
                    .map(mapping -> {
                                String paramName = mapping + "_lowerSearchText";
                                ctx.addStringParameter(paramName, lowerSearchText);
                                return String.format("LOWER(cast(%s as varchar)) LIKE concat('%%', :%s, '%%')", mapping, paramName);
                            }
                    ).collect(Collectors.toList());
            return String.format("%s", String.join(" or ", searchPredicates));
        } else {
            return "";
        }
    }

    private String buildPermissionsQuery(TenantId tenantId, QueryContext ctx) {
        StringBuilder permissionsQuery = new StringBuilder();
        ctx.addUuidParameter("permissions_tenant_id", tenantId.getId());
        permissionsQuery.append(" a.tenant_id = :permissions_tenant_id and ea.tenant_id = :permissions_tenant_id ");
        return permissionsQuery.toString();
    }

    private Set<AlarmStatus> toStatusSet(List<AlarmSearchStatus> statusList) {
        Set<AlarmStatus> result = new HashSet<>();
        for (AlarmSearchStatus searchStatus : statusList) {
            switch (searchStatus) {
                case ACK:
                    result.add(AlarmStatus.ACTIVE_ACK);
                    result.add(AlarmStatus.CLEARED_ACK);
                    break;
                case UNACK:
                    result.add(AlarmStatus.ACTIVE_UNACK);
                    result.add(AlarmStatus.CLEARED_UNACK);
                    break;
                case CLEARED:
                    result.add(AlarmStatus.CLEARED_ACK);
                    result.add(AlarmStatus.CLEARED_UNACK);
                    break;
                case ACTIVE:
                    result.add(AlarmStatus.ACTIVE_ACK);
                    result.add(AlarmStatus.ACTIVE_UNACK);
                    break;
                default:
                    break;
            }
            if (searchStatus == AlarmSearchStatus.ANY || result.size() == AlarmStatus.values().length) {
                result.clear();
                return result;
            }
        }
        return result;
    }

    private void addAndIfNeeded(StringBuilder wherePart, boolean addAnd) {
        if (addAnd) {
            wherePart.append(" and ");
        }
    }
}
